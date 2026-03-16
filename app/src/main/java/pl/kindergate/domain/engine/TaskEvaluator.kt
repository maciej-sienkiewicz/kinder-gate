package pl.kindergate.domain.engine

import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task

/**
 * Stateless evaluator for a specific [Task] type.
 *
 * Each [TaskType] must have exactly one registered [TaskEvaluator].
 * Evaluators are injected into [TaskEngine] as a [Set] via Hilt multibinding,
 * allowing new task types to be added without touching [TaskEngine].
 *
 * Implementations must be stateless and thread-safe.
 */
interface TaskEvaluator {

    /** Returns true iff this evaluator can handle the given [task]. */
    fun canEvaluate(task: Task): Boolean

    /**
     * Evaluates [answer] against [task] and returns the result.
     * The [answer] string comes directly from the UI (may have leading/trailing whitespace).
     *
     * @param task must satisfy [canEvaluate]; behaviour is undefined otherwise.
     */
    fun evaluate(task: Task, answer: String): EvaluationResult
}
