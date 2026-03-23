package com.healthtrend.app.ui.settings

import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderAccessibilityDescriptionsTest {

    @Test
    fun `global toggle description announces enabled state`() {
        assertEquals(
            "Reminders, enabled. Double tap to toggle.",
            buildGlobalToggleDescription(enabled = true)
        )
    }

    @Test
    fun `slot toggle description announces enabled with time when global is on`() {
        val state = SlotReminderState(
            timeSlot = TimeSlot.MORNING,
            enabled = true,
            timeDisplay = "8:00 AM",
            hour = 8,
            minute = 0
        )

        assertEquals(
            "Morning reminder, enabled, 8:00 AM. Double tap to toggle.",
            buildSlotToggleDescription(state, globalEnabled = true)
        )
    }

    @Test
    fun `slot toggle description announces disabled reason when global is off`() {
        val state = SlotReminderState(
            timeSlot = TimeSlot.MORNING,
            enabled = true,
            timeDisplay = "8:00 AM",
            hour = 8,
            minute = 0
        )

        assertEquals(
            "Morning reminder, disabled. Enable global reminders first.",
            buildSlotToggleDescription(state, globalEnabled = false)
        )
    }

    @Test
    fun `slot time description announces change action when enabled`() {
        val state = SlotReminderState(
            timeSlot = TimeSlot.MORNING,
            enabled = true,
            timeDisplay = "8:00 AM",
            hour = 8,
            minute = 0
        )

        assertEquals(
            "Morning reminder time, 8:00 AM. Double tap to change.",
            buildSlotTimeDescription(state, globalEnabled = true)
        )
    }
}
