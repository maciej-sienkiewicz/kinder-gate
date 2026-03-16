package pl.kindergate.domain.usecase

import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.repository.ChildRepository
import javax.inject.Inject

class GetChildrenUseCase @Inject constructor(
    private val childRepository: ChildRepository
) {
    suspend operator fun invoke(): List<ChildProfile> = childRepository.getChildren()
}
