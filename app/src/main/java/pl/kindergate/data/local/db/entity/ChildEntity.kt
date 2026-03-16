package pl.kindergate.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.kindergate.domain.model.ChildProfile

@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "age") val age: Int,
    @ColumnInfo(name = "grade_level") val gradeLevel: Int?,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
) {
    fun toDomain() = ChildProfile(
        id = id,
        name = name,
        age = age,
        gradeLevel = gradeLevel,
    )

    companion object {
        fun fromDomain(profile: ChildProfile, createdAtMs: Long = System.currentTimeMillis()) =
            ChildEntity(
                id = profile.id,
                name = profile.name,
                age = profile.age,
                gradeLevel = profile.gradeLevel,
                createdAtMs = createdAtMs,
            )
    }
}
