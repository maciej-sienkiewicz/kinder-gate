package pl.kindergate.domain.model

/**
 * Records tamper attempts detected by KinderGate.
 * Used to inform parent that the protection was interfered with.
 *
 * These events are intentionally simple for MVP.
 * In v1: add push notification to parent device.
 */
data class TamperEvent(
    val id: Long = 0,
    val type: TamperType,
    val detectedAtMs: Long,
    val detail: String = ""
)

enum class TamperType {
    USAGE_STATS_REVOKED,
    OVERLAY_PERMISSION_REVOKED,
    ACCESSIBILITY_DISABLED,
    BATTERY_OPTIMIZATION_ENABLED,
    FORCE_STOP_DETECTED,          // inferred from process restart after short uptime
    BOOT_AFTER_SHORT_UPTIME,      // device rebooted shortly after service was running
    MONITORED_APP_UNINSTALLED,
    SERVICE_KILLED_AND_RESTARTED,
    UNINSTALL_ATTEMPTED           // detected via DeviceAdminReceiver if admin granted
}

data class MonitorState(
    val foregroundPackage: String? = null,
    val activeElapsedMs: Long = 0L,         // monotonic time accumulated in current session
    val isBlocking: Boolean = false,
    val pendingBlockSessionId: Long? = null,
    val lastTickElapsedMs: Long = 0L
)
