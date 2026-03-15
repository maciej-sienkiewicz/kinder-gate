package pl.kindergate.domain.repository

import kotlinx.coroutines.flow.Flow
import pl.kindergate.domain.model.MonitoredApp

interface MonitoredAppsRepository {
    fun observeMonitoredApps(): Flow<List<MonitoredApp>>
    suspend fun getMonitoredApps(): List<MonitoredApp>
    suspend fun getEnabledPackageNames(): Set<String>
    suspend fun addMonitoredApp(app: MonitoredApp)
    suspend fun removeMonitoredApp(packageName: String)
    suspend fun setEnabled(packageName: String, enabled: Boolean)
    suspend fun replaceAll(apps: List<MonitoredApp>)
}
