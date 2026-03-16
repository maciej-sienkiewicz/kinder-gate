package pl.kindergate.data.engine

import pl.kindergate.domain.engine.TaskEvaluator
import pl.kindergate.domain.model.task.EvaluationResult
import pl.kindergate.domain.model.task.Task
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import javax.inject.Inject

/**
 * Evaluates [TaskType.SIMPLE_ADDITION] tasks.
 *
 * Correctness rule: [answer] trimmed, parsed as Int, compared to [TaskContent.SimpleAdditionContent.correctAnswer].
 * Non-numeric answers are always incorrect.
 *
 * Feedback messages are in Polish, child-friendly and non-punishing.
 */
class SimpleAdditionEvaluator @Inject constructor() : TaskEvaluator {

    override fun canEvaluate(task: Task): Boolean =
        task.taskType == TaskType.SIMPLE_ADDITION &&
            task.content is TaskContent.SimpleAdditionContent

    override fun evaluate(task: Task, answer: String): EvaluationResult {
        val content = task.content as TaskContent.SimpleAdditionContent
        val trimmed = answer.trim()
        val userInt = trimmed.toIntOrNull()
        val isCorrect = userInt == content.correctAnswer

        val feedback = if (isCorrect) {
            "Dobrze! ${content.operandA} + ${content.operandB} = ${content.correctAnswer}."
        } else {
            "Następnym razem! ${content.operandA} + ${content.operandB} = ${content.correctAnswer}, nie $trimmed."
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
