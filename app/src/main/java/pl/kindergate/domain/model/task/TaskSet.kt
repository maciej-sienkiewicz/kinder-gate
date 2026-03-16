package pl.kindergate.domain.model.task

/**
 * Named, versioned collection of [Task]s sharing a [subject] and [difficultyRange].
 *
 * MVP: three static sets (addition level 1/2/3) defined in InMemoryTaskRepository.
 * Future: parent-curated sets, AI-generated daily packs, curriculum-aligned sets.
 */
data class TaskSet(
    val id: String,
    val name: String,
    val description: String,
    val subject: TaskSubject,
    /** Inclusive range of difficulty levels covered by this set. */
    val difficultyRange: IntRange,
    val taskIds: List<String>,
    val version: Int = 1,
)
