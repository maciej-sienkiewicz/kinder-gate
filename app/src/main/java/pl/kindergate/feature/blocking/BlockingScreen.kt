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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import pl.kindergate.R
import pl.kindergate.ui.theme.BlockingAccent
import pl.kindergate.ui.theme.BlockingBackground
import pl.kindergate.ui.theme.BlockingSubtext
import pl.kindergate.ui.theme.BlockingText

/**
 * The PAUSE screen shown to the child.
 *
 * Design decisions:
 * - Deep navy background (#0D1B2A) – clearly different from any typical app UI,
 *   hard to confuse with something the child expects
 * - Warm amber accent (#F4A261) – eye-catching without being alarming/red
 * - Large, bold "PAUZA" text – immediately legible even to younger children
 * - Minimal animation – pulsing pause icon to draw attention, not distract
 * - Single clear CTA: "OK, rozumiem" – no ambiguity, no other options
 * - No "close" or "X" button – cannot be dismissed accidentally
 *
 * Accessibility:
 * - All text has contentDescription for screen readers
 * - Button has sufficient touch target (56dp height)
 * - Color contrast meets WCAG AA requirements
 */
@Composable
fun BlockingScreen(onAcknowledge: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconScale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(BlockingBackground)
            .semantics { contentDescription = "Ekran pauzy KinderGate" },
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 32.dp)
        ) {
            // Animated pause icon
            Box(
                modifier = Modifier
                    .size(96.dp)
                    .scale(scale)
                    .clip(CircleShape)
                    .background(BlockingAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Pause,
                    contentDescription = null,
                    tint = BlockingAccent,
                    modifier = Modifier.size(56.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Main heading
            Text(
                text = stringResource(R.string.blocking_pause),
                color = BlockingText,
                fontSize = 64.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center,
                letterSpacing = 4.sp,
                modifier = Modifier.semantics {
                    contentDescription = "Pauza"
                }
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Subtext
            Text(
                text = stringResource(R.string.blocking_message),
                color = BlockingSubtext,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Masz chwilę, żeby odpocząć od ekranu.",
                color = BlockingSubtext.copy(alpha = 0.7f),
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            // Acknowledgment button
            Button(
                onClick = onAcknowledge,
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlockingAccent,
                    contentColor = Color(0xFF1A1A1A)
                )
            ) {
                Text(
                    text = stringResource(R.string.blocking_ok),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
