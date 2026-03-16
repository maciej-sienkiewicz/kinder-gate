package pl.kindergate.service

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.IBinder
import android.os.PowerManager
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import pl.kindergate.KinderGateApplication
import pl.kindergate.MainActivity
import pl.kindergate.R
import pl.kindergate.data.local.prefs.SecurePreferencesManager
import pl.kindergate.domain.model.BlockSession
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.model.TamperType
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.repository.MonitoredAppsRepository
import pl.kindergate.domain.repository.SessionRepository
import pl.kindergate.feature.blocking.BlockingActivity
import javax.inject.Inject

/**
 * Core monitoring service for KinderGate.
 *
 * This is a FOREGROUND SERVICE that runs continuously while the app is active.
 * It is the single source of truth for monitoring logic.
 *
 * Architecture:
 * - Started by BootReceiver after device restart
 * - Started by MainActivity when onboarding completes
 * - Runs as long as the device is on; cannot be killed by battery optimizer
 *   (if parent granted the exemption during onboarding)
 * - Uses a tight poll loop (every 1s) to detect foreground app changes
 *
 * Service lifecycle and resistance to process death:
 * - START_STICKY: Android recreates the service after it's killed
 * - The service is re-started by BootReceiver after reboot
 * - WakeLock: PARTIAL wake lock ensures the CPU stays awake during detection
 *   (screen can be off, we still monitor)
 *
 * NOTE on wake locks and battery:
 * - We hold a partial WakeLock to ensure the poll loop continues even when
 *   the screen is off. This is necessary for accurate time tracking.
 * - The parent must grant battery optimization exemption for this to be reliable.
 * - WakeLock is released when the service is destroyed.
 *
 * Thread model:
 * - All monitoring logic runs on a single coroutine (Dispatchers.Default)
 * - DB writes are dispatched to Dispatchers.IO via repository layer
 * - UI interactions (launching BlockingActivity) use Dispatchers.Main
 */
@AndroidEntryPoint
class MonitorService : Service() {

    @Inject lateinit var detector: AppUsageDetector
    @Inject lateinit var timer: SessionTimer
    @Inject lateinit var monitoredAppsRepository: MonitoredAppsRepository
    @Inject lateinit var sessionRepository: SessionRepository
    @Inject lateinit var configRepository: ConfigRepository
    @Inject lateinit var securePrefs: SecurePreferencesManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitorJob: Job? = null
    private var healthCheckJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    // Cached set of excluded (blacklisted) packages – refreshed when DB changes
    @Volatile private var excludedPackages: Set<String> = emptySet()

    // Last known permission states for change detection (avoid duplicate tamper events)
    @Volatile private var lastUsageStatsGranted: Boolean = true
    @Volatile private var lastOverlayGranted: Boolean = true
    @Volatile private var lastAccessibilityEnabled: Boolean = true

    // Track whether we've launched blocking to avoid duplicate launches
    @Volatile private var isBlockingActive: Boolean = false

    // Pending block session ID for acknowledgment
    @Volatile private var pendingBlockSessionId: Long? = null

    override fun onCreate() {
        super.onCreate()
        acquireWakeLock()
        startForeground(NOTIFICATION_ID, buildForegroundNotification())
        securePrefs.setServiceStartedAtElapsed(SystemClock.elapsedRealtime())
        observeMonitoredApps()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_BLOCK_ACKNOWLEDGED -> handleBlockAcknowledged()
            ACTION_START -> startMonitoring()
            ACTION_STOP -> stopSelf()
            else -> startMonitoring()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        monitorJob?.cancel()
        healthCheckJob?.cancel()
        releaseWakeLock()
        securePrefs.setLastKnownServiceUptimeMs(SystemClock.elapsedRealtime())
    }

    // -------------------------------------------------------------------------
    // Monitoring loop
    // -------------------------------------------------------------------------

    private fun startMonitoring() {
        if (monitorJob?.isActive == true) return
        monitorJob = serviceScope.launch {
            while (isActive) {
                tick()
                delay(AppUsageDetector.POLL_INTERVAL_MS)
            }
        }
        healthCheckJob = serviceScope.launch {
            while (isActive) {
                checkPermissionHealth()
                delay(HEALTH_CHECK_INTERVAL_MS)
            }
        }
    }

    private suspend fun tick() {
        if (!configRepository.getConfig().isMonitoringEnabled) {
            timer.reset()
            return
        }

        val foregroundPackage = detector.getForegroundPackage() ?: return
        val isExcluded = foregroundPackage in excludedPackages

        if (isExcluded) {
            timer.onNonMonitoredForeground()
            isBlockingActive = false
            return
        }

        // If we're already in blocking state, re-launch if child navigated away
        if (timer.getCurrentState() == SessionTimer.State.BLOCKING) {
            if (foregroundPackage != this::class.java.`package`?.name && !isBlockingActive) {
                launchBlockingScreen(foregroundPackage)
            }
            return
        }

        val intervalMs = configRepository.getConfig().blockIntervalSeconds * 1_000L
        val shouldBlock = timer.tick(foregroundPackage, intervalMs)

        if (shouldBlock) {
            val sessionId = recordBlockSession(foregroundPackage)
            pendingBlockSessionId = sessionId
            launchBlockingScreen(foregroundPackage)
        }
    }

