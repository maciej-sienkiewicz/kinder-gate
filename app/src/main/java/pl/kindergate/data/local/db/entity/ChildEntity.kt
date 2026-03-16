package pl.kindergate.data.local.db.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType

/**
 * Room entity for a child profile.
 *
 * [enabledSubjects] and [enabledTaskTypes] are persisted as comma-delimited enum names
 * (e.g. "MATH,WRITING"). An empty/blank string means "all allowed" – matching the
 * domain convention in [ChildProfile].
 */
@Entity(tableName = "children")
data class ChildEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,
    @ColumnInfo(name = "name") val name: String,
    @ColumnInfo(name = "age") val age: Int,
    @ColumnInfo(name = "grade_level") val gradeLevel: Int?,
    @ColumnInfo(name = "created_at_ms") val createdAtMs: Long = System.currentTimeMillis(),
    /** Comma-separated [TaskSubject] names; empty = all enabled. */
    @ColumnInfo(name = "enabled_subjects") val enabledSubjects: String = "",
    /** Comma-separated [TaskType] names; empty = all enabled. */
    @ColumnInfo(name = "enabled_task_types") val enabledTaskTypes: String = "",
) {
    fun toDomain() = ChildProfile(
        id = id,
        name = name,
        age = age,
        gradeLevel = gradeLevel,
        enabledSubjects = deserializeSubjects(enabledSubjects),
        enabledTaskTypes = deserializeTaskTypes(enabledTaskTypes),
    )

    companion object {
        fun fromDomain(profile: ChildProfile, createdAtMs: Long = System.currentTimeMillis()) =
            ChildEntity(
                id = profile.id,
                name = profile.name,
                age = profile.age,
                gradeLevel = profile.gradeLevel,
                createdAtMs = createdAtMs,
                enabledSubjects = serializeSubjects(profile.enabledSubjects),
                enabledTaskTypes = serializeTaskTypes(profile.enabledTaskTypes),
            )

        fun serializeSubjects(subjects: Set<TaskSubject>): String =
            subjects.joinToString(",") { it.name }

        fun serializeTaskTypes(types: Set<TaskType>): String =
            types.joinToString(",") { it.name }

        fun deserializeSubjects(raw: String): Set<TaskSubject> {
            if (raw.isBlank()) return emptySet()
            return raw.split(",").mapNotNullTo(mutableSetOf()) { name ->
                TaskSubject.entries.find { it.name == name }
            }
        }

        fun deserializeTaskTypes(raw: String): Set<TaskType> {
            if (raw.isBlank()) return emptySet()
            return raw.split(",").mapNotNullTo(mutableSetOf()) { name ->
                TaskType.entries.find { it.name == name }
            }
        }
    }
}
