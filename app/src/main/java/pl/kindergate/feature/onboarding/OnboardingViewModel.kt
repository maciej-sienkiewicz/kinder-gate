package pl.kindergate.feature.onboarding

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
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.service.MonitorService
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    val totalSteps: Int = 5,
    val permissions: PermissionStatus? = null,
    val pinInput: String = "",
    val pinConfirmInput: String = "",
    val pinError: String? = null,
    val isLoading: Boolean = false
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OnboardingUiState())
    val uiState: StateFlow<OnboardingUiState> = _uiState.asStateFlow()

    val isOnboardingComplete = kotlinx.coroutines.flow.flow {
        emit(configRepository.getConfig().hasCompletedOnboarding)
    }

    init {
        refreshPermissions()
    }

    fun refreshPermissions() {
        viewModelScope.launch {
            val status = configRepository.getPermissionStatus()
            _uiState.update { it.copy(permissions = status) }
        }
    }

    fun nextStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep + 1).coerceAtMost(it.totalSteps - 1)) }
    }

    fun prevStep() {
        _uiState.update { it.copy(currentStep = (it.currentStep - 1).coerceAtLeast(0)) }
    }

    fun onPinInput(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(pinInput = pin, pinError = null) }
        }
    }

    fun onPinConfirmInput(pin: String) {
        if (pin.length <= 8 && pin.all { it.isDigit() }) {
            _uiState.update { it.copy(pinConfirmInput = pin, pinError = null) }
        }
    }

    fun savePin(): Boolean {
        val state = _uiState.value
        return when {
            state.pinInput.length < 4 -> {
                _uiState.update { it.copy(pinError = "PIN musi mieć co najmniej 4 cyfry") }
                false
            }
            state.pinInput != state.pinConfirmInput -> {
                _uiState.update { it.copy(pinError = "PINy nie są zgodne") }
                false
            }
            else -> {
                viewModelScope.launch {
                    configRepository.setPinHash(state.pinInput)
                }
                true
            }
        }
    }

    fun completeOnboarding() {
        viewModelScope.launch {
            configRepository.setOnboardingComplete()
            // Start the monitoring service
            val intent = MonitorService.startIntent(context)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }
    }

    // Intent helpers for permission settings screens

    fun getUsageStatsSettingsIntent(): Intent =
        Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)

    fun getOverlaySettingsIntent(): Intent =
        Intent(
            Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
            Uri.parse("package:${context.packageName}")
        )

    fun getBatteryOptimizationIntent(): Intent =
        Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:${context.packageName}")
        }

    fun getAccessibilitySettingsIntent(): Intent =
        Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
}
