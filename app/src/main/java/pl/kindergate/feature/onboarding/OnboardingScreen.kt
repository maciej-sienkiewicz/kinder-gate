package pl.kindergate.feature.onboarding

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import pl.kindergate.domain.model.PermissionStatus
import pl.kindergate.feature.children.ChildProfileFormContent
import pl.kindergate.feature.children.ChildProfileUiState

@Composable
fun OnboardingScreen(
    onComplete: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Notification permission launcher (Android 13+)
    val notifLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    // Refresh permissions when user returns from settings
    LaunchedEffect(state.currentStep) {
        viewModel.refreshPermissions()
    }

    Scaffold { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Progress bar
            LinearProgressIndicator(
                progress = { (state.currentStep + 1f) / state.totalSteps },
                modifier = Modifier.fillMaxWidth()
            )

            AnimatedContent(
                targetState = state.currentStep,
                transitionSpec = {
                    slideInHorizontally { it } togetherWith slideOutHorizontally { -it }
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    0 -> WelcomeStep(onNext = viewModel::nextStep)
                    1 -> PermissionsStep(
                        permissions = state.permissions,
                        onGrantUsageStats = { context.startActivity(viewModel.getUsageStatsSettingsIntent()) },
                        onGrantOverlay = { context.startActivity(viewModel.getOverlaySettingsIntent()) },
                        onGrantNotification = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        },
                        onGrantBattery = { context.startActivity(viewModel.getBatteryOptimizationIntent()) },
                        onGrantAccessibility = { context.startActivity(viewModel.getAccessibilitySettingsIntent()) },
                        onNext = viewModel::nextStep
                    )
                    2 -> PinSetupStep(
                        pin = state.pinInput,
                        pinConfirm = state.pinConfirmInput,
                        error = state.pinError,
                        onPinChange = viewModel::onPinInput,
                        onPinConfirmChange = viewModel::onPinConfirmInput,
                        onNext = {
                            if (viewModel.savePin()) viewModel.nextStep()
                        }
                    )
                    3 -> ChildProfileStep(
                        name = state.childName,
                        ageInput = state.childAgeInput,
                        gradeLevelInput = state.childGradeLevelInput,
                        nameError = state.childNameError,
                        ageError = state.childAgeError,
                        onNameChange = viewModel::onChildNameInput,
                        onAgeChange = viewModel::onChildAgeInput,
                        onGradeLevelChange = viewModel::onChildGradeLevelInput,
                        onNext = {
                            if (viewModel.saveChildProfile()) viewModel.nextStep()
                        }
                    )
                    4 -> AppPickerPlaceholderStep(
                        onNext = viewModel::nextStep
                    )
                    5 -> CompletionStep(
                        onFinish = {
                            viewModel.completeOnboarding()
                            onComplete()
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun WelcomeStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Security,
            contentDescription = null,
            modifier = Modifier.size(80.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Witaj w KinderGate",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Transparentne narzędzie do kontroli czasu ekranowego. Twoje dziecko będzie wiedziało, że KinderGate jest aktywny.",
            style = MaterialTheme.typography.bodyLarge,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(16.dp))
        Card(
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Text(
                text = "⚠️ Ważne: KinderGate wymaga kilku uprawnień systemowych. Bez nich nie może działać niezawodnie. W następnym kroku przeprowadzimy Cię przez konfigurację.",
                modifier = Modifier.padding(16.dp),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Zaczynajmy →")
        }
    }
}

@Composable
private fun PermissionsStep(
    permissions: PermissionStatus?,
    onGrantUsageStats: () -> Unit,
    onGrantOverlay: () -> Unit,
    onGrantNotification: () -> Unit,
    onGrantBattery: () -> Unit,
    onGrantAccessibility: () -> Unit,
    onNext: () -> Unit
) {
    if (permissions == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Uprawnienia systemowe",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Wymagane do poprawnego działania monitoringu:",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        PermissionRow(
            title = "Dostęp do statystyk użycia",
            description = "Niezbędne – wykrywa aktywną aplikację",
            isGranted = permissions.isUsageStatsGranted,
            isRequired = true,
            onGrant = onGrantUsageStats
        )

        PermissionRow(
            title = "Powiadomienia",
            description = "Wymagane dla usługi działającej w tle",
            isGranted = permissions.isNotificationGranted,
            isRequired = true,
            onGrant = onGrantNotification
        )

        PermissionRow(
            title = "Wyświetlanie nad aplikacjami",
            description = "Zapasowy mechanizm blokady",
            isGranted = permissions.isOverlayGranted,
            isRequired = false,
            onGrant = onGrantOverlay
        )

        PermissionRow(
            title = "Wyłącz optymalizację baterii",
            description = "Zapewnia ciągłe działanie w tle",
            isGranted = permissions.isBatteryOptimizationExempt,
            isRequired = false,
            onGrant = onGrantBattery
        )

        PermissionRow(
            title = "Usługa dostępności",
            description = "Zapasowa detekcja dla Xiaomi/Huawei",
            isGranted = permissions.isAccessibilityEnabled,
            isRequired = false,
            onGrant = onGrantAccessibility
        )

        Spacer(Modifier.weight(1f))

        if (!permissions.isUsageStatsGranted || !permissions.isNotificationGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer
                )
            ) {
                Text(
                    text = "Wymagane uprawnienia nie zostały udzielone. KinderGate nie będzie działać bez nich.",
                    modifier = Modifier.padding(12.dp),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onErrorContainer
                )
            }
        }

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = permissions.isUsageStatsGranted
        ) {
            Text("Dalej →")
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
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isGranted)
                MaterialTheme.colorScheme.secondaryContainer
            else
                MaterialTheme.colorScheme.surface
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = if (isGranted) Icons.Default.Check else Icons.Default.Lock,
                contentDescription = null,
                tint = if (isGranted) MaterialTheme.colorScheme.secondary
                       else MaterialTheme.colorScheme.onSurfaceVariant
            )
            Column(modifier = Modifier.weight(1f)) {
                Row {
                    Text(text = title, fontWeight = FontWeight.Medium, style = MaterialTheme.typography.bodyMedium)
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
                OutlinedButton(onClick = onGrant) {
                    Text("Udziel")
                }
            }
        }
    }
}

