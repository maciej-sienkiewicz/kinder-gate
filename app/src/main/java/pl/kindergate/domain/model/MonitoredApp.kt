package pl.kindergate.domain.model

import kotlinx.serialization.Serializable

/**
 * A user-installed app that is subject to monitoring.
 * Parent explicitly adds apps to this list during onboarding or via dashboard.
 */
@Serializable
data class MonitoredApp(
    val packageName: String,
    val appLabel: String,
    val isEnabled: Boolean = true,
    val addedAtMs: Long = System.currentTimeMillis()
)
