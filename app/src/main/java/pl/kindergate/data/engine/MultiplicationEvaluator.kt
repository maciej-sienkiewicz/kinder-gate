package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.MULTIPLICATION] tasks.
 *
 * Correctness rule: [answer] trimmed, parsed as Int, compared to
 * [TaskContent.MultiplicationContent.correctAnswer]. Non-numeric → always incorrect.
 */
class MultiplicationEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.MULTIPLICATION &&
            task.content is TaskContent.MultiplicationContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.MultiplicationContent
        val trimmed = answer.trim()
        val userInt = trimmed.toIntOrNull()
        val isCorrect = userInt == content.correctAnswer

        val feedback = if (isCorrect) {
            "Super! ${content.factorA} × ${content.factorB} = ${content.correctAnswer}."
        } else {
            "Jeszcze raz! ${content.factorA} × ${content.factorB} = ${content.correctAnswer}, nie $trimmed."
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
