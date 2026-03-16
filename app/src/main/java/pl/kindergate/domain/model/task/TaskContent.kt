package pl.kindergate.domain.model.task

/**
 * Polymorphic payload of a [Task] – the actual question data.
 *
 * Each subtype corresponds to one [TaskType] and is handled by the matching [TaskEvaluator].
 * Adding a new task format means:
 *   1. Add a new subtype here
 *   2. Add a matching [TaskType] enum constant
 *   3. Implement a new [TaskEvaluator]
 *   4. Register the evaluator in the DI module
 */
sealed class TaskContent {

    /**
     * Single-step integer addition: `operandA + operandB = ?`
     * Used by [TaskType.SIMPLE_ADDITION].
     */
    data class SimpleAdditionContent(
        val operandA: Int,
        val operandB: Int,
        val correctAnswer: Int,
    ) : TaskContent()
}
