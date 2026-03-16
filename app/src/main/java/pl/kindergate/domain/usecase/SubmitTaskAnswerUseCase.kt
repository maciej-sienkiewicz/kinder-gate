package pl.kindergate.domain.usecase

import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.model.task.EvaluationResult
import javax.inject.Inject

/**
 * Thin wrapper around [TaskEngine.submitAnswer].
 * See [GetNextTaskUseCase] for the rationale behind keeping use cases.
 */
class SubmitTaskAnswerUseCase @Inject constructor(
    private val taskEngine: TaskEngine,
) {
    suspend operator fun invoke(
        childId: String,
        taskId: String,
        answer: String,
    ): EvaluationResult = taskEngine.submitAnswer(childId, taskId, answer)
}
