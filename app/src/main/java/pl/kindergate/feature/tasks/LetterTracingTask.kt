package pl.kindergate.feature.tasks

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pl.kindergate.domain.model.task.TaskContent
import kotlin.math.roundToInt

// ── Palette ───────────────────────────────────────────────────────────────────

private val StrokeColors = listOf(
    Color(0xFFFF5252), // red
    Color(0xFFFF9800), // orange
    Color(0xFFFFEB3B), // yellow
    Color(0xFF4CAF50), // green
    Color(0xFF2196F3), // blue
    Color(0xFF9C27B0), // purple
    Color(0xFFE91E63), // pink
    Color(0xFF00BCD4), // cyan
)

private val TemplateDash = Color(0xFFBBDEFB)     // soft blue dashes
private val TemplateDot  = Color(0xFF90CAF9)     // slightly darker dots

private val CoverageColors = listOf(
    Color(0xFFFF5252) to Color(0xFFFF9800),  // 0–30 %  red→orange
    Color(0xFFFF9800) to Color(0xFFFFEB3B),  // 30–60 % orange→yellow
    Color(0xFF4CAF50) to Color(0xFF00BCD4),  // 60–90 % green→cyan
    Color(0xFF00E5FF) to Color(0xFF69F0AE),  // ≥ 90 %  cyan→mint
)

private fun coverageGradient(pct: Float): Pair<Color, Color> = when {
    pct < 30f -> CoverageColors[0]
    pct < 60f -> CoverageColors[1]
    pct < 90f -> CoverageColors[2]
    else      -> CoverageColors[3]
}

// ── Data model ────────────────────────────────────────────────────────────────

private data class DrawnStroke(val points: List<Offset>, val color: Color)

// ── Main composable ───────────────────────────────────────────────────────────

/**
 * Renders a letter-tracing task inside [TaskScreen].
 *
 * The child sees the letter drawn as dashed lines and traces it with their finger.
 * When coverage of the template ≥ 90 % the task is automatically submitted as correct.
 *
 * Coverage is computed by checking what fraction of the letter's guide-point set
 * lies within [TOUCH_RADIUS_NORM] of any drawn touch point (both in normalised coords).
 */
