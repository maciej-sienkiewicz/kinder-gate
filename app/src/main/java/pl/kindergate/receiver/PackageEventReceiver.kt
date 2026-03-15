package pl.kindergate.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.model.TamperType
import pl.kindergate.domain.repository.MonitoredAppsRepository
import pl.kindergate.domain.repository.SessionRepository
import javax.inject.Inject

/**
 * Listens for package removal events.
 *
 * If a monitored app is uninstalled:
 * 1. Remove it from the monitored list (cleanup)
 * 2. Log a tamper event so parent knows which app was removed
 *
 * This does NOT prevent uninstallation of monitored apps – we can't do that
 * without Device Admin, which is opt-in and has other downsides.
 * The parent is informed, not blocked.
 */
@AndroidEntryPoint
class PackageEventReceiver : BroadcastReceiver() {

    @Inject lateinit var monitoredAppsRepository: MonitoredAppsRepository
    @Inject lateinit var sessionRepository: SessionRepository

    override fun onReceive(context: Context, intent: Intent) {
        val packageName = intent.data?.schemeSpecificPart ?: return
        val action = intent.action ?: return

        if (action != Intent.ACTION_PACKAGE_REMOVED &&
            action != Intent.ACTION_PACKAGE_REPLACED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val isMonitored = monitoredAppsRepository.getEnabledPackageNames()
                    .contains(packageName)

                if (isMonitored) {
                    monitoredAppsRepository.removeMonitoredApp(packageName)
                    sessionRepository.insertTamperEvent(
                        TamperEvent(
                            type = TamperType.MONITORED_APP_UNINSTALLED,
                            detectedAtMs = System.currentTimeMillis(),
                            detail = "Package removed: $packageName"
                        )
                    )
                }
            } finally {
                pendingResult.finish()
            }
        }
    }
}
