package pl.kindergate.feature.dashboard

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
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
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.MonitoredApp
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.domain.model.TamperEvent
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.repository.MonitoredAppsRepository
import pl.kindergate.domain.repository.SessionRepository
import pl.kindergate.domain.usecase.GetChildrenUseCase
import pl.kindergate.service.MonitorService
import java.util.concurrent.TimeUnit
import javax.inject.Inject

data class DashboardUiState(
    val permissionStatus: PermissionStatus? = null,
    val monitoredApps: List<MonitoredApp> = emptyList(),
    val todaySessions: List<BlockSession> = emptyList(),
    val recentTamperEvents: List<TamperEvent> = emptyList(),
    val isServiceRunning: Boolean = false,
    val unacknowledgedTamperCount: Int = 0,
    /** The currently active child profile; null when not yet configured. */
    val childProfile: ChildProfile? = null,
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val monitoredAppsRepository: MonitoredAppsRepository,
    private val sessionRepository: SessionRepository,
    private val getChildrenUseCase: GetChildrenUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState: StateFlow<DashboardUiState> = _uiState.asStateFlow()

    init {
        observeData()
        refreshPermissions()
        refreshChildProfile()
        // Ensure service is running whenever dashboard is opened
        startService()
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

    fun refreshChildProfile() {
        viewModelScope.launch {
            val selectedId = configRepository.getSelectedChildId()
            val profile = if (selectedId != null) {
                getChildrenUseCase().find { it.id == selectedId }
                    ?: getChildrenUseCase().firstOrNull()
            } else {
                getChildrenUseCase().firstOrNull()
            }
            _uiState.update { it.copy(childProfile = profile) }
        }
    }

    fun startService() {
        val intent = MonitorService.startIntent(context)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
        _uiState.update { it.copy(isServiceRunning = true) }
    }

    fun stopService() {
        context.startService(
            Intent(context, MonitorService::class.java).apply {
                action = MonitorService.ACTION_STOP
            }
        )
        _uiState.update { it.copy(isServiceRunning = false) }
    }

    // Intent helpers for opening permission settings from the dashboard
    fun getUsageStatsSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun getOverlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun getNotificationSettingsIntent(): Intent =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:${context.packageName}")
            }
        }

    fun getBatteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun getAccessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
