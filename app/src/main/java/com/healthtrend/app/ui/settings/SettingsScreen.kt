package com.healthtrend.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings screen — patient name, Google Sheet URL, share link, Google Sign-In.
 * Auto-save: all changes persist immediately (debounced for name, instant for URL).
 * NO save button, NO confirmation messages.
 *
 * @see SettingsViewModel for business logic and auto-save behavior.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        when (val state = uiState) {
            is SettingsUiState.Loading -> {
                // No loading spinner per UX rules
            }

            is SettingsUiState.Success -> {
                SettingsContent(
                    state = state,
                    onPatientNameChanged = viewModel::onPatientNameChanged,
                    onSheetUrlChanged = viewModel::onSheetUrlChanged,
                    onShareSheetLink = { shareSheetLink(context, state.sheetUrl) },
                    onSignIn = {
                        // Credential Manager requires Activity context
                        val activity = context as? Activity ?: return@SettingsContent
                        viewModel.onSignIn(activity)
                    },
                    onSignOut = viewModel::onSignOut,
                    onGlobalRemindersToggled = viewModel::onGlobalRemindersToggled,
                    onSlotReminderToggled = viewModel::onSlotReminderToggled,
                    onSlotTimeChanged = viewModel::onSlotTimeChanged,
                    modifier = Modifier.padding(innerPadding)
                )
            }
        }
    }
}

/**
 * Settings screen content — scrollable column with text fields, share button, auth, and reminders.
 */
@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    onPatientNameChanged: (String) -> Unit,
    onSheetUrlChanged: (String) -> Unit,
    onShareSheetLink: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onGlobalRemindersToggled: (Boolean) -> Unit,
    onSlotReminderToggled: (com.healthtrend.app.data.model.TimeSlot, Boolean) -> Unit,
    onSlotTimeChanged: (com.healthtrend.app.data.model.TimeSlot, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for text fields — synced from SettingsUiState but editable locally
    // to avoid cursor jumping from debounced saves.
    var patientNameLocal by rememberSaveable(state.patientName) {
        mutableStateOf(state.patientName)
    }
    var sheetUrlLocal by rememberSaveable(state.sheetUrl) {
        mutableStateOf(state.sheetUrl)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Patient Name Field ---
        OutlinedTextField(
            value = patientNameLocal,
            onValueChange = { newValue ->
                patientNameLocal = newValue
                onPatientNameChanged(newValue)
            },
            label = { Text("Patient Name") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.Person,
                    contentDescription = null // decorative, label provides context
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Next
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Patient name, ${patientNameLocal.ifEmpty { "empty" }}. Edit text."
                }
        )

        // --- Google Sheet URL Field ---
        val isUrlInvalid = !state.isSheetUrlValid

        OutlinedTextField(
            value = sheetUrlLocal,
            onValueChange = { newValue ->
                sheetUrlLocal = newValue
                onSheetUrlChanged(newValue)
            },
            label = { Text("Google Sheet URL") },
            leadingIcon = {
                Icon(
                    imageVector = Icons.Filled.TableChart,
                    contentDescription = null // decorative
                )
            },
            isError = isUrlInvalid,
            supportingText = if (isUrlInvalid) {
                { Text("Enter a valid Google Sheets URL") }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Uri,
                imeAction = ImeAction.Done
            ),
            colors = if (isUrlInvalid) {
                OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.error,
                    unfocusedBorderColor = MaterialTheme.colorScheme.error
                )
            } else {
                OutlinedTextFieldDefaults.colors()
            },
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = buildString {
                        append("Google Sheet URL, ${sheetUrlLocal.ifEmpty { "empty" }}. Edit text.")
                        if (isUrlInvalid) {
                            append(" URL is invalid.")
                        }
                    }
                }
        )

        // --- Share Sheet Link Button ---
        Button(
            onClick = onShareSheetLink,
            enabled = state.sheetUrl.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (state.sheetUrl.isNotEmpty()) {
                        "Share Sheet link. Double tap to share."
                    } else {
                        "Share Sheet link. Disabled. Enter a Sheet URL first."
                    }
                }
        ) {
            Icon(
                imageVector = Icons.Filled.Share,
                contentDescription = null, // button semantics covers this
                modifier = Modifier.padding(end = 8.dp)
            )
            Text("Share Sheet Link")
        }

        // --- Divider before Auth Section ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Google Account Auth Section ---
        AuthSection(
            authState = state.authState,
            onSignIn = onSignIn,
            onSignOut = onSignOut
        )

        // --- Divider before Reminders Section ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))

        // --- Reminder Configuration Section (Story 4.2) ---
        ReminderSettingsSection(
            globalRemindersEnabled = state.globalRemindersEnabled,
            slotReminders = state.slotReminders,
            onGlobalToggled = onGlobalRemindersToggled,
            onSlotToggled = onSlotReminderToggled,
            onSlotTimeChanged = onSlotTimeChanged
        )
    }
}

/**
 * Auth section — shows sign-in/sign-out state and actions.
 * Inline in Settings — NOT a blocking modal, NOT a popup.
 */
@Composable
private fun AuthSection(
    authState: AuthState,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(
            text = "Google Account",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface
        )

        when (authState) {
            is AuthState.SignedOut -> {
                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Sign in with Google. Double tap to sign in."
                        }
                ) {
                    Text("Sign in with Google")
                }
            }

            is AuthState.SignedIn -> {
                Text(
                    text = "Signed in as ${authState.email}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.semantics {
                        contentDescription = "Signed in as ${authState.email}"
                    }
                )

                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Sign out. Double tap to sign out."
                        }
                ) {
                    Text("Sign out")
                }
            }

            is AuthState.RefreshFailed -> {
                Text(
                    text = "Please sign in again",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.semantics {
                        contentDescription = "Authentication expired. Please sign in again."
                    }
                )

                Button(
                    onClick = onSignIn,
                    modifier = Modifier
                        .fillMaxWidth()
                        .semantics {
                            contentDescription = "Sign in with Google. Double tap to sign in."
                        }
                ) {
                    Text("Sign in with Google")
                }
            }

            is AuthState.Loading -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.padding(16.dp),
                        strokeWidth = 2.dp
                    )
                }
            }
        }
    }
}

/**
 * Launch Android share sheet with the Google Sheet URL.
 * AC #5 (Story 3.1): Intent.ACTION_SEND with Sheet URL as content.
 */
private fun shareSheetLink(context: Context, sheetUrl: String) {
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sheetUrl)
        type = "text/plain"
    }
    val shareIntent = Intent.createChooser(sendIntent, null)
    context.startActivity(shareIntent)
}
