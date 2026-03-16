package pl.kindergate.feature.tasks

import pl.kindergate.domain.model.task.Task

/**
 * UI state for the task widget rendered inside [BlockingScreen].
 *
 * Sealed class keeps the rendering logic in the UI layer; the ViewModel emits
 * one of these variants and the Composable switches on it.
 */
sealed class TaskUiState {

    /** Initial state while the engine picks the next task. */
    data object Loading : TaskUiState()

    /**
     * A task is ready to be solved.
     *
     * @param task          The task the child is answering.
     * @param currentAnswer Live text field value (bound two-way to the input).
     * @param feedback      Feedback from the last incorrect attempt; null if no attempt yet.
     * @param isIncorrect   True after a wrong submission – used to colour the input red.
     */
    data class ShowingTask(
        val task: Task,
        val currentAnswer: String = "",
        val feedback: String? = null,
        val isIncorrect: Boolean = false,
    ) : TaskUiState()

    /**
     * The engine or repository threw an unexpected error.
     * The UI falls back to the simple acknowledgement button so the child is never stuck.
     */
    data class Error(val message: String? = null) : TaskUiState()
}
