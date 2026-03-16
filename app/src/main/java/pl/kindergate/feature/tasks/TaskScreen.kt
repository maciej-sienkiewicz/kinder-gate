package pl.kindergate.feature.tasks

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.kindergate.R
import pl.kindergate.domain.model.task.TaskContent
import pl.kindergate.domain.model.task.TaskType
import pl.kindergate.ui.theme.BlockingAccent
import pl.kindergate.ui.theme.BlockingText

/**
 * Renders the active task inside [BlockingScreen].
 *
 * Dispatches to a type-specific composable based on [TaskUiState.ShowingTask.task.taskType].
 * Adding a new task type only requires adding a branch in the `when` block and
 * implementing a private composable – no changes to [BlockingScreen] or the engine.
 */
@Composable
fun TaskScreen(
    state: TaskUiState.ShowingTask,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        when (state.task.taskType) {

            TaskType.SIMPLE_ADDITION -> {
                val c = state.task.content as TaskContent.SimpleAdditionContent
                NumericAnswerTask(
                    question    = "${c.operandA} + ${c.operandB} = ?",
                    answer      = state.currentAnswer,
                    feedback    = state.feedback,
                    isIncorrect = state.isIncorrect,
                    onAnswerChange = onAnswerChange,
                    onSubmit    = onSubmit,
                )
            }

            TaskType.SIMPLE_SUBTRACTION -> {
                val c = state.task.content as TaskContent.SubtractionContent
                NumericAnswerTask(
                    question    = "${c.minuend} − ${c.subtrahend} = ?",
                    answer      = state.currentAnswer,
                    feedback    = state.feedback,
                    isIncorrect = state.isIncorrect,
                    onAnswerChange = onAnswerChange,
                    onSubmit    = onSubmit,
                )
            }

            TaskType.MULTIPLICATION -> {
                val c = state.task.content as TaskContent.MultiplicationContent
                NumericAnswerTask(
                    question    = "${c.factorA} × ${c.factorB} = ?",
                    answer      = state.currentAnswer,
                    feedback    = state.feedback,
                    isIncorrect = state.isIncorrect,
                    onAnswerChange = onAnswerChange,
                    onSubmit    = onSubmit,
                )
            }

            TaskType.DIVISION -> {
                val c = state.task.content as TaskContent.DivisionContent
                NumericAnswerTask(
                    question    = "${c.dividend} ÷ ${c.divisor} = ?",
                    answer      = state.currentAnswer,
                    feedback    = state.feedback,
                    isIncorrect = state.isIncorrect,
                    onAnswerChange = onAnswerChange,
                    onSubmit    = onSubmit,
                )
            }

            TaskType.MIXED_OPERATIONS -> {
                val c = state.task.content as TaskContent.ExpressionContent
                NumericAnswerTask(
                    question    = "${c.expression} = ?",
                    answer      = state.currentAnswer,
                    feedback    = state.feedback,
                    isIncorrect = state.isIncorrect,
                    onAnswerChange = onAnswerChange,
                    onSubmit    = onSubmit,
                )
            }

            TaskType.LETTER_TRACING -> LetterTracingTask(
                content        = state.task.content as TaskContent.LetterTracingContent,
                onAnswerChange = onAnswerChange,
                onSubmit       = onSubmit,
            )
        }
    }
}

// ── Shared numeric-answer renderer ────────────────────────────────────────────

/**
 * Generic numeric-answer UI: shows [question], an integer input field, optional feedback,
 * and a submit button. Used by all binary-operation and expression task types.
 */
@Composable
private fun NumericAnswerTask(
    question: String,
    answer: String,
    feedback: String?,
    isIncorrect: Boolean,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    // Question
    Text(
        text = question,
        color = BlockingText,
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
        lineHeight = 48.sp,
        modifier = Modifier.fillMaxWidth(),
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Answer input – numbers only, max 4 digits (negative answers not needed in catalog)
    OutlinedTextField(
        value = answer,
        onValueChange = { input ->
            if (input.length <= 4 && input.all { it.isDigit() }) onAnswerChange(input)
        },
        singleLine = true,
        isError = isIncorrect,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done,
        ),
        keyboardActions = KeyboardActions(onDone = { onSubmit() }),
        textStyle = LocalTextStyle.current.copy(
            textAlign = TextAlign.Center,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = BlockingText,
        ),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = BlockingAccent,
            unfocusedBorderColor = BlockingText.copy(alpha = 0.4f),
            focusedTextColor = BlockingText,
            unfocusedTextColor = BlockingText,
            cursorColor = BlockingAccent,
            errorBorderColor = Color(0xFFE57373),
        ),
        modifier = Modifier.width(160.dp),
    )

    // Feedback from previous attempt
    if (feedback != null) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = feedback,
            color = if (isIncorrect) Color(0xFFE57373) else Color(0xFF81C784),
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
    }

    Spacer(modifier = Modifier.height(28.dp))

    // Submit
    Button(
        onClick = onSubmit,
        enabled = answer.isNotBlank(),
        modifier = Modifier
            .fillMaxWidth(0.75f)
            .height(56.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = BlockingAccent,
            contentColor = Color(0xFF1A1A1A),
            disabledContainerColor = BlockingAccent.copy(alpha = 0.4f),
            disabledContentColor = Color(0xFF1A1A1A).copy(alpha = 0.4f),
        ),
    ) {
        Text(
            text = stringResource(R.string.task_submit),
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
        )
    }
}
