package pl.kindergate.domain.usecase

import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContext
import javax.inject.Inject

/**
 * Thin wrapper around [TaskEngine.getNextTask].
 *
 * Keeping use cases separate from the engine gives us:
 * - isolated unit tests without mocking the full engine,
 * - a stable call site in the ViewModel even if the engine interface evolves,
 * - a natural extension point for pre/post processing (analytics, logging, etc.).
 */
class GetNextTaskUseCase @Inject constructor(
    private val taskEngine: TaskEngine,
) {
    suspend operator fun invoke(childId: String, context: TaskContext? = null): Task =
        taskEngine.getNextTask(childId, context)
}
