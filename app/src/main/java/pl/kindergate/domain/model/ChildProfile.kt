package pl.kindergate.domain.model

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
     * Used to calibrate default task difficulty in future phases.
     */
    val gradeLevel: Int? = null,
)
