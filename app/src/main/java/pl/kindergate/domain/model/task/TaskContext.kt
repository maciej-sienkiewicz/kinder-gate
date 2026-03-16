package pl.kindergate.domain.model.task

/**
 * Runtime hint passed to [TaskEngine.getNextTask] to influence task selection.
 *
 * All fields are optional – the engine falls back to sensible defaults when null.
 *
 * MVP: only [preferredDifficultyLevel].
 * Future: [blockedAppPackage] (tailor difficulty to app type),
 *         [dailyGateCount] (ease up late in the day),
 *         [timeOfDayHour], subject rotation preferences, etc.
 */
data class TaskContext(
    /**
     * Explicit difficulty level request (1..3 in MVP).
     * Overrides the engine's adaptive choice only if the child has no history yet.
     */
    val preferredDifficultyLevel: Int? = null,
)
