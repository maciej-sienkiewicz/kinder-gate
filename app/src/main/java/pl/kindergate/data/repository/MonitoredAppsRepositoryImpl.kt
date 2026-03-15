package pl.kindergate.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import pl.kindergate.data.local.db.dao.MonitoredAppDao
import pl.kindergate.data.local.db.entity.MonitoredAppEntity
import pl.kindergate.domain.model.MonitoredApp
import pl.kindergate.domain.repository.MonitoredAppsRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MonitoredAppsRepositoryImpl @Inject constructor(
    private val dao: MonitoredAppDao
) : MonitoredAppsRepository {

    override fun observeMonitoredApps(): Flow<List<MonitoredApp>> =
        dao.observeAll().map { it.map(MonitoredAppEntity::toDomain) }

    override suspend fun getMonitoredApps(): List<MonitoredApp> =
        dao.getAll().map(MonitoredAppEntity::toDomain)

    override suspend fun getEnabledPackageNames(): Set<String> =
        dao.getEnabledPackageNames().toSet()

    override suspend fun addMonitoredApp(app: MonitoredApp) =
        dao.insert(MonitoredAppEntity.fromDomain(app))

    override suspend fun removeMonitoredApp(packageName: String) =
        dao.delete(packageName)

    override suspend fun setEnabled(packageName: String, enabled: Boolean) =
        dao.setEnabled(packageName, enabled)

    override suspend fun replaceAll(apps: List<MonitoredApp>) =
        dao.replaceAll(apps.map(MonitoredAppEntity::fromDomain))
}
