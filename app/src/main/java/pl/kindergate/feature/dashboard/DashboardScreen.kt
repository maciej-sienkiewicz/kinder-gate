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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.kindergate.domain.model.ChildProfile
import pl.kindergate.domain.model.HealthLevel
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.ui.theme.HealthCritical
import pl.kindergate.ui.theme.HealthOk
import pl.kindergate.ui.theme.HealthWarning

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DashboardScreen(
    onEditApps: () -> Unit,
    onEditChildProfile: () -> Unit,
    onConfigureCategories: () -> Unit,
    viewModel: DashboardViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

            // Per-permission status card
            item {
                state.permissionStatus?.let { status ->
                    PermissionsCard(
                        status = status,
                        onGrantUsageStats = { context.startActivity(viewModel.getUsageStatsSettingsIntent()) },
                        onGrantOverlay = { context.startActivity(viewModel.getOverlaySettingsIntent()) },
                        onGrantNotification = { context.startActivity(viewModel.getNotificationSettingsIntent()) },
                        onGrantBattery = { context.startActivity(viewModel.getBatteryOptimizationIntent()) },
                        onGrantAccessibility = { context.startActivity(viewModel.getAccessibilitySettingsIntent()) }
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

            // Child profile card
            item {
                ChildProfileCard(
                    profile = state.childProfile,
                    onEdit = onEditChildProfile
                )
            }

            // Task categories card
            item {
                CategoryConfigCard(onConfigure = onConfigureCategories)
            }

            // Excluded apps section
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Wykluczone z monitorowania",
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
                                text = "Wszystkie aplikacje są monitorowane. Dodaj aplikacje, które chcesz wykluczyć z monitorowania.",
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
private fun PermissionsCard(
    status: PermissionStatus,
    onGrantUsageStats: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotification: () -> Unit,
    onGrantBattery: () -> Unit,
    onGrantAccessibility: () -> Unit
) {
    val headerColor = when (status.healthLevel) {
        HealthLevel.OK -> HealthOk
        HealthLevel.WARNING -> HealthWarning
        HealthLevel.CRITICAL -> HealthCritical
    }
    val headerIcon = when (status.healthLevel) {
        HealthLevel.OK -> Icons.Default.CheckCircle
        HealthLevel.WARNING -> Icons.Default.Warning
        HealthLevel.CRITICAL -> Icons.Default.Error
    }
    val headerText = when (status.healthLevel) {
        HealthLevel.OK -> "Ochrona w pełni aktywna"
        HealthLevel.WARNING -> "Brakuje niektórych uprawnień"
        HealthLevel.CRITICAL -> "Ochrona wyłączona!"
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Header row
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(headerIcon, null, tint = headerColor, modifier = Modifier.size(24.dp))
                Text(
                    text = headerText,
                    fontWeight = FontWeight.Bold,
                    color = headerColor,
                    style = MaterialTheme.typography.titleSmall
                )
            }

            Spacer(Modifier.height(12.dp))

            PermissionRow(
                title = "Dostęp do statystyk użycia",
                description = "Wymagane – wykrywa aktywną aplikację",
                isGranted = status.isUsageStatsGranted,
                isRequired = true,
                onGrant = onGrantUsageStats
            )
            PermissionRow(
                title = "Powiadomienia",
                description = "Wymagane dla usługi w tle",
                isGranted = status.isNotificationGranted,
                isRequired = true,
                onGrant = onGrantNotification
            )
            PermissionRow(
                title = "Wyświetlanie nad aplikacjami",
                description = "Zapasowy mechanizm blokady",
                isGranted = status.isOverlayGranted,
                isRequired = false,
                onGrant = onGrantOverlay
            )
            PermissionRow(
                title = "Wyłącz optymalizację baterii",
                description = "Zapewnia ciągłe działanie w tle",
                isGranted = status.isBatteryOptimizationExempt,
                isRequired = false,
                onGrant = onGrantBattery
            )
            PermissionRow(
                title = "Usługa dostępności",
                description = "Zapasowa detekcja (Xiaomi/Huawei)",
                isGranted = status.isAccessibilityEnabled,
                isRequired = false,
                onGrant = onGrantAccessibility
            )
        }
    }
}

@Composable
private fun PermissionRow(
    title: String,
    description: String,
    isGranted: Boolean,
    isRequired: Boolean,
    onGrant: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Lock,
            contentDescription = null,
            tint = if (isGranted) HealthOk else if (isRequired) HealthCritical else HealthWarning,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Row {
                Text(
                    text = title,
                    fontWeight = FontWeight.Medium,
                    style = MaterialTheme.typography.bodyMedium
                )
                if (isRequired) Text(
                    text = " *",
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                text = description,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (!isGranted) {
            OutlinedButton(
                onClick = onGrant,
                modifier = Modifier.height(32.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp)
            ) {
                Text("Udziel", style = MaterialTheme.typography.labelSmall)
            }
        }
    }
}

@Composable
private fun CategoryConfigCard(onConfigure: () -> Unit) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.School,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Typy zadań",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Włącz lub wyłącz przedmioty i typy ćwiczeń.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            OutlinedButton(onClick = onConfigure) { Text("Konfiguruj") }
        }
    }
}

@Composable
private fun ChildProfileCard(
    profile: ChildProfile?,
    onEdit: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Person,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Profil dziecka",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold
                )
                if (profile != null) {
                    val grade = profile.gradeLevel?.let { ", klasa $it" } ?: ""
                    Text(
                        text = "${profile.name}, ${profile.age} lat$grade",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                } else {
                    Text(
                        text = "Nie skonfigurowano. Kliknij, aby dodać.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            OutlinedButton(onClick = onEdit) {
                Text(if (profile != null) "Edytuj" else "Dodaj")
            }
        }
    }
}

private fun formatTime(ms: Long): String {
    val date = java.util.Date(ms)
    val format = java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault())
    return format.format(date)
}
