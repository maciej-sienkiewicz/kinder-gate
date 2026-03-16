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

    /** `minuend − subtrahend = ?`  Used by [TaskType.SIMPLE_SUBTRACTION]. */
    data class SubtractionContent(
        val minuend: Int,
        val subtrahend: Int,
        val correctAnswer: Int,
    ) : TaskContent()

    /** `factorA × factorB = ?`  Used by [TaskType.MULTIPLICATION]. */
    data class MultiplicationContent(
        val factorA: Int,
        val factorB: Int,
        val correctAnswer: Int,
    ) : TaskContent()

    /** `dividend ÷ divisor = ?`  (always exact integer result)  Used by [TaskType.DIVISION]. */
    data class DivisionContent(
        val dividend: Int,
        val divisor: Int,
        val correctAnswer: Int,
    ) : TaskContent()

    /**
     * Arbitrary math expression with parentheses / mixed operations.
     * [expression] is a ready-to-display string (e.g. "(2+3)×4").
     * Used by [TaskType.MIXED_OPERATIONS].
     */
    data class ExpressionContent(
        val expression: String,
        val correctAnswer: Int,
    ) : TaskContent()

    /**
     * Finger-tracing task: child traces a letter shown as a dashed template.
     * Used by [TaskType.LETTER_TRACING].
     * Evaluated by checking that drawing coverage ≥ 90 %.
     */
    data class LetterTracingContent(
        val letter: Char,
    ) : TaskContent()
}
