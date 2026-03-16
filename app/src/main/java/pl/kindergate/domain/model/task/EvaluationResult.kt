package pl.kindergate.domain.model.task

/**
 * Outcome of a single answer submission.
 *
 * Both [correctAnswer] and [userAnswer] are stored as strings for uniformity across
 * task types (numeric, text, multiple-choice).
 */
data class EvaluationResult(
    val taskId: String,
    val isCorrect: Boolean,
    val correctAnswer: String,
    val userAnswer: String,
    /** Human-readable, child-friendly feedback message in Polish. */
    val feedbackMessage: String,
    val timestampMs: Long,
)
