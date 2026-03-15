package pl.kindergate.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.kindergate.domain.model.MonitoredApp

@Entity(tableName = "monitored_apps")
data class MonitoredAppEntity(
    @PrimaryKey @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "app_label") val appLabel: String,
    @ColumnInfo(name = "is_enabled") val isEnabled: Boolean = true,
    @ColumnInfo(name = "added_at_ms") val addedAtMs: Long = System.currentTimeMillis()
) {
    fun toDomain() = MonitoredApp(packageName, appLabel, isEnabled, addedAtMs)

    companion object {
        fun fromDomain(app: MonitoredApp) = MonitoredAppEntity(
            packageName = app.packageName,
            appLabel = app.appLabel,
            isEnabled = app.isEnabled,
            addedAtMs = app.addedAtMs
        )
    }
}
