package com.healthtrend.app.ui.settings

import com.healthtrend.app.data.model.TimeSlot

/**
 * UI state for the Settings screen.
 * Sealed interface per project conventions — never data class or open class.
 */
sealed interface SettingsUiState {

    /**
     * Settings are loading (initial state before Room emits).
     */
    data object Loading : SettingsUiState

    /**
     * Settings loaded successfully.
     */
    data class Success(
        val patientName: String = "",
        val sheetUrl: String = "",
        val isSheetUrlValid: Boolean = true,
        val authState: AuthState = AuthState.SignedOut,
        val globalRemindersEnabled: Boolean = true,
        val slotReminders: List<SlotReminderState> = emptyList()
    ) : SettingsUiState
}

/**
 * Per-slot reminder state for the Settings UI.
 */
data class SlotReminderState(
    val timeSlot: TimeSlot,
    val enabled: Boolean,
    val timeDisplay: String,
    val hour: Int,
    val minute: Int
)

/**
 * Authentication state for Google Sign-In.
 * Sealed interface — not data class, not open class.
 */
sealed interface AuthState {
    /** No Google account signed in. */
    data object SignedOut : AuthState

    /** Signed in with a Google account. */
    data class SignedIn(val email: String) : AuthState

    /** Token refresh failed — user must re-authenticate. */
    data object RefreshFailed : AuthState

    /** Sign-in is in progress. */
    data object Loading : AuthState
}
