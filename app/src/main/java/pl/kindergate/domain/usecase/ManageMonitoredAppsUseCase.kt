package pl.kindergate.domain.usecase

import pl.kindergate.domain.model.MonitoredApp
import pl.kindergate.domain.repository.MonitoredAppsRepository
import javax.inject.Inject

/**
 * Replaces the entire monitored apps list in one atomic operation.
 * Used when parent finishes the app picker: we replace all, not diff.
 * This avoids partial update bugs and simplifies the picker UI.
 */
class ManageMonitoredAppsUseCase @Inject constructor(
    private val repository: MonitoredAppsRepository
) {
    suspend fun saveSelection(packageNames: Set<String>, existingApps: List<MonitoredApp>) {
        val existingByPackage = existingApps.associateBy { it.packageName }
        val updated = packageNames.map { pkg ->
            existingByPackage[pkg] ?: MonitoredApp(
                packageName = pkg,
                appLabel = pkg // label enriched in UI layer from PackageManager
            )
        }
        repository.replaceAll(updated)
    }

    suspend fun remove(packageName: String) = repository.removeMonitoredApp(packageName)

    suspend fun toggleEnabled(packageName: String, enabled: Boolean) =
        repository.setEnabled(packageName, enabled)
}
