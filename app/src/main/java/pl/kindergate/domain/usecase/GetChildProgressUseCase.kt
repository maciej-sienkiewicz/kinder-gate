package pl.kindergate.domain.usecase

import pl.kindergate.domain.engine.TaskEngine
import pl.kindergate.domain.model.task.ChildProgress
import javax.inject.Inject

/**
 * Thin wrapper around [TaskEngine.getChildProgress].
 * See [GetNextTaskUseCase] for the rationale behind keeping use cases.
 */
class GetChildProgressUseCase @Inject constructor(
    private val taskEngine: TaskEngine,
) {
    suspend operator fun invoke(childId: String): ChildProgress =
        taskEngine.getChildProgress(childId)
}
