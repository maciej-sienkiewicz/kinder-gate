package pl.kindergate.domain.model.task

/**
 * Aggregated progress snapshot for a single child.
 *
 * MVP: simple totals only, no persistence (in-memory per session).
 * Future: per-subject breakdown, streak tracking, persistence via Room.
 */
data class ChildProgress(
    val childId: String,
    val totalAnswered: Int,
    val correctCount: Int,
    /** 0.0–1.0; 0.0 when no answers yet. */
    val accuracy: Double,
    /**
     * Per-difficulty-level breakdown.
     * Empty map in MVP; populated in future versions.
     */
    val progressByLevel: Map<Int, LevelProgress> = emptyMap(),
)

/** Per-level sub-summary inside [ChildProgress]. */
data class LevelProgress(
    val level: Int,
    val totalAnswered: Int,
    val correctCount: Int,
)
