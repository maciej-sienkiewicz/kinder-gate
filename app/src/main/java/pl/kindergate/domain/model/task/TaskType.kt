package pl.kindergate.domain.model.task

/**
 * Concrete task format / interaction pattern.
 * Determines which [TaskContent] subtype is present and which [TaskEvaluator] handles it.
 *
 * MVP: only SIMPLE_ADDITION.
 * Future: SIMPLE_SUBTRACTION, MULTIPLICATION, DIVISION, WORD_PROBLEM,
 *         TRUE_FALSE, MULTIPLE_CHOICE, FILL_IN_THE_BLANK, …
 */
enum class TaskType {
    SIMPLE_ADDITION,
    LETTER_TRACING,
    // SIMPLE_SUBTRACTION,
    // MULTIPLICATION,
    // DIVISION,
    // WORD_PROBLEM,
    // TRUE_FALSE,
    // MULTIPLE_CHOICE,
}
