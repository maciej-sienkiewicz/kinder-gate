package pl.kindergate.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.kindergate.domain.model.BlockSession

@Entity(tableName = "block_sessions")
data class BlockSessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "package_name") val packageName: String,
    @ColumnInfo(name = "elapsed_triggered_ms") val elapsedRealtimeTriggeredMs: Long,
    @ColumnInfo(name = "wall_clock_triggered_ms") val wallClockTriggeredMs: Long,
    @ColumnInfo(name = "acknowledged_elapsed_ms") val acknowledgedAtElapsedMs: Long? = null,
    @ColumnInfo(name = "session_duration_ms") val sessionDurationMs: Long = 0
) {
    fun toDomain() = BlockSession(
        id = id,
        packageName = packageName,
        elapsedRealtimeTriggeredMs = elapsedRealtimeTriggeredMs,
        wallClockTriggeredMs = wallClockTriggeredMs,
        acknowledgedAtElapsedMs = acknowledgedAtElapsedMs,
        sessionDurationMs = sessionDurationMs
    )

    companion object {
        fun fromDomain(s: BlockSession) = BlockSessionEntity(
            id = s.id,
            packageName = s.packageName,
            elapsedRealtimeTriggeredMs = s.elapsedRealtimeTriggeredMs,
            wallClockTriggeredMs = s.wallClockTriggeredMs,
            acknowledgedAtElapsedMs = s.acknowledgedAtElapsedMs,
            sessionDurationMs = s.sessionDurationMs
        )
    }
}
