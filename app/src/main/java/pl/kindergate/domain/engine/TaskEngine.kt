package pl.kindergate.domain.engine

import pl.kindergate.domain.model.task.ChildProgress
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContext

/**
 * Orchestrates task selection, evaluation delegation and progress tracking.
 *
 * MVP implementation: [SimpleTaskEngine] – in-memory, adaptive difficulty.
 * Future: a more sophisticated engine with spaced-repetition, multi-subject
 * scheduling, and persistent history via Room.
 *
 * Concurrency: all methods are suspending; implementations must be thread-safe
 * (multiple coroutines may call concurrently from BlockingActivity restarts).
 */
interface TaskEngine {

    /**
     * Selects the next task for [childId] based on history and optional [context].
     *
     * @throws IllegalStateException if no tasks are available (should never happen
     *   in production with a non-empty catalog).
     */
    suspend fun getNextTask(childId: String, context: TaskContext? = null): Task

    /**
     * Evaluates [answer] for [taskId], records the result in the child's history,
     * and returns the [EvaluationResult].
     *
     * @throws IllegalArgumentException if [taskId] is unknown.
     * @throws IllegalStateException if no evaluator supports the task's type.
     */
    suspend fun submitAnswer(childId: String, taskId: String, answer: String): EvaluationResult

    /** Returns the current aggregated progress for [childId]. */
    suspend fun getChildProgress(childId: String): ChildProgress
}
