package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.DIVISION] tasks.
 *
 * All division tasks in the catalog have exact integer results.
 * Correctness rule: [answer] trimmed, parsed as Int, compared to
 * [TaskContent.DivisionContent.correctAnswer]. Non-numeric → always incorrect.
 */
class DivisionEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.DIVISION &&
            task.content is TaskContent.DivisionContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.DivisionContent
        val trimmed = answer.trim()
        val userInt = trimmed.toIntOrNull()
        val isCorrect = userInt == content.correctAnswer

        val feedback = if (isCorrect) {
            "Świetnie! ${content.dividend} ÷ ${content.divisor} = ${content.correctAnswer}."
        } else {
            "Spróbuj jeszcze! ${content.dividend} ÷ ${content.divisor} = ${content.correctAnswer}, nie $trimmed."
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
