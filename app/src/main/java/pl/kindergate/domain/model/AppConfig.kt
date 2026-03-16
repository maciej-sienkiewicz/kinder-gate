package pl.kindergate.domain.model

/**
 * Parent-configurable settings for the monitoring engine.
 *
 * blockIntervalSeconds: time in foreground before PAUSE triggers.
 *   Default 60s. For MVP, this is the primary (and only) control knob.
 * isMonitoringEnabled: global kill switch, protected by PIN.
 * hasCompletedOnboarding: whether parent finished setup flow.
 * pinHash: BCrypt hash of the 4-8 digit parent PIN.
 *   Never store plain PIN. Hash stored in EncryptedSharedPreferences.
 */
data class AppConfig(
    val blockIntervalSeconds: Int = 60,
    val isMonitoringEnabled: Boolean = true,
    val hasCompletedOnboarding: Boolean = false,
    val pinConfigured: Boolean = false,
    /**
     * UUID of the currently active child profile.
     * Null until the parent completes the child-profile setup step.
     * MVP: only one child per device, so this is always the sole profile's id
     * once onboarding is done.
     */
    val selectedChildId: String? = null,
)
