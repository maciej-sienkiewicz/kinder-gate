package pl.kindergate.domain.usecase

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import dagger.hilt.android.qualifiers.ApplicationContext
import pl.kindergate.domain.model.InstalledApp
import pl.kindergate.domain.repository.MonitoredAppsRepository
import javax.inject.Inject

/**
 * Returns installed user-facing apps merged with excluded (blacklisted) status.
 * Filters out system apps by default (parent can opt-in to see all).
 * Result is sorted: excluded first, then alphabetically.
 */
class GetInstalledAppsUseCase @Inject constructor(
    @ApplicationContext private val context: Context,
    private val monitoredAppsRepository: MonitoredAppsRepository
) {
    suspend operator fun invoke(includeSystemApps: Boolean = false): List<InstalledApp> {
        val pm = context.packageManager
        val excludedPackages = monitoredAppsRepository.getEnabledPackageNames()

        val flags = PackageManager.GET_META_DATA
        val installedPackages = pm.getInstalledApplications(flags)

        return installedPackages
            .filter { appInfo ->
                // Keep: user-installed apps (no FLAG_SYSTEM) OR system apps if requested
                includeSystemApps || (appInfo.flags and ApplicationInfo.FLAG_SYSTEM == 0)
            }
            .filter { appInfo ->
                // Filter out KinderGate itself
                appInfo.packageName != context.packageName
            }
            .filter { appInfo ->
                // Must have a launcher intent (visible to user)
                pm.getLaunchIntentForPackage(appInfo.packageName) != null
            }
            .map { appInfo ->
                InstalledApp(
                    packageName = appInfo.packageName,
                    label = pm.getApplicationLabel(appInfo).toString(),
                    icon = try { pm.getApplicationIcon(appInfo.packageName) } catch (e: Exception) { null },
                    isSystemApp = (appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0,
                    isExcluded = appInfo.packageName in excludedPackages
                )
            }
            .sortedWith(compareByDescending<InstalledApp> { it.isExcluded }.thenBy { it.label })
    }
}
