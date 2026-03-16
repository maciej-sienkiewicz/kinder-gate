package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.LETTER_TRACING] tasks.
 *
 * The answer is the drawing coverage percentage (0.0–100.0) encoded as a String.
 * A task is considered correct when coverage ≥ [REQUIRED_COVERAGE_PERCENT].
 *
 * Feedback messages are in Polish, child-friendly and encouraging.
 */
class LetterTracingEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.LETTER_TRACING &&
            task.content is TaskContent.LetterTracingContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.LetterTracingContent
        val coverage = answer.toFloatOrNull() ?: 0f
        val isCorrect = coverage >= REQUIRED_COVERAGE_PERCENT

        val feedback = if (isCorrect) {
            "Super! Literka ${content.letter} narysowana świetnie!"
        } else {
            "Prawie! Spróbuj jeszcze raz narysować literę ${content.letter}."
        }

        return EvaluationResult(
            taskId = task.id,
            isCorrect = isCorrect,
            correctAnswer = "$REQUIRED_COVERAGE_PERCENT%",
            userAnswer = "%.1f%%".format(coverage),
            feedbackMessage = feedback,
            timestampMs = System.currentTimeMillis(),
        )
    }

    companion object {
        const val REQUIRED_COVERAGE_PERCENT = 90f
    }
}
