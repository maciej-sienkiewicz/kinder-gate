package pl.kindergate.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.domain.repository.ChildRepository
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.usecase.GetChildByIdUseCase
import pl.kindergate.domain.usecase.UpsertChildUseCase
import javax.inject.Inject

// ── Tile state models ─────────────────────────────────────────────────────────

data class SubjectTileState(
    val subject: TaskSubject,
    val label: String,
    val isEnabled: Boolean,
)

data class TaskTypeTileState(
    val taskType: TaskType,
    val label: String,
    val isEnabled: Boolean,
)

data class CategoryConfigUiState(
    val subjectTiles: List<SubjectTileState> = emptyList(),
    val taskTypeTiles: List<TaskTypeTileState> = emptyList(),
    val isLoading: Boolean = true,
    /** Null = no child profile found yet (parent skipped child setup). */
    val childName: String? = null,
)

/**
 * ViewModel for the parent-facing category / task-type configuration screen.
 *
 * Loads the active child profile, exposes tile states for all [TaskSubject] and
 * [TaskType] entries, and persists changes via [UpsertChildUseCase] on every toggle.
 *
 * Invariant: empty [enabledSubjects]/[enabledTaskTypes] in [ChildProfile] means "all
 * enabled". When the parent enables ALL tiles the ViewModel stores an empty set so
 * that new subjects/types added in future releases are automatically enabled.
 */
@HiltViewModel
class CategoryConfigViewModel @Inject constructor(
    private val configRepository: ConfigRepository,
    private val getChildByIdUseCase: GetChildByIdUseCase,
    private val upsertChildUseCase: UpsertChildUseCase,
    private val childRepository: ChildRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryConfigUiState())
    val uiState: StateFlow<CategoryConfigUiState> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            val childId = configRepository.getSelectedChildId()
            val profile = if (childId != null) {
                getChildByIdUseCase(childId)
            } else {
                childRepository.getChildren().firstOrNull()
            }

            if (profile == null) {
                _uiState.update { it.copy(isLoading = false) }
                return@launch
            }

            // Treat empty set as "all enabled": build tile list with all entries ON.
            val enabledSubjects = profile.enabledSubjects.takeIf { it.isNotEmpty() }
                ?: TaskSubject.entries.toSet()
            val enabledTypes = profile.enabledTaskTypes.takeIf { it.isNotEmpty() }
                ?: TaskType.entries.toSet()

            _uiState.update {
                it.copy(
                    isLoading = false,
                    childName = profile.name,
                    subjectTiles = TaskSubject.entries.map { subject ->
                        SubjectTileState(
                            subject = subject,
                            label = subject.displayLabel(),
                            isEnabled = subject in enabledSubjects,
                        )
                    },
                    taskTypeTiles = TaskType.entries.map { taskType ->
                        TaskTypeTileState(
                            taskType = taskType,
                            label = taskType.displayLabel(),
                            isEnabled = taskType in enabledTypes,
                        )
                    },
                )
            }
        }
    }

    fun onSubjectToggled(subject: TaskSubject) {
        _uiState.update { state ->
            state.copy(
                subjectTiles = state.subjectTiles.map { tile ->
                    if (tile.subject == subject) tile.copy(isEnabled = !tile.isEnabled) else tile
                },
            )
        }
        persistChanges()
    }

    fun onTaskTypeToggled(taskType: TaskType) {
        _uiState.update { state ->
            state.copy(
                taskTypeTiles = state.taskTypeTiles.map { tile ->
                    if (tile.taskType == taskType) tile.copy(isEnabled = !tile.isEnabled) else tile
                },
            )
        }
        persistChanges()
    }

    private fun persistChanges() {
        viewModelScope.launch {
            val childId = configRepository.getSelectedChildId()
                ?: childRepository.getChildren().firstOrNull()?.id
                ?: return@launch

            val current = getChildByIdUseCase(childId) ?: return@launch
            val state = _uiState.value

            val enabledSubjects = state.subjectTiles
                .filter { it.isEnabled }
                .map { it.subject }
                .toSet()

            val enabledTypes = state.taskTypeTiles
                .filter { it.isEnabled }
                .map { it.taskType }
                .toSet()

            // Store empty set when everything is enabled to stay future-proof.
            val subjectsToStore =
                if (enabledSubjects.size == TaskSubject.entries.size) emptySet() else enabledSubjects
            val typesToStore =
                if (enabledTypes.size == TaskType.entries.size) emptySet() else enabledTypes

            upsertChildUseCase(
                current.copy(
                    enabledSubjects = subjectsToStore,
                    enabledTaskTypes = typesToStore,
                ),
            )
        }
    }
}

// ── Display label helpers ─────────────────────────────────────────────────────

fun TaskSubject.displayLabel(): String = when (this) {
    TaskSubject.MATH    -> "Matematyka"
    TaskSubject.WRITING -> "Pisanie"
}

fun TaskType.displayLabel(): String = when (this) {
    TaskType.SIMPLE_ADDITION    -> "Dodawanie"
    TaskType.SIMPLE_SUBTRACTION -> "Odejmowanie"
    TaskType.MULTIPLICATION     -> "Mnożenie"
    TaskType.DIVISION           -> "Dzielenie"
    TaskType.MIXED_OPERATIONS   -> "Działania mieszane"
    TaskType.LETTER_TRACING     -> "Rysowanie liter"
}
