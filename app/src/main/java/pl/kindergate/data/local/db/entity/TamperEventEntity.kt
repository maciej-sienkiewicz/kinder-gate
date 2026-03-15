package pl.kindergate.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.model.TamperType

@Entity(tableName = "tamper_events")
data class TamperEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    @ColumnInfo(name = "type") val type: String,
    @ColumnInfo(name = "detected_at_ms") val detectedAtMs: Long,
    @ColumnInfo(name = "detail") val detail: String = "",
    @ColumnInfo(name = "parent_acknowledged") val parentAcknowledged: Boolean = false
) {
    fun toDomain() = TamperEvent(
        id = id,
        type = TamperType.valueOf(type),
        detectedAtMs = detectedAtMs,
        detail = detail
    )

    companion object {
        fun fromDomain(e: TamperEvent) = TamperEventEntity(
            id = e.id,
            type = e.type.name,
            detectedAtMs = e.detectedAtMs,
            detail = e.detail
        )
    }
}
