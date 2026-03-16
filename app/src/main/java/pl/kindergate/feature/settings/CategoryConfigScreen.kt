package pl.kindergate.feature.settings

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Create
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.kindergate.domain.model.task.TaskSubject
import pl.kindergate.domain.model.task.TaskType

// ── Tile icon helpers ─────────────────────────────────────────────────────────

private fun TaskSubject.icon(): ImageVector = when (this) {
    TaskSubject.MATH    -> Icons.Default.Calculate
    TaskSubject.WRITING -> Icons.Default.Create
}

private fun TaskType.icon(): ImageVector = when (this) {
    TaskType.SIMPLE_ADDITION    -> Icons.Default.Add
    TaskType.SIMPLE_SUBTRACTION -> Icons.Default.Remove
    TaskType.MULTIPLICATION     -> Icons.Default.Star
    TaskType.DIVISION           -> Icons.Default.Functions
    TaskType.MIXED_OPERATIONS   -> Icons.Default.Calculate
    TaskType.LETTER_TRACING     -> Icons.Default.Create
}

// ── Screen ────────────────────────────────────────────────────────────────────

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryConfigScreen(
    onBack: () -> Unit,
    viewModel: CategoryConfigViewModel = hiltViewModel(),
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    val title = if (state.childName != null)
                        "Zadania – ${state.childName}"
                    else
                        "Konfiguracja zadań"
                    Text(title)
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        if (state.isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentAlignment = Alignment.Center,
            ) { CircularProgressIndicator() }
            return@Scaffold
        }

        if (state.childName == null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(32.dp),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "Najpierw utwórz profil dziecka, aby skonfigurować zadania.",
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            return@Scaffold
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp),
        ) {
            Spacer(Modifier.height(16.dp))

            // ── Subject tiles ─────────────────────────────────────────────────
            SectionHeader(title = "Przedmioty")
            Spacer(Modifier.height(8.dp))
            TileGrid(
                tiles = state.subjectTiles.map { tile ->
                    TileData(
                        label = tile.label,
                        icon = tile.subject.icon(),
                        isEnabled = tile.isEnabled,
                        onClick = { viewModel.onSubjectToggled(tile.subject) },
                    )
                }
            )

            Spacer(Modifier.height(24.dp))

            // ── Task type tiles ───────────────────────────────────────────────
            SectionHeader(title = "Typy zadań")
            Spacer(Modifier.height(8.dp))
            TileGrid(
                tiles = state.taskTypeTiles.map { tile ->
                    TileData(
                        label = tile.label,
                        icon = tile.taskType.icon(),
                        isEnabled = tile.isEnabled,
                        onClick = { viewModel.onTaskTypeToggled(tile.taskType) },
                    )
                }
            )

            Spacer(Modifier.height(24.dp))
        }
    }
}

// ── Section header ────────────────────────────────────────────────────────────

@Composable
private fun SectionHeader(title: String) {
    Text(
        text = title,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.Bold,
        modifier = Modifier.fillMaxWidth(),
    )
}

// ── Tile grid ─────────────────────────────────────────────────────────────────

private data class TileData(
    val label: String,
    val icon: ImageVector,
    val isEnabled: Boolean,
    val onClick: () -> Unit,
)

@Composable
private fun TileGrid(tiles: List<TileData>) {
    // LazyVerticalGrid inside a verticalScroll requires a fixed height; use a simple
    // wrapping approach with fixed-column grid of fixed item size instead.
    val columns = 3
    val rows = (tiles.size + columns - 1) / columns

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        for (row in 0 until rows) {
            androidx.compose.foundation.layout.Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth(),
            ) {
                for (col in 0 until columns) {
                    val index = row * columns + col
                    if (index < tiles.size) {
                        val tile = tiles[index]
                        CategoryTile(
                            label = tile.label,
                            icon = tile.icon,
                            isEnabled = tile.isEnabled,
                            onClick = tile.onClick,
                            modifier = Modifier.weight(1f),
                        )
                    } else {
                        // Empty placeholder to keep grid alignment
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

// ── Single tile ───────────────────────────────────────────────────────────────

@Composable
private fun CategoryTile(
    label: String,
    icon: ImageVector,
    isEnabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val enabledBackground = MaterialTheme.colorScheme.primaryContainer
    val disabledBackground = MaterialTheme.colorScheme.surfaceVariant

    val backgroundColor by animateColorAsState(
        targetValue = if (isEnabled) enabledBackground else disabledBackground,
        animationSpec = tween(durationMillis = 200),
        label = "tile_bg",
    )
    val contentAlpha = if (isEnabled) 1f else 0.45f
    val shape = RoundedCornerShape(16.dp)

    Column(
        modifier = modifier
            .clip(shape)
            .background(backgroundColor)
            .border(
                width = 1.5.dp,
                color = if (isEnabled)
                    MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                else
                    Color.Transparent,
                shape = shape,
            )
            .clickable(onClick = onClick)
            .padding(vertical = 16.dp, horizontal = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = if (isEnabled)
                MaterialTheme.colorScheme.primary
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .size(32.dp)
                .alpha(contentAlpha),
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            textAlign = TextAlign.Center,
            color = if (isEnabled)
                MaterialTheme.colorScheme.onPrimaryContainer
            else
                MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.alpha(contentAlpha),
        )
    }
}
