package com.healthtrend.app.ui.settings

import android.app.Activity
import android.content.Context
import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

/**
 * Settings screen — patient name, Google Sheet status, share link, Google Sign-In.
 * Auto-save: all changes persist immediately (debounced for name).
 * Sheet URL is auto-created on sign-in — not manually editable.
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

    // Launcher for Google OAuth scope consent screen (UserRecoverableAuthIOException recovery)
    val authRecoveryLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        viewModel.onSheetAuthRecoveryResult(result.resultCode == Activity.RESULT_OK)
    }

    // Collect one-shot auth recovery events and launch consent screen
    LaunchedEffect(Unit) {
        viewModel.authRecoveryEvent.collect { intent ->
            authRecoveryLauncher.launch(intent)
        }
    }

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
 * Settings screen content — scrollable column with patient name, sheet status, auth, and reminders.
 */
@Composable
private fun SettingsContent(
    state: SettingsUiState.Success,
    onPatientNameChanged: (String) -> Unit,
    onShareSheetLink: () -> Unit,
    onSignIn: () -> Unit,
    onSignOut: () -> Unit,
    onGlobalRemindersToggled: (Boolean) -> Unit,
    onSlotReminderToggled: (com.healthtrend.app.data.model.TimeSlot, Boolean) -> Unit,
    onSlotTimeChanged: (com.healthtrend.app.data.model.TimeSlot, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    // Local state for patient name — synced from SettingsUiState but editable locally
    // to avoid cursor jumping from debounced saves.
    var patientNameLocal by rememberSaveable(state.patientName) {
        mutableStateOf(state.patientName)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // --- Profile Section Header ---
        Text(
            text = "Profile",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

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
                imeAction = ImeAction.Done
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = "Patient name, ${patientNameLocal.ifEmpty { "empty" }}. Edit text."
                }
        )

        // --- Divider before Sync Section ---
        HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))

        // --- Sync Section Header ---
        Text(
            text = "Sync",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        // --- Google Sheet Status (read-only, auto-created on sign-in) ---
        SheetStatusSection(
            sheetUrl = state.sheetUrl,
            sheetCreationInProgress = state.sheetCreationInProgress,
            sheetCreationError = state.sheetCreationError,
            onShareSheetLink = onShareSheetLink
        )

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
 * Read-only Google Sheet status display (Story 3.4).
 * Shows Sheet URL when auto-created, "Creating sheet..." during creation,
 * or "Sheet will be created on sign-in" if not yet started.
 * Share button enabled when a Sheet URL exists.
 */
@Composable
private fun SheetStatusSection(
    sheetUrl: String,
    sheetCreationInProgress: Boolean,
    sheetCreationError: String?,
    onShareSheetLink: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // --- Sheet URL, creating state, or placeholder ---
        if (sheetUrl.isNotEmpty()) {
            Text(
                text = "Google Sheet",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = sheetUrl,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                modifier = Modifier.semantics {
                    contentDescription = "Google Sheet URL. $sheetUrl. Read only."
                }
            )
        } else if (sheetCreationInProgress) {
            Text(
                text = "Creating sheet\u2026",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Google Sheet, creating now."
                }
            )
        } else {
            Text(
                text = "Sheet will be created on sign-in",
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.semantics {
                    contentDescription = "Google Sheet, not created yet."
                }
            )
        }

        // --- Diagnostic: show sheet creation error (TODO: remove after debugging) ---
        if (sheetCreationError != null) {
            Text(
                text = "DEBUG: $sheetCreationError",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
                maxLines = 4
            )
        }

        // --- Share Sheet Link Button ---
        Button(
            onClick = onShareSheetLink,
            enabled = sheetUrl.isNotEmpty(),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .semantics {
                    contentDescription = if (sheetUrl.isNotEmpty()) {
                        "Share Sheet link. Double tap to share."
                    } else {
                        "Share Sheet link. Disabled. Sign in to create a sheet first."
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
                Text(
                    text = "Not signed in",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Not signed in"
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
                // No spinner per UX rules (only PDF generation may show a spinner).
                Text(
                    text = "Signing in…",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.semantics {
                        contentDescription = "Signing in. Please wait."
                    }
                )
            }
        }
    }
}

/**
 * Launch Android share sheet with the Google Sheet URL.
 * AC #5 (Story 3.1): Intent.ACTION_SEND with Sheet URL as content.
 */
private fun shareSheetLink(context: Context, sheetUrl: String) {
    val shareIntent = Intent.createChooser(ShareUtils.createShareSheetIntent(sheetUrl), null)
    context.startActivity(shareIntent)
}
