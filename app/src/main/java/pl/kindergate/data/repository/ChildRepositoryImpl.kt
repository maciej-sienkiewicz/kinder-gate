package pl.kindergate.data.repository

import pl.kindergate.data.local.db.dao.ChildDao
import pl.kindergate.data.local.db.entity.ChildEntity
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.repository.ChildRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ChildRepositoryImpl @Inject constructor(
    private val childDao: ChildDao
) : ChildRepository {

    override suspend fun getChildren(): List<ChildProfile> =
        childDao.getAll().map { it.toDomain() }

    override suspend fun getChildById(id: String): ChildProfile? =
        childDao.getById(id)?.toDomain()

    override suspend fun upsertChild(child: ChildProfile) {
        // Preserve existing createdAtMs when updating an existing record.
        val existing = childDao.getById(child.id)
        val createdAtMs = existing?.createdAtMs ?: System.currentTimeMillis()
        childDao.upsert(ChildEntity.fromDomain(child, createdAtMs))
    }

    override suspend fun deleteChild(id: String) {
        childDao.delete(id)
    }
}
