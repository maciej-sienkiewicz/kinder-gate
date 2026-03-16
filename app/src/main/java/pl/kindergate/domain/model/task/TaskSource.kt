package pl.kindergate.domain.model.task

/**
 * Where a task originates from.
 *
 * MVP: only APP_LIBRARY (static, curated catalog bundled with the app).
 * Future: AI_GENERATED (LLM-based generation), PARENT_CUSTOM (parent-authored),
 *         EXTERNAL_API (Khan Academy, etc.)
 */
enum class TaskSource {
    APP_LIBRARY,
    // AI_GENERATED,
    // PARENT_CUSTOM,
    // EXTERNAL_API,
}
