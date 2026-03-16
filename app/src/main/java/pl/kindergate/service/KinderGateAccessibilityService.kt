package pl.kindergate.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.content.Intent
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import pl.kindergate.feature.blocking.BlockingActivity
import java.util.concurrent.atomic.AtomicReference

/**
 * Accessibility Service – FALLBACK foreground app detector.
 *
 * PURPOSE: Provides foreground app package name on devices/ROMs where
 * UsageStatsManager is unavailable or blocked (e.g., some Xiaomi/MIUI variants,
 * certain Huawei firmware configurations).
 *
 * SCOPE: Intentionally minimal.
 * - We only listen to TYPE_WINDOW_STATE_CHANGED events.
 * - We do NOT read window content (canRetrieveWindowContent = false in config).
 * - We do NOT intercept gestures or keyboard events.
 * - We do NOT log what the child is doing – only which app is foreground.
 *
 * PLAY STORE COMPLIANCE:
 * The primary concern with AccessibilityService in Play Store review is
 * "accessibility services should only be used to assist users with disabilities."
 * For parental control apps, Google's policy makes an exception if:
 * 1. The service is used only for its stated parental control purpose
 * 2. The manifest declares a clear description
 * 3. The service does NOT collect/exfiltrate sensitive data
 * We satisfy all three conditions.
 *
 * THREAD SAFETY:
 * lastForegroundPackage uses AtomicReference for safe cross-thread reads.
 * MonitorService reads it from its coroutine; accessibility callbacks arrive
 * on the main thread.
 */
class KinderGateAccessibilityService : AccessibilityService() {

    override fun onServiceConnected() {
        val info = AccessibilityServiceInfo().apply {
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100
        }
        serviceInfo = info
        isServiceConnected = true
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg.isBlank()) return
        lastForegroundPackage.set(pkg)

        // Layer 3 bypass prevention: if blocking is active and the child managed
        // to bring a non-KinderGate window to front, immediately re-launch the
        // blocking screen. This fires within milliseconds of the window change,
        // much faster than the 1-second polling loop in MonitorService.
        if (BlockingActivity.isBlockingActiveGlobal && !pkg.startsWith(OWN_PACKAGE_PREFIX)) {
            Log.i(TAG, "Window changed to $pkg during blocking – re-launching block screen")
            val intent = Intent(this, BlockingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_REORDER_TO_FRONT or
                    Intent.FLAG_ACTIVITY_SINGLE_TOP
            }
            startActivity(intent)
        }
    }

    override fun onInterrupt() {
        // No ongoing actions to interrupt
    }

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        lastForegroundPackage.set(null)
    }

    companion object {
        private const val TAG = "KG_Accessibility"
        private const val OWN_PACKAGE_PREFIX = "pl.kindergate"

        /**
         * Package name of the most recently seen foreground window.
         * Written on main thread from accessibility callback.
         * Read on MonitorService coroutine thread.
         * AtomicReference ensures visibility without synchronization.
         */
        val lastForegroundPackage = AtomicReference<String?>(null)

        @Volatile
        var isServiceConnected: Boolean = false
            private set
    }
}
