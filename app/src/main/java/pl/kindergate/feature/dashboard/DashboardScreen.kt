package pl.kindergate.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Apps
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.kindergate.domain.model.HealthLevel
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.ui.theme.HealthCritical
import pl.kindergate.ui.theme.HealthOk
import pl.kindergate.ui.theme.HealthWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEditApps: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Panel rodzica") },
                actions = {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 16.dp)
                    )
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item { Spacer(Modifier.height(8.dp)) }

            // System health card
            item {
                state.permissionStatus?.let { status ->
                    HealthCard(
                        status = status,
                        onTap = { /* open permission settings */ }
                    )
                }
            }

            // Tamper alert
            item {
                if (state.unacknowledgedTamperCount > 0) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Icon(Icons.Default.Warning, null, tint = MaterialTheme.colorScheme.error)
                            Text(
                                text = "${state.unacknowledgedTamperCount} nieodczytane alerty bezpieczeństwa",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Monitored apps section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Monitorowane aplikacje",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            FilledTonalButton(onClick = onEditApps) {
                                Icon(Icons.Default.Apps, null, modifier = Modifier.size(16.dp))
                                Text(" Zarządzaj")
                            }
                        }
                        Spacer(Modifier.height(8.dp))
                        if (state.monitoredApps.isEmpty()) {
                            Text(
                                text = "Brak monitorowanych aplikacji. Dodaj aplikacje, które chcesz ograniczyć.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.monitoredApps.take(5).forEach { app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(if (app.isEnabled) HealthOk else HealthWarning)
                                    )
                                    Text(
                                        text = app.appLabel,
                                        style = MaterialTheme.typography.bodyMedium
                                    )
                                }
                            }
                            if (state.monitoredApps.size > 5) {
                                Text(
                                    text = "…i ${state.monitoredApps.size - 5} więcej",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }

            // Today's sessions
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Dzisiejsze pauzy: ${state.todaySessions.size}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(Modifier.height(8.dp))
                        if (state.todaySessions.isEmpty()) {
                            Text(
                                text = "Brak pauz dzisiaj.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            state.todaySessions.take(3).forEach { session ->
                                Text(
                                    text = "• ${session.packageName} – ${formatTime(session.wallClockTriggeredMs)}",
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
                    }
                }
            }

            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

@Composable
private fun HealthCard(status: PermissionStatus, onTap: () -> Unit) {
    val (color, icon, text) = when (status.healthLevel) {
        HealthLevel.OK -> Triple(HealthOk, Icons.Default.CheckCircle, "Ochrona w pełni aktywna")
        HealthLevel.WARNING -> Triple(HealthWarning, Icons.Default.Warning, "Brakuje niektórych uprawnień")
        HealthLevel.CRITICAL -> Triple(HealthCritical, Icons.Default.Error, "Ochrona wyłączona!")
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onTap),
        colors = CardDefaults.cardColors(containerColor = color.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, null, tint = color, modifier = Modifier.size(32.dp))
            Column {
                Text(text = text, fontWeight = FontWeight.Bold, color = color)
                if (!status.isUsageStatsGranted) {
                    Text("• Brak dostępu do statystyk użycia", style = MaterialTheme.typography.bodySmall)
                }
                if (!status.isNotificationGranted) {
                    Text("• Brak uprawnień do powiadomień", style = MaterialTheme.typography.bodySmall)
                }
                if (!status.isBatteryOptimizationExempt) {
                    Text("• Optymalizacja baterii może ograniczać działanie", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val date = java.util.Date(ms)
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
