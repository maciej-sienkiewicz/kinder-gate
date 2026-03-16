package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.SIMPLE_SUBTRACTION] tasks.
 *
 * Correctness rule: [answer] trimmed, parsed as Int, compared to
 * [TaskContent.SubtractionContent.correctAnswer]. Non-numeric → always incorrect.
 */
class SubtractionEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.SIMPLE_SUBTRACTION &&
            task.content is TaskContent.SubtractionContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.SubtractionContent
        val trimmed = answer.trim()
        val userInt = trimmed.toIntOrNull()
        val isCorrect = userInt == content.correctAnswer

        val feedback = if (isCorrect) {
            "Brawo! ${content.minuend} − ${content.subtrahend} = ${content.correctAnswer}."
        } else {
            "Prawie! ${content.minuend} − ${content.subtrahend} = ${content.correctAnswer}, nie $trimmed."
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