    private suspend fun recordBlockSession(packageName: String): Long {
        val session = BlockSession(
            packageName = packageName,
            elapsedRealtimeTriggeredMs = SystemClock.elapsedRealtime(),
            wallClockTriggeredMs = System.currentTimeMillis(),
            sessionDurationMs = timer.getElapsedMs()
        )
        return sessionRepository.insertBlockSession(session)
    }

    private fun launchBlockingScreen(packageName: String) {
        isBlockingActive = true
        val intent = Intent(this, BlockingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(BlockingActivity.EXTRA_PACKAGE_NAME, packageName)
            putExtra(BlockingActivity.EXTRA_SESSION_ID, pendingBlockSessionId ?: -1L)
        }
        startActivity(intent)
    }

    private fun handleBlockAcknowledged() {
        isBlockingActive = false
        val sessionId = pendingBlockSessionId
        if (sessionId != null) {
            serviceScope.launch {
                sessionRepository.acknowledgeSession(sessionId, SystemClock.elapsedRealtime())
            }
            pendingBlockSessionId = null
        }
        timer.onBlockAcknowledged()
    }

    // -------------------------------------------------------------------------
    // Permission health monitoring (tamper detection)
    // -------------------------------------------------------------------------

    private suspend fun checkPermissionHealth() {
        val status = configRepository.getPermissionStatus()

        // Only log when state transitions from granted → revoked (not on every tick)
        if (!status.isUsageStatsGranted && lastUsageStatsGranted) {
            recordTamperEvent(TamperType.USAGE_STATS_REVOKED)
            sendTamperNotification()
        }
        lastUsageStatsGranted = status.isUsageStatsGranted

        if (!status.isOverlayGranted && lastOverlayGranted) {
            recordTamperEvent(TamperType.OVERLAY_PERMISSION_REVOKED)
        }
        lastOverlayGranted = status.isOverlayGranted

        if (!status.isAccessibilityEnabled && lastAccessibilityEnabled) {
            // Accessibility is optional but its revocation is still worth noting
            recordTamperEvent(TamperType.ACCESSIBILITY_DISABLED)
        }
        lastAccessibilityEnabled = status.isAccessibilityEnabled
    }

    private suspend fun recordTamperEvent(type: TamperType) {
        sessionRepository.insertTamperEvent(
            TamperEvent(
                type = type,
                detectedAtMs = System.currentTimeMillis()
            )
        )
    }

    private fun sendTamperNotification() {
        val nm = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val notification = NotificationCompat.Builder(this, KinderGateApplication.CHANNEL_ALERT)
            .setSmallIcon(R.drawable.ic_warning)
            .setContentTitle(getString(R.string.notification_tamper_title))
            .setContentText(getString(R.string.notification_tamper_text))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(mainActivityPendingIntent())
            .build()
        nm.notify(TAMPER_NOTIFICATION_ID, notification)
    }

    // -------------------------------------------------------------------------
    // Monitored apps cache
    // -------------------------------------------------------------------------

    private fun observeMonitoredApps() {
        serviceScope.launch {
            monitoredAppsRepository.observeMonitoredApps().collect { apps ->
                excludedPackages = apps.filter { it.isEnabled }.map { it.packageName }.toSet()
            }
        }
    }

    // -------------------------------------------------------------------------
    // WakeLock
    // -------------------------------------------------------------------------

    private fun acquireWakeLock() {
        val pm = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = pm.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "kindergate:MonitorWakeLock"
        ).apply {
            acquire(WAKE_LOCK_TIMEOUT_MS)
        }
    }

    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    // -------------------------------------------------------------------------
    // Foreground notification
    // -------------------------------------------------------------------------

    private fun buildForegroundNotification(): Notification {
        return NotificationCompat.Builder(this, KinderGateApplication.CHANNEL_MONITOR)
            .setSmallIcon(R.drawable.ic_shield)
            .setContentTitle(getString(R.string.notification_monitor_title))
            .setContentText(getString(R.string.notification_monitor_text))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setContentIntent(mainActivityPendingIntent())
            .build()
    }

    private fun mainActivityPendingIntent(): PendingIntent {
        val intent = Intent(this, MainActivity::class.java)
        return PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    companion object {
        const val ACTION_START = "pl.kindergate.action.START_MONITOR"
        const val ACTION_STOP = "pl.kindergate.action.STOP_MONITOR"
        const val ACTION_BLOCK_ACKNOWLEDGED = "pl.kindergate.action.BLOCK_ACK"

        private const val NOTIFICATION_ID = 1001
        private const val TAMPER_NOTIFICATION_ID = 1002
        private const val HEALTH_CHECK_INTERVAL_MS = 30_000L
        private const val WAKE_LOCK_TIMEOUT_MS = 60L * 60L * 1000L // 1 hour, auto-renew

        fun startIntent(context: Context) = Intent(context, MonitorService::class.java).apply {
            action = ACTION_START
        }

        fun blockAcknowledgedIntent(context: Context) =
            Intent(context, MonitorService::class.java).apply {
                action = ACTION_BLOCK_ACKNOWLEDGED
            }
    }
}
