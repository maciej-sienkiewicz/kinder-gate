package pl.kindergate.feature.apppicker

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.InstalledApp
import pl.kindergate.domain.usecase.GetInstalledAppsUseCase
import pl.kindergate.domain.usecase.ManageMonitoredAppsUseCase
import javax.inject.Inject

data class AppPickerUiState(
    val apps: List<InstalledApp> = emptyList(),
    val selectedPackages: Set<String> = emptySet(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false
) {
    val filteredApps: List<InstalledApp> get() = if (searchQuery.isBlank()) apps
        else apps.filter { it.label.contains(searchQuery, ignoreCase = true) }
}

@HiltViewModel
class AppPickerViewModel @Inject constructor(
    private val getInstalledAppsUseCase: GetInstalledAppsUseCase,
    private val manageMonitoredAppsUseCase: ManageMonitoredAppsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppPickerUiState())
    val uiState: StateFlow<AppPickerUiState> = _uiState.asStateFlow()

    init {
        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            val apps = getInstalledAppsUseCase()
            val selectedPackages = apps.filter { it.isMonitored }.map { it.packageName }.toSet()
            _uiState.update { it.copy(apps = apps, selectedPackages = selectedPackages, isLoading = false) }
        }
    }

    fun toggleApp(packageName: String) {
        _uiState.update { state ->
            val updated = if (packageName in state.selectedPackages) {
                state.selectedPackages - packageName
            } else {
                state.selectedPackages + packageName
            }
            state.copy(selectedPackages = updated)
        }
    }

    fun onSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun saveSelection(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            try {
                manageMonitoredAppsUseCase.saveSelection(
                    packageNames = _uiState.value.selectedPackages,
                    existingApps = _uiState.value.apps
                        .filter { it.isMonitored }
                        .map { app ->
                            pl.kindergate.domain.model.MonitoredApp(
                                packageName = app.packageName,
                                appLabel = app.label
                            )
                        }
                )
                onDone()
            } finally {
                _uiState.update { it.copy(isSaving = false) }
            }
        }
    }
}
