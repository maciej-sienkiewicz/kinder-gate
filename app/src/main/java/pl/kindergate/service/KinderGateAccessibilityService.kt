package pl.kindergate.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import kotlinx.coroutines.flow.MutableSharedFlow
import java.util.concurrent.atomic.AtomicReference

/**
 * Accessibility Service – FALLBACK foreground app detector and EVENT TRIGGER.
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
        // Emit event to trigger immediate tick in MonitorService
        packageEvents.tryEmit(pkg)
    }

    override fun onInterrupt() {}

    override fun onDestroy() {
        super.onDestroy()
        isServiceConnected = false
        lastForegroundPackage.set(null)
    }

    companion object {
        val lastForegroundPackage = AtomicReference<String?>(null)
        
        /**
         * Flow of package names that have just come to the foreground.
         * Used by MonitorService to react nearly instantly to window changes.
         */
        val packageEvents = MutableSharedFlow<String>(
            extraBufferCapacity = 16,
            onBufferOverflow = kotlinx.coroutines.channels.BufferOverflow.DROP_OLDEST
        )

        @Volatile
        var isServiceConnected: Boolean = false
            private set
    }
}
