package pl.kindergate.domain.usecase

import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.repository.ChildRepository
import javax.inject.Inject

class GetChildByIdUseCase @Inject constructor(
    private val childRepository: ChildRepository
) {
    suspend operator fun invoke(id: String): ChildProfile? = childRepository.getChildById(id)
}
