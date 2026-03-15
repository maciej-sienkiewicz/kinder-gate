package pl.kindergate.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents an app installed on the device.
 * Used in the app picker UI. Not persisted – fetched at runtime from PackageManager.
 */
data class InstalledApp(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean,
    val isMonitored: Boolean = false
)
