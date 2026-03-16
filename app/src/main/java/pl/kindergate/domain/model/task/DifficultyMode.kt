package pl.kindergate.domain.model.task

/**
 * How the difficulty level is determined.
 *
 * MVP: MANUAL – difficulty set explicitly per task in the catalog.
 * Future: ADAPTIVE – engine adjusts level based on child's history;
 *         PARENT_DEFINED – parent sets a fixed level from the dashboard.
 */
enum class DifficultyMode {
    MANUAL,
    // ADAPTIVE,
    // PARENT_DEFINED,
}
