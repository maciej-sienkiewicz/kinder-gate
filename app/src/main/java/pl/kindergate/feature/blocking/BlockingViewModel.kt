package pl.kindergate.feature.blocking

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import pl.kindergate.domain.repository.ChildRepository
import pl.kindergate.domain.repository.ConfigRepository
import pl.kindergate.domain.usecase.GetNextTaskUseCase
import pl.kindergate.domain.usecase.SubmitTaskAnswerUseCase
import pl.kindergate.feature.tasks.TaskUiState
import javax.inject.Inject

/**
 * ViewModel for [BlockingActivity] / [BlockingScreen].
 *
 * Responsibilities:
 *  - Resolve the active child ID from [ConfigRepository] so that the task engine
 *    can apply the correct subject/type allow-list for that child.
 *  - Load the next task from [GetNextTaskUseCase] on creation.
 *  - Evaluate the answer via [SubmitTaskAnswerUseCase] on submit.
 *  - Emit [BlockingEvent.TaskSolvedCorrectly] so [BlockingScreen] calls [onAcknowledge].
 *
 * Error handling: unexpected engine failures emit [TaskUiState.Error] and immediately
 * unblock the child so they are never stuck on the blocking screen.
 */
@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val getNextTaskUseCase: GetNextTaskUseCase,
    private val submitTaskAnswerUseCase: SubmitTaskAnswerUseCase,
    private val configRepository: ConfigRepository,
    private val childRepository: ChildRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    private val _events = MutableSharedFlow<BlockingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BlockingEvent> = _events.asSharedFlow()

    /**
     * The child ID used for every engine call in this session.
     * Resolved once on init; null until resolution completes (tasks are not
     * fetched until this is known).
     */
    private var resolvedChildId: String? = null

    init {
        viewModelScope.launch {
            resolvedChildId = resolveChildId()
            loadNextTask()
        }
    }

    /**
     * Returns the selected child's UUID, or falls back to the first stored profile.
     * Returns null only when no profile exists at all (first-run before onboarding completes).
     */
    private suspend fun resolveChildId(): String? {
        configRepository.getSelectedChildId()?.let { return it }
        return childRepository.getChildren().firstOrNull()?.id
    }

    fun loadNextTask() {
        val childId = resolvedChildId ?: return  // still resolving; init will call us again
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            runCatching { getNextTaskUseCase(childId) }
                .onSuccess { task -> _uiState.value = TaskUiState.ShowingTask(task = task) }
                .onFailure { _uiState.value = TaskUiState.Error(it.message) }
        }
    }

    fun updateAnswer(answer: String) {
        val current = _uiState.value as? TaskUiState.ShowingTask ?: return
        _uiState.value = current.copy(currentAnswer = answer, feedback = null, isIncorrect = false)
    }

    fun submitAnswer() {
        val current = _uiState.value as? TaskUiState.ShowingTask ?: return
        val childId = resolvedChildId ?: run {
            _events.tryEmit(BlockingEvent.TaskSolvedCorrectly)
            return
        }
        viewModelScope.launch {
            runCatching {
                submitTaskAnswerUseCase(childId, current.task.id, current.currentAnswer)
            }.onSuccess { result ->
                if (result.isCorrect) {
                    _events.tryEmit(BlockingEvent.TaskSolvedCorrectly)
                } else {
                    _uiState.value = current.copy(
                        currentAnswer = "",
                        feedback = result.feedbackMessage,
                        isIncorrect = true,
                    )
                }
            }.onFailure {
                _events.tryEmit(BlockingEvent.TaskSolvedCorrectly)
            }
        }
    }
}

sealed class BlockingEvent {
    data object TaskSolvedCorrectly : BlockingEvent()
}
