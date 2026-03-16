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
import pl.kindergate.domain.usecase.GetNextTaskUseCase
import pl.kindergate.domain.usecase.SubmitTaskAnswerUseCase
import pl.kindergate.feature.tasks.TaskUiState
import javax.inject.Inject

/**
 * ViewModel for [BlockingActivity] / [BlockingScreen].
 *
 * Responsibilities:
 *  - Load the next task from [GetNextTaskUseCase] on creation.
 *  - Expose [uiState] for the UI to render.
 *  - Evaluate the answer via [SubmitTaskAnswerUseCase] on submit.
 *  - Emit [BlockingEvent.TaskSolvedCorrectly] so [BlockingScreen] calls [onAcknowledge].
 *
 * Error handling: if the engine throws unexpectedly we emit [TaskUiState.Error].
 * The UI falls back to the plain "OK" button so the child is never stuck on the
 * blocking screen due to a task engine failure.
 *
 * [CHILD_ID]: MVP uses a single hardcoded child. Multi-child support will pass the id
 * from navigation arguments once the parent Dashboard has a child-picker.
 */
@HiltViewModel
class BlockingViewModel @Inject constructor(
    private val getNextTaskUseCase: GetNextTaskUseCase,
    private val submitTaskAnswerUseCase: SubmitTaskAnswerUseCase,
) : ViewModel() {

    private val _uiState = MutableStateFlow<TaskUiState>(TaskUiState.Loading)
    val uiState: StateFlow<TaskUiState> = _uiState.asStateFlow()

    /** One-shot events consumed by [BlockingScreen] to trigger side-effects. */
    private val _events = MutableSharedFlow<BlockingEvent>(extraBufferCapacity = 1)
    val events: SharedFlow<BlockingEvent> = _events.asSharedFlow()

    init {
        loadNextTask()
    }

    fun loadNextTask() {
        _uiState.value = TaskUiState.Loading
        viewModelScope.launch {
            runCatching { getNextTaskUseCase(CHILD_ID) }
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
        viewModelScope.launch {
            runCatching {
                submitTaskAnswerUseCase(CHILD_ID, current.task.id, current.currentAnswer)
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
                // Unexpected engine failure – unblock the child rather than leaving them stuck.
                _events.tryEmit(BlockingEvent.TaskSolvedCorrectly)
            }
        }
    }

    companion object {
        /** Hardcoded in MVP; will be injected from parent's child-picker in v1. */
        internal const val CHILD_ID = "child_0"
    }
}

/** One-shot events emitted by [BlockingViewModel] and consumed once by the UI. */
sealed class BlockingEvent {
    /** Emitted when the child answers correctly – the screen should unblock. */
    data object TaskSolvedCorrectly : BlockingEvent()
}
