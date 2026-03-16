package pl.kindergate.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects the currently active (foreground) application.
 *
 * PRIMARY METHOD: UsageStatsManager.queryEvents()
 *
 * Design rationale:
 * - UsageStatsManager is the official Android API for this purpose
 * - queryEvents() gives us ACTIVITY_RESUMED events, which is more accurate
 *   than queryUsageStats() (which gives per-app aggregated time windows)
 * - Requires PACKAGE_USAGE_STATS permission (granted by user in Settings)
 * - Works on API 21+ without root
 * - NOT subject to Play Store restrictions unlike AccessibilityService
 *   for this specific use case
 *
 * FALLBACK METHOD: AccessibilityService
 *
 * KinderGateAccessibilityService maintains a companion object with the
 * last seen package name from typeWindowStateChanged events. We read
 * this if UsageStats is unavailable (some OEM ROMs block the API despite
 * the permission being granted).
 *
 * OEM-SPECIFIC NOTES:
 * - Samsung (OneUI): UsageStats works, may need "Allow usage tracking" in
 *   Digital Wellbeing settings on newer models
 * - Xiaomi (MIUI): Requires additional "Background pop-up" permission
 *   in Security app; UsageStats unreliable → Accessibility fallback important
 * - Huawei (EMUI/HarmonyOS): May restrict UsageStats; Accessibility works
 * - Oppo/Realme (ColorOS): Generally fine with standard approach
 *
 * ACCURACY: We query the last 2 seconds of events (configurable) to find
 * the most recent ACTIVITY_RESUMED. This gives us ~1s accuracy which is
 * sufficient for a 60s interval timer.
 */
@Singleton
class AppUsageDetector @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val usageStatsManager: UsageStatsManager? by lazy {
        try {
            context.getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Last known foreground package from UsageStats.
     *
     * ACTIVITY_RESUMED fires ONCE when an app comes to foreground, not continuously.
     * With a short query window (e.g. 2s), the event would fall out of the window
     * after 2 ticks and getForegroundPackage() would return null forever – breaking
     * the entire monitoring system.
     *
     * We cache the last detected package and clear it only when we see an explicit
     * ACTIVITY_PAUSED for that package (meaning the user left the app).
     */
    @Volatile
    private var lastKnownForegroundPackage: String? = null

    /**
     * Returns the package name of the currently foreground app, or null if unknown.
     * Called every [POLL_INTERVAL_MS] from MonitorService.
     *
     * Detection chain:
     * 1. UsageStats queryEvents – official API, most reliable
     * 2. Cached last known package – survives gaps between events
     * 3. AccessibilityService fallback – for OEMs that block UsageStats
     */
    fun getForegroundPackage(): String? {
        return getViaUsageStats() ?: getViaAccessibilityService()
    }

    private fun getViaUsageStats(): String? {
        val manager = usageStatsManager ?: run {
            Log.w(TAG, "getViaUsageStats: UsageStatsManager unavailable")
            return null
        }
        return try {
            val now = System.currentTimeMillis()
            val events = manager.queryEvents(now - QUERY_WINDOW_MS, now)
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null
            var lastResumedTime = 0L
            var lastPausedPackage: String? = null

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                when (event.eventType) {
                    UsageEvents.Event.ACTIVITY_RESUMED -> {
                        if (event.timeStamp > lastResumedTime) {
                            lastResumedTime = event.timeStamp
                            lastResumedPackage = event.packageName
                        }
                    }
                    UsageEvents.Event.ACTIVITY_PAUSED -> {
                        lastPausedPackage = event.packageName
                    }
                }
            }

            // If we found a fresh RESUMED event, update the cache
            if (lastResumedPackage != null && lastResumedPackage != context.packageName) {
                lastKnownForegroundPackage = lastResumedPackage
                return lastResumedPackage
            }

            // If the cached package was PAUSED, clear the cache (user left the app)
            if (lastPausedPackage != null && lastPausedPackage == lastKnownForegroundPackage) {
                Log.d(TAG, "getViaUsageStats: cached package $lastPausedPackage was paused, clearing")
                lastKnownForegroundPackage = null
                return null
            }

            // No fresh events – return cached package (app is still in foreground)
            val cached = lastKnownForegroundPackage
            if (cached != null) {
                return cached
            }

            // Filter out our own package
            if (lastResumedPackage == context.packageName) null
            else lastResumedPackage
        } catch (e: SecurityException) {
            Log.e(TAG, "getViaUsageStats: SecurityException – PACKAGE_USAGE_STATS revoked?", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "getViaUsageStats: unexpected error", e)
            null
        }
    }

    private fun getViaAccessibilityService(): String? {
        val pkg = KinderGateAccessibilityService.lastForegroundPackage.get()
            .takeIf { it != null && it != context.packageName }
        if (pkg != null) Log.d(TAG, "getViaAccessibilityService: fallback detected $pkg")
        return pkg
    }

    companion object {
        private const val TAG = "KG_Detector"
        const val POLL_INTERVAL_MS = 1_000L

        /**
         * Query window for UsageStats events.
         * Must be long enough to catch the last ACTIVITY_RESUMED event even if
         * the user has been in the same app for a while. 60s matches the default
         * block interval and ensures we always find recent events.
         */
        const val QUERY_WINDOW_MS = 60_000L
    }
}
