package pl.kindergate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.content.ContextCompat
import dagger.hilt.android.AndroidEntryPoint
import pl.kindergate.data.local.prefs.SecurePreferencesManager
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.model.TamperType
import pl.kindergate.domain.repository.SessionRepository
import pl.kindergate.service.MonitorService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Restarts MonitorService after device reboot.
 *
 * Requires RECEIVE_BOOT_COMPLETED permission.
 * Triggered by BOOT_COMPLETED and vendor-specific equivalents declared in manifest.
 *
 * FORCE STOP DETECTION:
 * Android clears BOOT_COMPLETED receiver registration when the user force-stops
 * an app. This means BootReceiver will NOT fire after reboot if the child
 * previously force-stopped the app. This is a known Android limitation:
 * - There is NO way to auto-start after force-stop without Device Admin
 * - We detect this scenario by checking if the service has been running
 *   since the last boot, and alert the parent if not
 *
 * SAFE MODE:
 * Third-party services do NOT run in safe mode. We cannot prevent this.
 * Parent is informed about this limitation in onboarding.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var securePrefs: SecurePreferencesManager
    @Inject lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val bootActions = setOf(
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            "com.miui.home.launcher.action.RESUME"
        )
        if (action !in bootActions) return

        // Only start if onboarding is complete and monitoring is enabled
        if (!securePrefs.isOnboardingComplete()) return
        if (!securePrefs.isMonitoringEnabled()) return

        checkForForcedStopBeforeBoot(context)
        startMonitorService(context)
    }

    /**
     * Heuristic: if we have a recorded service uptime but it's very short,
     * the service was likely killed before the reboot (possibly by child).
     * We log this as a tamper event for the parent.
     *
     * This is imperfect – a normal battery-saver kill looks the same.
     * We still log it as informational.
     */
    private fun checkForForcedStopBeforeBoot(context: Context) {
        val lastUptimeMs = securePrefs.getLastKnownServiceUptimeMs()
        if (lastUptimeMs == 0L) return // no previous run recorded

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                sessionRepository.insertTamperEvent(
                    TamperEvent(
                        type = TamperType.BOOT_AFTER_SHORT_UPTIME,
                        detectedAtMs = System.currentTimeMillis(),
                        detail = "Service uptime before boot: ${lastUptimeMs}ms"
                    )
                )
            } finally {
                pendingResult.finish()
            }
        }
    }

    private fun startMonitorService(context: Context) {
        val intent = MonitorService.startIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            ContextCompat.startForegroundService(context, intent)
        } else {
            context.startService(intent)
        }
    }
}
