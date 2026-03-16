package pl.kindergate.domain.model

import android.graphics.drawable.Drawable

/**
 * UI model representing an app installed on the device.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isExcluded: Boolean
)
