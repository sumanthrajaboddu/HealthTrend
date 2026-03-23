package com.healthtrend.app.ui.settings

import android.content.Context
import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.api.client.googleapis.extensions.android.gms.auth.UserRecoverableAuthIOException
import com.healthtrend.app.data.auth.GoogleAuthClient
import com.healthtrend.app.data.auth.GoogleSignInResult
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.notification.ReminderScheduler
import com.healthtrend.app.data.notification.getEnabledForSlot
import com.healthtrend.app.data.notification.getTimeForSlot
import com.healthtrend.app.data.notification.parseAlarmTime
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.sync.SheetsClient
import com.healthtrend.app.data.sync.SyncTrigger
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException

/**
 * ViewModel for the Settings screen.
 * Manages auto-save for patient name (debounced).
 * Orchestrates Google Sign-In auth flow and auto-creates Google Sheet on first sign-in.
 * Orchestrates reminder configuration — toggle/time changes auto-saved + alarms updated.
 * No save button — changes persist automatically.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val reminderScheduler: ReminderScheduler,
    private val sheetsClient: SheetsClient,
    private val syncTrigger: SyncTrigger
) : ViewModel() {

    /**
     * Internal debounce flow for patient name changes.
     * Text input triggers this; after 500ms debounce, persists to Room.
     */
    private val patientNameInput = MutableStateFlow<String?>(null)

    /**
     * Immediate UI override for patient name input (debounced persistence).
     * Allows UI to reflect typing instantly while saving occurs later.
     */
    private val patientNameOverride = MutableStateFlow<String?>(null)

    /**
     * Auth state managed internally.
     * Derived from stored email + sign-in/out actions.
     */
    private val _authState = MutableStateFlow<AuthState>(AuthState.SignedOut)

    /**
     * Tracks whether a Google Sheet is currently being auto-created.
     * True between the start and end of [ensureSheetExists].
     */
    private val _sheetCreationInProgress = MutableStateFlow(false)

    /**
     * Diagnostic: captures sheet creation error message for on-device debugging.
     * TODO: Remove after diagnosing sheet creation failure — AC #4 requires silent errors.
     */
    private val _sheetCreationError = MutableStateFlow<String?>(null)

    /**
     * One-shot event: sends a recovery Intent when the Sheets API requires OAuth scope consent.
     * UI must launch this Intent and call [onSheetAuthRecoveryResult] with the outcome.
     */
    private val _authRecoveryEvent = Channel<Intent>(Channel.BUFFERED)
    val authRecoveryEvent: Flow<Intent> = _authRecoveryEvent.receiveAsFlow()

    /**
     * Stores the account email for retry after OAuth scope consent is granted.
     */
    private var _pendingSheetEmail: String? = null

    init {
        // Ensure settings row exists on first access
        viewModelScope.launch {
            appSettingsRepository.ensureSettingsExist()
        }

        // Debounce patient name input and persist
        patientNameInput
            .debounce(PATIENT_NAME_DEBOUNCE_MS)
            .onEach { name ->
                if (name != null) {
                    appSettingsRepository.updatePatientName(name)
                    // Clear override after persistence; repository flow will supply same value.
                    patientNameOverride.value = null
                }
            }
            .launchIn(viewModelScope)
    }

    /**
     * UI state derived from AppSettings Flow combined with auth state.
     * Uses stateIn with WhileSubscribed for lifecycle awareness.
     */
    val uiState: StateFlow<SettingsUiState> = combine(
        appSettingsRepository.getSettings(),
        _authState,
        patientNameOverride,
        _sheetCreationInProgress,
        _sheetCreationError
    ) { args ->
        @Suppress("UNCHECKED_CAST")
        val settings = args[0] as? com.healthtrend.app.data.model.AppSettings
        val authState = args[1] as AuthState
        val nameOverride = args[2] as? String
        val creatingSheet = args[3] as Boolean
        val creationError = args[4] as? String

        if (settings == null) {
            SettingsUiState.Loading
        } else {
            // Derive auth state from stored email if auth state is still initial
            val resolvedAuthState = if (authState == AuthState.SignedOut && !settings.googleAccountEmail.isNullOrEmpty()) {
                AuthState.SignedIn(settings.googleAccountEmail)
            } else {
                authState
            }

            SettingsUiState.Success(
                patientName = nameOverride ?: settings.patientName,
                sheetUrl = settings.sheetUrl,
                authState = resolvedAuthState,
                globalRemindersEnabled = settings.globalRemindersEnabled,
                slotReminders = buildSlotReminderStates(settings),
                sheetCreationInProgress = creatingSheet,
                sheetCreationError = creationError
            )
        }
    }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SettingsUiState.Loading
        )

    /**
     * Called when patient name text changes.
     * Debounced — persists after 500ms of no typing.
     */
    fun onPatientNameChanged(name: String) {
        patientNameOverride.value = name
        patientNameInput.value = name
    }

    // ── Reminder Configuration Handlers (AC #2, #3, #4) ────────────

    /**
     * Toggle global reminders on/off (AC #2).
     * When disabled: all 4 alarms cancelled immediately.
     * When enabled: all enabled slot alarms re-scheduled.
     */
    fun onGlobalRemindersToggled(enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateGlobalRemindersEnabled(enabled)
            if (!enabled) {
                reminderScheduler.cancelAll()
            } else {
                val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
                reminderScheduler.scheduleAllActive(settings)
            }
        }
    }

    /**
     * Toggle individual slot reminder on/off (AC #3).
     * Only affects the specified slot — others continue unchanged.
     */
    fun onSlotReminderToggled(timeSlot: TimeSlot, enabled: Boolean) {
        viewModelScope.launch {
            appSettingsRepository.updateSlotReminderEnabled(timeSlot, enabled)
            val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
            if (enabled && settings.globalRemindersEnabled && getEnabledForSlot(settings, timeSlot)) {
                val timeStr = getTimeForSlot(settings, timeSlot)
                val time = parseAlarmTime(timeStr)
                reminderScheduler.scheduleAlarm(timeSlot, time.hour, time.minute)
            } else {
                reminderScheduler.cancelAlarm(timeSlot)
            }
        }
    }

    /**
     * Change a slot's reminder time (AC #4).
     * Cancels old alarm, schedules new alarm at the new time.
     */
    fun onSlotTimeChanged(timeSlot: TimeSlot, hour: Int, minute: Int) {
        val timeStr = String.format("%02d:%02d", hour, minute)
        viewModelScope.launch {
            appSettingsRepository.updateSlotReminderTime(timeSlot, timeStr)
            // Cancel old alarm and schedule at new time
            reminderScheduler.cancelAlarm(timeSlot)
            val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
            if (settings.globalRemindersEnabled && getEnabledForSlot(settings, timeSlot)) {
                reminderScheduler.scheduleAlarm(timeSlot, hour, minute)
            }
        }
    }

    // ── Auth Handlers ──────────────────────────────────────────────

    /**
     * Initiate Google Sign-In via Credential Manager.
     * On success, auto-creates a "HealthTrend" Google Sheet if no Sheet URL exists yet.
     * Must be called with an Activity context for the Credential Manager UI.
     *
     * @param activityContext Activity context (required by Credential Manager)
     */
    fun onSignIn(activityContext: Context) {
        _authState.value = AuthState.Loading
        viewModelScope.launch {
            when (val result = googleAuthClient.signIn(activityContext)) {
                is GoogleSignInResult.Success -> {
                    appSettingsRepository.updateGoogleAccount(result.email)
                    _authState.value = AuthState.SignedIn(result.email)

                    // Auto-create Google Sheet if none exists yet (Story 3.4 AC #1)
                    ensureSheetExists(result.email)
                }
                is GoogleSignInResult.Failure -> {
                    _authState.value = AuthState.RefreshFailed
                }
                is GoogleSignInResult.Cancelled -> {
                    // User cancelled — restore to signed-out state
                    _authState.value = AuthState.SignedOut
                }
            }
        }
    }

    /**
     * Auto-create a Google Sheet titled "HealthTrend" if no Sheet URL exists.
     * Skips silently if a URL is already saved (AC #3).
     *
     * If the Sheets API scope hasn't been consented yet, catches
     * [UserRecoverableAuthIOException] and emits the recovery Intent
     * via [authRecoveryEvent] for the UI to launch the consent screen.
     * After consent, [onSheetAuthRecoveryResult] retries creation.
     *
     * Other failures are silent — retry happens on next app launch (AC #4).
     */
    private fun ensureSheetExists(accountEmail: String) {
        viewModelScope.launch {
            try {
                val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
                if (settings.sheetUrl.isNotEmpty()) return@launch // AC #3: already has a sheet

                _sheetCreationInProgress.value = true
                _sheetCreationError.value = null

                // Try to find an existing sheet first (cross-device reuse),
                // fall back to creating a new one if not found.
                val sheetUrl = withContext(Dispatchers.IO) {
                    sheetsClient.findSheet(accountEmail, SHEET_TITLE)
                        ?: sheetsClient.createSheet(accountEmail, SHEET_TITLE)
                }

                appSettingsRepository.updateSheetUrl(sheetUrl)
                // Trigger immediate sync to pull existing data from the sheet
                // (critical for cross-device reuse — user expects to see their data)
                syncTrigger.enqueueImmediateSync()
            } catch (e: UserRecoverableAuthIOException) {
                // OAuth scope consent needed — save email for retry and send recovery intent
                _pendingSheetEmail = accountEmail
                e.intent?.let { _authRecoveryEvent.send(it) }
            } catch (e: CancellationException) {
                throw e // Preserve structured concurrency
            } catch (e: Exception) {
                // AC #4: silent failure — retry on next app launch
                _sheetCreationError.value = "${e.javaClass.simpleName}: ${e.message}"
            } finally {
                _sheetCreationInProgress.value = false
            }
        }
    }

    /**
     * Called after the OAuth scope consent screen completes.
     * If the user granted consent, retries sheet creation with the stored email.
     *
     * @param success true if RESULT_OK, false if cancelled/denied
     */
    fun onSheetAuthRecoveryResult(success: Boolean) {
        val email = _pendingSheetEmail
        _pendingSheetEmail = null
        if (success && email != null) {
            ensureSheetExists(email)
        }
    }

    /**
     * Sign out — clears credential and stored account email.
     * Sync stops until re-authenticated.
     */
    fun onSignOut() {
        viewModelScope.launch {
            googleAuthClient.signOut()
            appSettingsRepository.updateGoogleAccount(null)
            _authState.value = AuthState.SignedOut
        }
    }

    companion object {
        /** Debounce delay for patient name auto-save (milliseconds). */
        const val PATIENT_NAME_DEBOUNCE_MS = 500L

        /** Title for the auto-created Google Sheet (Story 3.4). */
        const val SHEET_TITLE = SheetsClient.DEFAULT_SHEET_TITLE

        /** 12-hour time formatter for display (e.g., "8:00 AM"). */
        private val TIME_DISPLAY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a")

        /**
         * Build the list of [SlotReminderState] from [AppSettings].
         * Maps each [TimeSlot] to its enabled state and display time.
         */
        fun buildSlotReminderStates(settings: AppSettings): List<SlotReminderState> {
            return TimeSlot.entries.map { slot ->
                val timeStr = getTimeForSlot(settings, slot)
                val localTime = parseAlarmTime(timeStr)
                SlotReminderState(
                    timeSlot = slot,
                    enabled = getEnabledForSlot(settings, slot),
                    timeDisplay = localTime.format(TIME_DISPLAY_FORMATTER),
                    hour = localTime.hour,
                    minute = localTime.minute
                )
            }
        }
    }
}
