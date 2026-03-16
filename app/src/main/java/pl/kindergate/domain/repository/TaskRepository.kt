package pl.kindergate.domain.repository

import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType

/**
 * Read-only access to the task catalog.
 *
 * MVP implementation: [InMemoryTaskRepository] – static catalog in code.
 * Future implementations: RoomTaskRepository (persisted catalog), RemoteTaskRepository
 * (server-driven), or a hybrid CachingTaskRepository.
 *
 * Note: write operations (parent creates custom tasks, AI saves generated tasks) will be
 * added in a future interface extension – keep this interface narrow on purpose.
 */
interface TaskRepository {

    /** Returns a specific task by its stable [id], or null if not found. */
    suspend fun getTaskById(id: String): Task?

    /** Returns all tasks belonging to a named [TaskSet] identified by [taskSetId]. */
    suspend fun getTasksForSet(taskSetId: String): List<Task>

    /**
     * Returns a random task matching [subject] and [difficultyLevel].
     * Returns null if no tasks match (caller should handle gracefully).
     */
    suspend fun getRandomTask(subject: TaskSubject, difficultyLevel: Int): Task?

    /**
     * Returns a random task that satisfies ALL of the given filters:
     *   - [subjects]       – task.subject must be in this set
     *   - [taskTypes]      – task.taskType must be in this set
     *   - [difficultyLevel] – exact match
     *
     * Returns null if no tasks match (caller applies fallback logic).
     */
    suspend fun getRandomTaskFiltered(
        subjects: Set<TaskSubject>,
        taskTypes: Set<TaskType>,
        difficultyLevel: Int,
    ): Task?
}
