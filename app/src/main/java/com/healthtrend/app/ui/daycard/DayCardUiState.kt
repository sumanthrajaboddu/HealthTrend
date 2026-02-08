package com.healthtrend.app.ui.daycard

import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.TimeSlot
import java.time.LocalDate

/**
 * UI state for the Day Card screen.
 * Sealed interface — NEVER data class (per project rules).
 */
sealed interface DayCardUiState {

    /** Initial loading state (data from Room is near-instant, so brief). */
    data object Loading : DayCardUiState

    /**
     * Day Card data loaded successfully.
     * @param date The selected date (today or a past date).
     * @param entries Map of each TimeSlot to its HealthEntry (null = not logged).
     * @param currentTimeSlot The time slot matching the current device time, for subtle highlight.
     *   Null when viewing a past date (highlight only applies to today).
     * @param pickerOpenForSlot Which time slot's severity picker is open (null = none).
     * @param allCompleteBloom True when all 4 slots just became logged — triggers bloom animation.
     * @param isToday True when the displayed date is today's date.
     */
    data class Success(
        val date: LocalDate,
        val entries: Map<TimeSlot, HealthEntry?>,
        val currentTimeSlot: TimeSlot?,
        val pickerOpenForSlot: TimeSlot? = null,
        val allCompleteBloom: Boolean = false,
        val isToday: Boolean = true
    ) : DayCardUiState

    /** Error state (unlikely with local Room, but defensive). */
    data class Error(val message: String) : DayCardUiState
}
