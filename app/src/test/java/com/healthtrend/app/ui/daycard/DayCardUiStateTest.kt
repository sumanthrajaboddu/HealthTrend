package com.healthtrend.app.ui.daycard

import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate

/**
 * Tests for DayCardUiState sealed interface structure.
 */
class DayCardUiStateTest {

    @Test
    fun `Loading is a valid UiState`() {
        val state: DayCardUiState = DayCardUiState.Loading
        assertTrue(state is DayCardUiState.Loading)
    }

    @Test
    fun `Success holds date and entries map`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = TimeSlot.AFTERNOON
        )

        assertEquals(LocalDate.of(2026, 2, 8), state.date)
        assertEquals(4, state.entries.size)
        assertEquals(TimeSlot.AFTERNOON, state.currentTimeSlot)
    }

    @Test
    fun `Success entries map includes all TimeSlots`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = null
        )

        assertTrue(state.entries.containsKey(TimeSlot.MORNING))
        assertTrue(state.entries.containsKey(TimeSlot.AFTERNOON))
        assertTrue(state.entries.containsKey(TimeSlot.EVENING))
        assertTrue(state.entries.containsKey(TimeSlot.NIGHT))
    }

    @Test
    fun `Success with logged entry maps severity correctly`() {
        val entry = HealthEntry(
            id = 1L,
            date = "2026-02-08",
            timeSlot = TimeSlot.MORNING,
            severity = Severity.MILD
        )
        val entries = mapOf<TimeSlot, HealthEntry?>(
            TimeSlot.MORNING to entry,
            TimeSlot.AFTERNOON to null,
            TimeSlot.EVENING to null,
            TimeSlot.NIGHT to null
        )
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = TimeSlot.MORNING
        )

        assertEquals(Severity.MILD, state.entries[TimeSlot.MORNING]?.severity)
        assertNull(state.entries[TimeSlot.AFTERNOON])
    }

    @Test
    fun `Error holds message`() {
        val state = DayCardUiState.Error("Something went wrong")
        assertEquals("Something went wrong", state.message)
    }

    @Test
    fun `currentTimeSlot can be null when no slot matches`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = null
        )
        assertNull(state.currentTimeSlot)
    }

    // --- isToday field tests (Story 2.1) ---

    @Test
    fun `isToday defaults to true`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = null
        )
        assertTrue(state.isToday)
    }

    @Test
    fun `isToday can be set to false for past dates`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 7),
            entries = entries,
            currentTimeSlot = null,
            isToday = false
        )
        assertFalse(state.isToday)
    }

    @Test
    fun `isToday can be explicitly set to true`() {
        val entries = TimeSlot.entries.associateWith<TimeSlot, HealthEntry?> { null }
        val state = DayCardUiState.Success(
            date = LocalDate.of(2026, 2, 8),
            entries = entries,
            currentTimeSlot = TimeSlot.MORNING,
            isToday = true
        )
        assertTrue(state.isToday)
    }
}
