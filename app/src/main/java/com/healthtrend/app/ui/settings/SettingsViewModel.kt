package com.healthtrend.app.ui.settings

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtrend.app.data.auth.GoogleAuthClient
import com.healthtrend.app.data.auth.GoogleSignInResult
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.notification.ReminderScheduler
import com.healthtrend.app.data.notification.getEnabledForSlot
import com.healthtrend.app.data.notification.getTimeForSlot
import com.healthtrend.app.data.notification.parseAlarmTime
import com.healthtrend.app.data.repository.AppSettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Settings screen.
 * Manages auto-save for patient name (debounced) and Sheet URL (immediate with validation).
 * Orchestrates Google Sign-In auth flow — no auth logic in composables.
 * Orchestrates reminder configuration — toggle/time changes auto-saved + alarms updated.
 * No save button — changes persist automatically.
 */
@OptIn(FlowPreview::class)
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val googleAuthClient: GoogleAuthClient,
    private val reminderScheduler: ReminderScheduler
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
        patientNameOverride
    ) { settings, authState, nameOverride ->
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
                isSheetUrlValid = settings.sheetUrl.isEmpty() || isValidSheetUrl(settings.sheetUrl),
                authState = resolvedAuthState,
                globalRemindersEnabled = settings.globalRemindersEnabled,
                slotReminders = buildSlotReminderStates(settings)
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

    /**
     * Called when Sheet URL text changes.
     * Persists immediately (no debounce) with format validation.
     */
    fun onSheetUrlChanged(url: String) {
        viewModelScope.launch {
            appSettingsRepository.updateSheetUrl(url)
        }
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
            if (enabled) {
                val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
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
            reminderScheduler.scheduleAlarm(timeSlot, hour, minute)
        }
    }

    // ── Auth Handlers ──────────────────────────────────────────────

    /**
     * Initiate Google Sign-In via Credential Manager.
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

        /** Basic Google Sheets URL pattern for validation. */
        private val SHEETS_URL_PATTERN = Regex(
            "^https://docs\\.google\\.com/spreadsheets/d/[a-zA-Z0-9_-]+.*$"
        )

        /** 12-hour time formatter for display (e.g., "8:00 AM"). */
        private val TIME_DISPLAY_FORMATTER: DateTimeFormatter =
            DateTimeFormatter.ofPattern("h:mm a")

        /**
         * Validates whether a URL looks like a Google Sheets URL.
         * Returns true for valid format, false for invalid.
         */
        fun isValidSheetUrl(url: String): Boolean {
            return SHEETS_URL_PATTERN.matches(url)
        }

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
