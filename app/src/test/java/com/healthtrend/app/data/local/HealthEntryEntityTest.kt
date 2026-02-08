package com.healthtrend.app.data.local

import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class HealthEntryEntityTest {

    @Test
    fun `health entry has correct default values`() {
        val entry = HealthEntry(
            date = "2026-02-07",
            timeSlot = TimeSlot.MORNING,
            severity = Severity.MILD
        )
        assertEquals(0L, entry.id)
        assertEquals("2026-02-07", entry.date)
        assertEquals(TimeSlot.MORNING, entry.timeSlot)
        assertEquals(Severity.MILD, entry.severity)
        assertFalse(entry.synced)
    }

    @Test
    fun `health entry updatedAt defaults to current time`() {
        val before = System.currentTimeMillis()
        val entry = HealthEntry(
            date = "2026-02-07",
            timeSlot = TimeSlot.AFTERNOON,
            severity = Severity.MODERATE
        )
        val after = System.currentTimeMillis()
        assert(entry.updatedAt in before..after)
    }

    @Test
    fun `health entry can have all severity values`() {
        Severity.entries.forEach { severity ->
            val entry = HealthEntry(
                date = "2026-02-07",
                timeSlot = TimeSlot.MORNING,
                severity = severity
            )
            assertEquals(severity, entry.severity)
        }
    }

    @Test
    fun `health entry can have all time slot values`() {
        TimeSlot.entries.forEach { timeSlot ->
            val entry = HealthEntry(
                date = "2026-02-07",
                timeSlot = timeSlot,
                severity = Severity.NO_PAIN
            )
            assertEquals(timeSlot, entry.timeSlot)
        }
    }
}
