package com.healthtrend.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import java.time.LocalTime

class TimeSlotTest {

    @Test
    fun `time slot enum has exactly four values`() {
        assertEquals(4, TimeSlot.entries.size)
    }

    @Test
    fun `time slot values are in correct order`() {
        val values = TimeSlot.entries
        assertEquals(TimeSlot.MORNING, values[0])
        assertEquals(TimeSlot.AFTERNOON, values[1])
        assertEquals(TimeSlot.EVENING, values[2])
        assertEquals(TimeSlot.NIGHT, values[3])
    }

    @Test
    fun `time slot display names are correct`() {
        assertEquals("Morning", TimeSlot.MORNING.displayName)
        assertEquals("Afternoon", TimeSlot.AFTERNOON.displayName)
        assertEquals("Evening", TimeSlot.EVENING.displayName)
        assertEquals("Night", TimeSlot.NIGHT.displayName)
    }

    @Test
    fun `time slot default reminder times are correct`() {
        assertEquals(LocalTime.of(8, 0), TimeSlot.MORNING.defaultReminderTime)
        assertEquals(LocalTime.of(13, 0), TimeSlot.AFTERNOON.defaultReminderTime)
        assertEquals(LocalTime.of(18, 0), TimeSlot.EVENING.defaultReminderTime)
        assertEquals(LocalTime.of(22, 0), TimeSlot.NIGHT.defaultReminderTime)
    }

    @Test
    fun `time slot icons are defined and non-empty`() {
        TimeSlot.entries.forEach { slot ->
            assertNotNull("Icon should not be null for ${slot.name}", slot.icon)
        }
    }

    @Test
    fun `each time slot has unique display name`() {
        val names = TimeSlot.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }

    @Test
    fun `each time slot has unique default reminder time`() {
        val times = TimeSlot.entries.map { it.defaultReminderTime }
        assertEquals(times.size, times.toSet().size)
    }

    @Test
    fun `time slots are ordered chronologically by default reminder time`() {
        val times = TimeSlot.entries.map { it.defaultReminderTime }
        assertEquals(times, times.sorted())
    }
}
