package pl.kindergate.feature.children

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.usecase.GetChildrenUseCase
import pl.kindergate.domain.usecase.UpsertChildUseCase
import java.util.UUID
import javax.inject.Inject

data class ChildProfileUiState(
    val name: String = "",
    val ageInput: String = "",
    val gradeLevelInput: String = "",
    val nameError: String? = null,
    val ageError: String? = null,
    val isSaving: Boolean = false,
    val isSaved: Boolean = false,
)

@HiltViewModel
class ChildProfileViewModel @Inject constructor(
    private val getChildrenUseCase: GetChildrenUseCase,
    private val upsertChildUseCase: UpsertChildUseCase,
    private val configRepository: ConfigRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChildProfileUiState())
    val uiState: StateFlow<ChildProfileUiState> = _uiState.asStateFlow()

    /**
     * The id of the profile being edited.
     * Null means we'll generate a new UUID on first save.
     */
    private var editingId: String? = null

    init {
        loadExistingProfile()
    }

    private fun loadExistingProfile() {
        viewModelScope.launch {
            val children = getChildrenUseCase()
            children.firstOrNull()?.let { profile ->
                editingId = profile.id
                _uiState.update {
                    it.copy(
                        name = profile.name,
                        ageInput = profile.age.toString(),
                        gradeLevelInput = profile.gradeLevel?.toString() ?: "",
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) {
        _uiState.update { it.copy(name = value, nameError = null) }
    }

    fun onAgeChange(value: String) {
        if (value.length <= 2 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(ageInput = value, ageError = null) }
        }
    }

    fun onGradeLevelChange(value: String) {
        if (value.length <= 1 && value.all { it.isDigit() }) {
            _uiState.update { it.copy(gradeLevelInput = value) }
        }
    }

    /**
     * Validates and saves the profile.
     * Returns true when save succeeded; false when there are validation errors.
     */
    fun save(): Boolean {
        val state = _uiState.value
        val name = state.name.trim()
        val age = state.ageInput.toIntOrNull()
        val gradeLevel = state.gradeLevelInput.toIntOrNull()

        var hasError = false
        if (name.isBlank()) {
            _uiState.update { it.copy(nameError = "Imię nie może być puste") }
            hasError = true
        }
        if (age == null || age < 3 || age > 18) {
            _uiState.update { it.copy(ageError = "Wiek musi być liczbą od 3 do 18") }
            hasError = true
        }
        if (hasError) return false

        val id = editingId ?: UUID.randomUUID().toString().also { editingId = it }
        val profile = ChildProfile(
            id = id,
            name = name,
            age = age!!,
            gradeLevel = gradeLevel,
        )

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            upsertChildUseCase(profile)
            configRepository.setSelectedChildId(profile.id)
            _uiState.update { it.copy(isSaving = false, isSaved = true) }
        }
        return true
    }
}
