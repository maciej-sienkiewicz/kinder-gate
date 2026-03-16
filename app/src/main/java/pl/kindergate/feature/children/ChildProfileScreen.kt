package pl.kindergate.feature.children

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Full-screen editor for a single child profile.
 * Used as a standalone destination from the Dashboard.
 *
 * For the onboarding step variant see [ChildProfileFormContent] below –
 * it renders only the form body without a Scaffold, so it can be embedded
 * inside the onboarding pager.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChildProfileScreen(
    onSaved: () -> Unit,
    onBack: () -> Unit,
    viewModel: ChildProfileViewModel = hiltViewModel()
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) onSaved()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profil dziecka") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Wróć")
                    }
                }
            )
        }
    ) { paddingValues ->
        ChildProfileFormContent(
            state = state,
            onNameChange = viewModel::onNameChange,
            onAgeChange = viewModel::onAgeChange,
            onGradeLevelChange = viewModel::onGradeLevelChange,
            onSave = { viewModel.save() },
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 24.dp)
        )
    }
}

/**
 * The form body only – reusable inside the onboarding step.
 */
@Composable
fun ChildProfileFormContent(
    state: ChildProfileUiState,
    onNameChange: (String) -> Unit,
    onAgeChange: (String) -> Unit,
    onGradeLevelChange: (String) -> Unit,
    onSave: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Spacer(Modifier.height(8.dp))

        Text(
            text = "Dane dziecka",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Imię i wiek pomagają dopasować poziom zadań. Możesz zmienić te dane później w panelu rodzica.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        OutlinedTextField(
            value = state.name,
            onValueChange = onNameChange,
            label = { Text("Imię / pseudonim") },
            singleLine = true,
            isError = state.nameError != null,
            supportingText = state.nameError?.let { { Text(it) } },
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier.fillMaxWidth()
        )

        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            OutlinedTextField(
                value = state.ageInput,
                onValueChange = onAgeChange,
                label = { Text("Wiek (lata)") },
                singleLine = true,
                isError = state.ageError != null,
                supportingText = state.ageError?.let { { Text(it) } },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )

            OutlinedTextField(
                value = state.gradeLevelInput,
                onValueChange = onGradeLevelChange,
                label = { Text("Klasa (1–8, opcj.)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.weight(1f)
            )
        }

        Spacer(Modifier.weight(1f))

        Button(
            onClick = onSave,
            enabled = !state.isSaving,
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.CenterHorizontally)
        ) {
            if (state.isSaving) {
                CircularProgressIndicator(modifier = Modifier.height(18.dp))
            } else {
                Text("Zapisz profil →")
            }
        }

        Spacer(Modifier.height(16.dp))
    }
}
