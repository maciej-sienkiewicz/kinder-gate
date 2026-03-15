package pl.kindergate.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.os.Build
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
     * Returns the package name of the currently foreground app, or null if unknown.
     * Called every [POLL_INTERVAL_MS] from MonitorService.
     */
    fun getForegroundPackage(): String? {
        return getViaUsageStats() ?: getViaAccessibilityService()
    }

    private fun getViaUsageStats(): String? {
        val manager = usageStatsManager ?: return null
        return try {
            val now = System.currentTimeMillis()
            // Query last 2s to ensure we capture the most recent foreground event
            val events = manager.queryEvents(now - QUERY_WINDOW_MS, now)
            val event = UsageEvents.Event()
            var lastResumedPackage: String? = null
            var lastResumedTime = 0L

            while (events.hasNextEvent()) {
                events.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED &&
                    event.timeStamp > lastResumedTime) {
                    lastResumedTime = event.timeStamp
                    lastResumedPackage = event.packageName
                }
            }

            // Filter out our own blocking screen to prevent self-detection loop
            if (lastResumedPackage == context.packageName) null
            else lastResumedPackage
        } catch (e: SecurityException) {
            // Permission revoked at runtime – tamper detection handles this
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun getViaAccessibilityService(): String? {
        // KinderGateAccessibilityService.lastForegroundPackage is updated
        // from the accessibility event thread. Read is atomic for String references.
        return KinderGateAccessibilityService.lastForegroundPackage.get()
            .takeIf { it != null && it != context.packageName }
    }

    companion object {
        const val POLL_INTERVAL_MS = 1_000L
        const val QUERY_WINDOW_MS = 2_000L
    }
}
