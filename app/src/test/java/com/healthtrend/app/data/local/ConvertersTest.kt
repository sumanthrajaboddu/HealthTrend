package com.healthtrend.app.data.local

import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ConvertersTest {

    private val converters = Converters()

    @Test
    fun `severity to string conversion`() {
        Severity.entries.forEach { severity ->
            assertEquals(severity.name, converters.fromSeverity(severity))
        }
    }

    @Test
    fun `string to severity conversion`() {
        Severity.entries.forEach { severity ->
            assertEquals(severity, converters.toSeverity(severity.name))
        }
    }

    @Test
    fun `null severity converts to null string`() {
        assertNull(converters.fromSeverity(null))
    }

    @Test
    fun `null string converts to null severity`() {
        assertNull(converters.toSeverity(null))
    }

    @Test
    fun `time slot to string conversion`() {
        TimeSlot.entries.forEach { timeSlot ->
            assertEquals(timeSlot.name, converters.fromTimeSlot(timeSlot))
        }
    }

    @Test
    fun `string to time slot conversion`() {
        TimeSlot.entries.forEach { timeSlot ->
            assertEquals(timeSlot, converters.toTimeSlot(timeSlot.name))
        }
    }

    @Test
    fun `null time slot converts to null string`() {
        assertNull(converters.fromTimeSlot(null))
    }

    @Test
    fun `null string converts to null time slot`() {
        assertNull(converters.toTimeSlot(null))
    }
}