@Composable
fun LetterTracingTask(
    content: TaskContent.LetterTracingContent,
    onAnswerChange: (String) -> Unit,
    onSubmit: () -> Unit,
) {
    val letter = content.letter
    // Guide points in normalised [0,1] space – recomputed only when the letter changes.
    val guidePoints = remember(letter) { LetterPaths.samplePoints(letter) }

    // All completed strokes (finger-up) + the stroke being currently drawn.
    val completedStrokes = remember(letter) { mutableStateListOf<DrawnStroke>() }
    val activePoints     = remember(letter) { mutableStateListOf<Offset>() }
    var strokeColorIndex by remember(letter) { mutableStateOf(0) }

    var coverage by remember(letter) { mutableFloatStateOf(0f) }

    // Canvas size captured on first draw so we can normalise touch coords.
    var canvasWidthPx  by remember { mutableFloatStateOf(1f) }
    var canvasHeightPx by remember { mutableFloatStateOf(1f) }

    // A shimmering pulse on the letter template to invite the child to trace.
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val templateAlpha by infiniteTransition.animateFloat(
        initialValue = 0.55f,
        targetValue  = 1.00f,
        animationSpec = infiniteRepeatable(
            animation  = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "templateAlpha",
    )

    // Celebration bounce when task is solved.
    val celebrationScale = remember { Animatable(1f) }
    var solved by remember(letter) { mutableStateOf(false) }

    LaunchedEffect(solved) {
        if (solved) {
            repeat(3) {
                celebrationScale.animateTo(1.15f, tween(120))
                celebrationScale.animateTo(1.00f, tween(120))
            }
            onSubmit()
        }
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(0.dp),
        modifier = Modifier.fillMaxWidth(),
    ) {
        // ── Instruction ──────────────────────────────────────────────────────
        Text(
            text = "Narysuj literę palcem!",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFE3F2FD),
            textAlign = TextAlign.Center,
        )

        Spacer(modifier = Modifier.height(8.dp))

        // ── Drawing canvas ───────────────────────────────────────────────────
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(280.dp)
                .background(
                    color  = Color(0xFF1A237E).copy(alpha = 0.6f),
                    shape  = RoundedCornerShape(24.dp),
                )
                .padding(8.dp),
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(letter) {
                        detectDragGestures(
                            onDragStart = { offset ->
                                activePoints.clear()
                                activePoints.add(offset)
                            },
                            onDrag = { change, _ ->
                                activePoints.add(change.position)
                                // Recompute coverage on every touch event.
                                val allPoints = buildList {
                                    completedStrokes.forEach { addAll(it.points) }
                                    addAll(activePoints)
                                }
                                val newCoverage = computeCoverage(
                                    guidePoints = guidePoints,
                                    drawnPoints = allPoints,
                                    canvasW     = canvasWidthPx,
                                    canvasH     = canvasHeightPx,
                                )
                                coverage = newCoverage
                                onAnswerChange(newCoverage.toString())
                                if (newCoverage >= 90f && !solved) solved = true
                            },
                            onDragEnd = {
                                if (activePoints.isNotEmpty()) {
                                    completedStrokes.add(
                                        DrawnStroke(
                                            points = activePoints.toList(),
                                            color  = StrokeColors[strokeColorIndex % StrokeColors.size],
                                        ),
                                    )
                                    strokeColorIndex++
                                    activePoints.clear()
                                }
                            },
                        )
                    },
            ) {
                canvasWidthPx  = size.width
                canvasHeightPx = size.height

                // Draw template letter as dashed path.
                drawLetterTemplate(
                    letter  = letter,
                    canvasW = size.width,
                    canvasH = size.height,
                    alpha   = templateAlpha,
                )

                // Draw completed strokes.
                for (stroke in completedStrokes) {
                    drawStroke(stroke.points, stroke.color)
                }

                // Draw active (in-progress) stroke.
                if (activePoints.size >= 2) {
                    drawStroke(
                        points = activePoints,
                        color  = StrokeColors[strokeColorIndex % StrokeColors.size],
                    )
                }

                // Sparkle glow at fingertip.
                if (activePoints.isNotEmpty()) {
                    drawSparkle(activePoints.last(), StrokeColors[strokeColorIndex % StrokeColors.size])
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // ── Coverage meter ───────────────────────────────────────────────────
        CoverageMeter(coverage = coverage)

        Spacer(modifier = Modifier.height(8.dp))
    }
}

// ── Coverage calculation ──────────────────────────────────────────────────────

/** Radius (in normalised [0,1] units) within which a drawn point "covers" a guide point. */
private const val TOUCH_RADIUS_NORM = 0.07f

private fun computeCoverage(
    guidePoints : List<Pair<Float, Float>>,
    drawnPoints : List<Offset>,
    canvasW     : Float,
    canvasH     : Float,
): Float {
    if (guidePoints.isEmpty() || drawnPoints.isEmpty()) return 0f
    val radiusSq = TOUCH_RADIUS_NORM * TOUCH_RADIUS_NORM
    var covered = 0
    for ((gx, gy) in guidePoints) {
        for (dp in drawnPoints) {
            val nx = dp.x / canvasW
            val ny = dp.y / canvasH
            val dx = nx - gx
            val dy = ny - gy
            if (dx * dx + dy * dy <= radiusSq) {
                covered++
                break
            }
        }
    }
    return (covered.toFloat() / guidePoints.size * 100f).coerceIn(0f, 100f)
}

// ── Drawing helpers ───────────────────────────────────────────────────────────

private fun DrawScope.drawLetterTemplate(
    letter  : Char,
    canvasW : Float,
    canvasH : Float,
    alpha   : Float,
) {
    val strokes = LetterPaths.strokesFor(letter)
    val dashEffect = PathEffect.dashPathEffect(floatArrayOf(18f, 12f), 0f)
    val strokeStyle = Stroke(
        width      = 18f,
        cap        = StrokeCap.Round,
        join       = StrokeJoin.Round,
        pathEffect = dashEffect,
    )
    for (stroke in strokes) {
        if (stroke.size < 2) continue
        val path = Path()
        val (x0, y0) = stroke[0]
        path.moveTo(x0 * canvasW, y0 * canvasH)
        for (i in 1 until stroke.size) {
            val (xi, yi) = stroke[i]
            path.lineTo(xi * canvasW, yi * canvasH)
        }
        drawPath(
            path  = path,
            color = TemplateDash.copy(alpha = alpha),
            style = strokeStyle,
        )
        // Dot at each control point for extra visual clarity.
        for ((px, py) in stroke) {
            drawCircle(
                color  = TemplateDot.copy(alpha = alpha * 0.8f),
                radius = 5f,
                center = Offset(px * canvasW, py * canvasH),
            )
        }
    }
}

private fun DrawScope.drawStroke(points: List<Offset>, color: Color) {
    if (points.size < 2) return
    val path = Path()
    path.moveTo(points[0].x, points[0].y)
    for (i in 1 until points.size) {
        path.lineTo(points[i].x, points[i].y)
    }
    drawPath(
        path  = path,
        brush = Brush.linearGradient(
            colors = listOf(color, color.copy(alpha = 0.7f)),
            start  = points.first(),
            end    = points.last(),
        ),
        style = Stroke(width = 22f, cap = StrokeCap.Round, join = StrokeJoin.Round),
    )
}

private fun DrawScope.drawSparkle(center: Offset, color: Color) {
    // Glowing halo around the fingertip.
    drawCircle(color = color.copy(alpha = 0.25f), radius = 28f, center = center)
    drawCircle(color = color.copy(alpha = 0.55f), radius = 14f, center = center)
    drawCircle(color = Color.White.copy(alpha = 0.9f), radius = 5f, center = center)
}

// ── Coverage meter ────────────────────────────────────────────────────────────

@Composable
private fun CoverageMeter(coverage: Float) {
    val (startColor, endColor) = coverageGradient(coverage)
    val label = when {
        coverage < 30f -> "Zacznij rysować! ✏️"
        coverage < 60f -> "Tak trzymaj! 👍"
        coverage < 90f -> "Prawie gotowe! ⭐"
        else           -> "Brawo! Świetnie! 🎉"
    }
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .fillMaxWidth(0.85f)
            .padding(horizontal = 4.dp),
    ) {
        Text(
            text       = label,
            fontSize   = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color      = endColor,
            textAlign  = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(6.dp))
        // Track
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
                .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(8.dp)),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(fraction = (coverage / 100f).coerceIn(0f, 1f))
                    .height(16.dp)
                    .background(
                        Brush.horizontalGradient(listOf(startColor, endColor)),
                        RoundedCornerShape(8.dp),
                    ),
            )
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text      = "${coverage.roundToInt()}%",
            fontSize  = 13.sp,
            color     = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
        )
    }
}
