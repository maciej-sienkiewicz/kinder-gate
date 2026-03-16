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
            TaskType.SIMPLE_ADDITION -> SimpleAdditionTask(
                content = state.task.content as TaskContent.SimpleAdditionContent,
                answer = state.currentAnswer,
                feedback = state.feedback,
                isIncorrect = state.isIncorrect,
                onAnswerChange = onAnswerChange,
                onSubmit = onSubmit,
            )
        }
    }
}

// ── Task type renderers ───────────────────────────────────────────────────────

@Composable
private fun SimpleAdditionTask(
    content: TaskContent.SimpleAdditionContent,
    answer: String,
    feedback: String?,
    isIncorrect: Boolean,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    // Question
    Text(
        text = "${content.operandA} + ${content.operandB} = ?",
        color = BlockingText,
        fontSize = 44.sp,
        fontWeight = FontWeight.ExtraBold,
        textAlign = TextAlign.Center,
    )

    Spacer(modifier = Modifier.height(24.dp))

    // Answer input – numbers only, max 4 digits
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
