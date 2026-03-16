package pl.kindergate.feature.blocking

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.kindergate.R
import pl.kindergate.feature.tasks.TaskScreen
import pl.kindergate.feature.tasks.TaskUiState
import pl.kindergate.ui.theme.BlockingAccent
import pl.kindergate.ui.theme.BlockingBackground
import pl.kindergate.ui.theme.BlockingSubtext
import pl.kindergate.ui.theme.BlockingText

/**
 * Fullscreen PAUSE screen shown to the child.
 *
 * ## Task integration
 * Instead of a plain "OK, rozumiem" button the screen now hosts a short educational
 * task provided by [BlockingViewModel]. The child must answer correctly to dismiss
 * the screen. On a correct answer [BlockingEvent.TaskSolvedCorrectly] is emitted and
 * [onAcknowledge] is called – exactly the same path as before.
 *
 * ## Failure safety
 * If the task engine throws ([TaskUiState.Error]) we fall back to the original plain
 * button so the child is never permanently stuck on the blocking screen.
 *
 * ## No changes to MonitorService / SessionTimer
 * The task widget lives entirely in [BlockingViewModel] + [TaskScreen].
 * [onAcknowledge] still calls [BlockingActivity.onChildAcknowledged] which sends
 * ACTION_BLOCK_ACKNOWLEDGED to MonitorService – the monitoring layer is untouched.
 */
@Composable
fun BlockingScreen(
    onAcknowledge: () -> Unit,
    viewModel: BlockingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // One-shot event: correct answer → unblock
    LaunchedEffect(Unit) {
        viewModel.events.collect { event ->
            when (event) {
                is BlockingEvent.TaskSolvedCorrectly -> onAcknowledge()
            }
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "iconScale",
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlockingBackground)
            .semantics { contentDescription = "Ekran pauzy KinderGate" },
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 32.dp, vertical = 24.dp),
        ) {
            // ── Animated pause icon ───────────────────────────────────────────
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(BlockingAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = BlockingAccent,
                    modifier = Modifier.size(56.dp),
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // ── Heading ───────────────────────────────────────────────────────
            Text(
                text = stringResource(R.string.blocking_pause),
                color = BlockingText,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
                modifier = Modifier.semantics { contentDescription = "Pauza" },
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = stringResource(R.string.blocking_message),
                color = BlockingSubtext,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = stringResource(R.string.task_solve_to_unlock),
                color = BlockingSubtext.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(32.dp))

            // ── Task widget ───────────────────────────────────────────────────
            when (val state = uiState) {
                is TaskUiState.Loading -> {
                    CircularProgressIndicator(
                        color = BlockingAccent,
                        modifier = Modifier.size(48.dp),
                    )
                }

                is TaskUiState.ShowingTask -> {
                    TaskScreen(
                        state = state,
                        onAnswerChange = viewModel::updateAnswer,
                        onSubmit = viewModel::submitAnswer,
                    )
                }

                is TaskUiState.Error -> {
                    // Fallback: plain acknowledgement button so the child isn't stuck
                    Text(
                        text = stringResource(R.string.task_error_load),
                        color = BlockingSubtext.copy(alpha = 0.6f),
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onAcknowledge,
                        modifier = Modifier
                            .fillMaxWidth(0.75f)
                            .height(56.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BlockingAccent,
                            contentColor = Color(0xFF1A1A1A),
                        ),
                    ) {
                        Text(
                            text = stringResource(R.string.blocking_ok),
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
            }
        }
    }
}
