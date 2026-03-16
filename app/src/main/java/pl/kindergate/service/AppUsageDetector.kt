package pl.kindergate.service

import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Detects the currently active (foreground) application.
 *
 * Uses a hybrid approach:
 * 1. Accessibility Service (Real-time, preferred)
 * 2. UsageEvents (Fast fallback, queries last 10s)
 * 3. UsageStats (Slow fallback, queries last hour)
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

    fun getForegroundPackage(): String? {
        // Priority 1: Accessibility Service (Instant)
        val fromAccessibility = KinderGateAccessibilityService.lastForegroundPackage.get()
        if (fromAccessibility != null) return fromAccessibility

        // Priority 2: Usage Events (Recent transitions)
        val manager = usageStatsManager ?: return null
        val now = System.currentTimeMillis()
        val fromEvents = getViaUsageEvents(manager, now)
        if (fromEvents != null) return fromEvents

        // Priority 3: Usage Stats (Aggregated)
        return getViaUsageStats(manager, now)
    }

    private fun getViaUsageEvents(manager: UsageStatsManager, now: Long): String? {
        return try {
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
            lastResumedPackage
        } catch (e: Exception) {
            null
        }
    }

    private fun getViaUsageStats(manager: UsageStatsManager, now: Long): String? {
        return try {
            val stats = manager.queryUsageStats(
                UsageStatsManager.INTERVAL_DAILY,
                now - 1000 * 60 * 60,
                now
            )
            val lastApp = stats?.filter { it.lastTimeUsed > 0 }?.maxByOrNull { it.lastTimeUsed }
            lastApp?.packageName
        } catch (e: Exception) {
            null
        }
    }

    companion object {
        private const val TAG = "KG_Detector"
        const val POLL_INTERVAL_MS = 1_000L
        const val QUERY_WINDOW_MS = 10_000L // 10s window for events
    }
}
