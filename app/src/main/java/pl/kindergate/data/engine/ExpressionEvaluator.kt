package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.MIXED_OPERATIONS] tasks (parentheses, mixed operators).
 *
 * The correct answer is stored in [TaskContent.ExpressionContent.correctAnswer] –
 * the evaluator never executes the expression string itself (safe, no eval).
 */
class ExpressionEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.MIXED_OPERATIONS &&
            task.content is TaskContent.ExpressionContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.ExpressionContent
        val trimmed = answer.trim()
        val userInt = trimmed.toIntOrNull()
        val isCorrect = userInt == content.correctAnswer

        val feedback = if (isCorrect) {
            "Znakomicie! ${content.expression} = ${content.correctAnswer}."
        } else {
            "Prawie! ${content.expression} = ${content.correctAnswer}, nie $trimmed."
        }

        return EvaluationResult(
            taskId = task.id,
            isCorrect = isCorrect,
            correctAnswer = content.correctAnswer.toString(),
            userAnswer = trimmed,
            feedbackMessage = feedback,
            timestampMs = System.currentTimeMillis(),
        )
    }
}
