package pl.kindergate.domain.model

/**
 * Snapshot of all permissions/capabilities KinderGate requires.
 * Used in the health dashboard and onboarding flow.
 *
 * isUsageStatsGranted: required, without this detection is impossible
 * isOverlayGranted: required for TYPE_APPLICATION_OVERLAY blocking fallback
 * isNotificationGranted: required for foreground service (Android 13+)
 * isBatteryOptimizationExempt: strongly recommended
 * isAccessibilityEnabled: optional but strongly recommended
 * isIgnoringBatteryOptimizations: alias for isBatteryOptimizationExempt
 */
data class PermissionStatus(
    val isUsageStatsGranted: Boolean,
    val isOverlayGranted: Boolean,
    val isNotificationGranted: Boolean,
    val isBatteryOptimizationExempt: Boolean,
    val isAccessibilityEnabled: Boolean
) {
    val isCritical: Boolean get() = !isUsageStatsGranted
    val isFullyOperational: Boolean get() =
        isUsageStatsGranted && isOverlayGranted && isNotificationGranted && isBatteryOptimizationExempt
    val healthLevel: HealthLevel get() = when {
        isCritical -> HealthLevel.CRITICAL
        !isFullyOperational -> HealthLevel.WARNING
        else -> HealthLevel.OK
    }
}

enum class HealthLevel { OK, WARNING, CRITICAL }
