package pl.kindergate.domain.model

import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType

/**
 * Single source of truth for a child's identity in KinderGate.
 *
 * MVP: 1 child per device, identified by UUID [id].
 * Future: list of children, each with own task history, rewards, etc.
 *
 * Fields marked "future" are intentionally absent from MVP but the class
 * is designed so they can be added without breaking existing callers:
 *   - avatarType: String  – avatar identifier (emoji key or drawable res name)
 *   - createdAtMs: Long   – unix epoch of profile creation
 *
 * ## Task configuration
 * [enabledSubjects] and [enabledTaskTypes] form an allow-list used by [TaskEngine]:
 *   - Empty set means "all allowed" (default, safe fallback for freshly created profiles).
 *   - Non-empty set means "only these entries are permitted".
 *
 * This design lets the parent progressively restrict what the engine serves
 * without touching any gatekeeping code.
 */
data class ChildProfile(
    /** Stable UUID; generated once at creation, never changes. */
    val id: String,
    /** Display name or nickname shown to the child and parent. */
    val name: String,
    /** Age in full years, 3..18. */
    val age: Int,
    /**
     * School grade level (1–8 primary + optional preschool 0).
     * Null when the parent has not set it.
     */
    val gradeLevel: Int? = null,
    /**
     * Which academic subjects are enabled for this child.
     * Empty = all subjects allowed (default).
     */
    val enabledSubjects: Set<TaskSubject> = emptySet(),
    /**
     * Which task interaction types are enabled for this child.
     * Empty = all task types allowed (default).
     */
    val enabledTaskTypes: Set<TaskType> = emptySet(),
)
