package pl.kindergate.domain.model.task

/**
 * Core domain model representing a single educational task presented to the child.
 *
 * Design notes:
 * - [id] is stable across app sessions (catalog tasks have deterministic ids).
 * - [content] is a sealed class; callers must check [taskType] or cast via `when`.
 * - [timeLimitSeconds] null means no enforced time limit (child can take as long as needed).
 * - Kept as a plain data class (no Room annotation) – persistence adapters live in data/.
 */
data class Task(
    val id: String,
    val source: TaskSource,
    val subject: TaskSubject,
    val cognitiveSkill: CognitiveSkill,
    val difficultyMode: DifficultyMode,
    /** 1 = easiest, 3 = hardest in MVP; scale may expand in future versions. */
    val difficultyLevel: Int,
    val taskType: TaskType,
    val timeLimitSeconds: Int?,
    val content: TaskContent,
    val metadata: TaskMetadata = TaskMetadata(),
)
