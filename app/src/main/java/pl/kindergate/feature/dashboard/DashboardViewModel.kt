package pl.kindergate.feature.dashboard

import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.BlockSession
import pl.kindergate.domain.model.MonitoredApp
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.repository.MonitoredAppsRepository
import pl.kindergate.domain.repository.SessionRepository
import pl.kindergate.service.MonitorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DashboardUiState(
    val permissionStatus: PermissionStatus? = null,
    val monitoredApps: List<MonitoredApp> = emptyList(),
    val todaySessions: List<BlockSession> = emptyList(),
    val recentTamperEvents: List<TamperEvent> = emptyList(),
    val isServiceRunning: Boolean = false,
    val unacknowledgedTamperCount: Int = 0
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val sessionRepository: SessionRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refreshPermissions()
    }

    private fun observeData() {
        viewModelScope.launch {
            combine(
                monitoredAppsRepository.observeMonitoredApps(),
                sessionRepository.observeTodaySessions(),
                sessionRepository.observeTamperEvents(
                    System.currentTimeMillis() - TimeUnit.DAYS.toMillis(7)
                )
            ) { apps, sessions, tampers ->
                Triple(apps, sessions, tampers)
            }.collect { (apps, sessions, tampers) ->
                _uiState.update { state ->
                    state.copy(
                        monitoredApps = apps,
                        todaySessions = sessions,
                        recentTamperEvents = tampers
                    )
                }
            }
        }
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            val status = configRepository.getPermissionStatus()
            val tamperCount = sessionRepository.getUnacknowledgedTamperCount()
            _uiState.update { it.copy(permissionStatus = status, unacknowledgedTamperCount = tamperCount) }
        }
    }

    fun startService() {
        val intent = MonitorService.startIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }

    fun stopService() {
        context.startService(
            Intent(context, MonitorService::class.java).apply {
                action = MonitorService.ACTION_STOP
            }
        )
    }
}
