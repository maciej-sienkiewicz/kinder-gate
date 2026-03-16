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
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.usecase.UpsertChildUseCase
import pl.kindergate.service.MonitorService
import java.util.UUID
import javax.inject.Inject

data class OnboardingUiState(
    val currentStep: Int = 0,
    /** Steps: 0=Welcome, 1=Permissions, 2=PIN, 3=ChildProfile, 4=AppPicker, 5=Completion */
    val totalSteps: Int = 6,
    val permissions: PermissionStatus? = null,
    val pinInput: String = "",
    val pinConfirmInput: String = "",
    val pinError: String? = null,
    val isLoading: Boolean = false,
    // Child profile step
    val childName: String = "",
    val childAgeInput: String = "",
    val childGradeLevelInput: String = "",
    val childNameError: String? = null,
    val childAgeError: String? = null,
)

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val configRepository: ConfigRepository,
    private val upsertChildUseCase: UpsertChildUseCase,
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

    fun onChildNameInput(value: String) {
        _uiState.update { it.copy(childName = value, childNameError = null) }
    }

    fun onChildAgeInput(value: String) {
        if (value.length <= 2 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(childAgeInput = value, childAgeError = null) }
        }
    }

    fun onChildGradeLevelInput(value: String) {
        if (value.length <= 1 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(childGradeLevelInput = value) }
        }
    }

    /**
     * Validates child profile fields and saves to repository.
     * Returns true on success, false when there are validation errors.
     */
    fun saveChildProfile(): Boolean {
        val state = _uiState.value
        val name = state.childName.trim()
        val age = state.childAgeInput.toIntOrNull()
        val gradeLevel = state.childGradeLevelInput.toIntOrNull()

        var hasError = false
        if (name.isBlank()) {
            _uiState.update { it.copy(childNameError = "Imię nie może być puste") }
            hasError = true
        }
        if (age == null || age < 3 || age > 18) {
            _uiState.update { it.copy(childAgeError = "Wiek musi być liczbą od 3 do 18") }
            hasError = true
        }
        if (hasError) return false

        val profile = ChildProfile(
            id = UUID.randomUUID().toString(),
            name = name,
            age = age!!,
            gradeLevel = gradeLevel,
        )
        viewModelScope.launch {
            upsertChildUseCase(profile)
            configRepository.setSelectedChildId(profile.id)
        }
        return true
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
