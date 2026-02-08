package com.healthtrend.app.data.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for AppSettings entity.
 * Validates default values and field assignments.
 */
class AppSettingsTest {

    @Test
    fun `default settings have id 1`() {
        val settings = AppSettings()
        assertEquals(1, settings.id)
    }

    @Test
    fun `default patient name is empty string`() {
        val settings = AppSettings()
        assertEquals("", settings.patientName)
    }

    @Test
    fun `default sheet url is empty string`() {
        val settings = AppSettings()
        assertEquals("", settings.sheetUrl)
    }

    @Test
    fun `default google account email is null`() {
        val settings = AppSettings()
        assertNull(settings.googleAccountEmail)
    }

    @Test
    fun `default global reminders enabled is true`() {
        val settings = AppSettings()
        assertTrue(settings.globalRemindersEnabled)
    }

    @Test
    fun `default per-slot reminder enabled flags are all true`() {
        val settings = AppSettings()
        assertTrue(settings.morningReminderEnabled)
        assertTrue(settings.afternoonReminderEnabled)
        assertTrue(settings.eveningReminderEnabled)
        assertTrue(settings.nightReminderEnabled)
    }

    @Test
    fun `default reminder times match time slot defaults`() {
        val settings = AppSettings()
        assertEquals("08:00", settings.morningReminderTime)
        assertEquals("13:00", settings.afternoonReminderTime)
        assertEquals("18:00", settings.eveningReminderTime)
        assertEquals("22:00", settings.nightReminderTime)
    }

    @Test
    fun `custom values are assigned correctly`() {
        val settings = AppSettings(
            patientName = "Uncle",
            sheetUrl = "https://docs.google.com/spreadsheets/d/abc123",
            googleAccountEmail = "raja@example.com"
        )
        assertEquals("Uncle", settings.patientName)
        assertEquals("https://docs.google.com/spreadsheets/d/abc123", settings.sheetUrl)
        assertEquals("raja@example.com", settings.googleAccountEmail)
    }

    @Test
    fun `copy preserves unmodified fields`() {
        val original = AppSettings(patientName = "Uncle")
        val copied = original.copy(sheetUrl = "https://sheets.google.com/test")
        assertEquals("Uncle", copied.patientName)
        assertEquals("https://sheets.google.com/test", copied.sheetUrl)
        assertEquals(1, copied.id)
    }
}