@Composable
private fun PinSetupStep(
    pin: String,
    pinConfirm: String,
    error: String?,
    onPinChange: (String) -> Unit,
    onPinConfirmChange: (String) -> Unit,
    onNext: () -> Unit
) {
    var showPin by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "PIN rodzica",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "PIN chroni ustawienia KinderGate. Zapamiętaj go – bez niego nie możesz zmienić konfiguracji.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = pin,
            onValueChange = onPinChange,
            label = { Text("PIN (4-8 cyfr)") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = if (showPin) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { showPin = !showPin }) {
                    Icon(
                        if (showPin) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                        contentDescription = null
                    )
                }
            },
            modifier = Modifier.fillMaxWidth(),
            isError = error != null
        )

        OutlinedTextField(
            value = pinConfirm,
            onValueChange = onPinConfirmChange,
            label = { Text("Potwierdź PIN") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            visualTransformation = PasswordVisualTransformation(),
            modifier = Modifier.fillMaxWidth(),
            isError = error != null
        )

        if (error != null) {
            Text(text = error, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onNext,
            modifier = Modifier.fillMaxWidth(),
            enabled = pin.length >= 4 && pinConfirm.isNotEmpty()
        ) {
            Text("Ustaw PIN i kontynuuj →")
        }
    }
}

@Composable
private fun ChildProfileStep(
    name: String,
    ageInput: String,
    gradeLevelInput: String,
    nameError: String?,
    ageError: String?,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGradeLevelChange: (String) -> Unit,
    onNext: () -> Unit,
) {
    ChildProfileFormContent(
        state = ChildProfileUiState(
            name = name,
            ageInput = ageInput,
            gradeLevelInput = gradeLevelInput,
            nameError = nameError,
            ageError = ageError,
        ),
        onNameChange = onNameChange,
        onAgeChange = onAgeChange,
        onGradeLevelChange = onGradeLevelChange,
        onSave = onNext,
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
    )
}

@Composable
private fun AppPickerPlaceholderStep(onNext: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Wybierz monitorowane aplikacje",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "Po kliknięciu 'Dalej' przejdziesz do wyboru aplikacji. Możesz to też zrobić później z panelu rodzica.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onNext, modifier = Modifier.fillMaxWidth()) {
            Text("Wybiorę później w panelu →")
        }
    }
}

@Composable
private fun CompletionStep(onFinish: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.Check,
            contentDescription = null,
            modifier = Modifier
                .size(80.dp)
                .background(MaterialTheme.colorScheme.secondaryContainer, shape = androidx.compose.foundation.shape.CircleShape)
                .padding(16.dp),
            tint = MaterialTheme.colorScheme.secondary
        )
        Spacer(Modifier.height(24.dp))
        Text(
            text = "Konfiguracja zakończona!",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = "KinderGate zaczyna działać. Nie zapomnij wybrać monitorowanych aplikacji w panelu rodzica.",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(32.dp))
        Button(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
            Text("Przejdź do panelu →")
        }
    }
}
