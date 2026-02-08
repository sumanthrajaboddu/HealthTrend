package com.healthtrend.app.data.sync

import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * Unit tests for GoogleSheetsService helper methods.
 */
class GoogleSheetsServiceTest {

    @Test
    fun `extractSpreadsheetId extracts ID from full URL`() {
        val url = "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit#gid=0"
        val id = GoogleSheetsService.extractSpreadsheetId(url)
        assertEquals("1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms", id)
    }

    @Test
    fun `extractSpreadsheetId extracts ID from minimal URL`() {
        val url = "https://docs.google.com/spreadsheets/d/abc123"
        val id = GoogleSheetsService.extractSpreadsheetId(url)
        assertEquals("abc123", id)
    }

    @Test
    fun `extractSpreadsheetId returns null for invalid URL`() {
        val url = "https://example.com/not-a-sheet"
        val id = GoogleSheetsService.extractSpreadsheetId(url)
        assertNull(id)
    }

    @Test
    fun `parseSeverity returns correct enum for display names`() {
        assertEquals(Severity.NO_PAIN, GoogleSheetsService.parseSeverity("No Pain"))
        assertEquals(Severity.MILD, GoogleSheetsService.parseSeverity("Mild"))
        assertEquals(Severity.MODERATE, GoogleSheetsService.parseSeverity("Moderate"))
        assertEquals(Severity.SEVERE, GoogleSheetsService.parseSeverity("Severe"))
    }

    @Test
    fun `parseSeverity returns null for unknown display name`() {
        assertNull(GoogleSheetsService.parseSeverity("Unknown"))
        assertNull(GoogleSheetsService.parseSeverity(""))
    }

    @Test
    fun `parseTimestamp returns valid long`() {
        assertEquals(1707350400000L, GoogleSheetsService.parseTimestamp("1707350400000"))
    }

    @Test
    fun `parseTimestamp returns 0 for null`() {
        assertEquals(0L, GoogleSheetsService.parseTimestamp(null))
    }

    @Test
    fun `parseTimestamp returns 0 for invalid string`() {
        assertEquals(0L, GoogleSheetsService.parseTimestamp("not-a-number"))
    }

    @Test
    fun `SEVERITY_COLUMN maps all time slots`() {
        assertEquals("B", GoogleSheetsService.SEVERITY_COLUMN[TimeSlot.MORNING])
        assertEquals("C", GoogleSheetsService.SEVERITY_COLUMN[TimeSlot.AFTERNOON])
        assertEquals("D", GoogleSheetsService.SEVERITY_COLUMN[TimeSlot.EVENING])
        assertEquals("E", GoogleSheetsService.SEVERITY_COLUMN[TimeSlot.NIGHT])
    }

    @Test
    fun `TIMESTAMP_COLUMN maps all time slots`() {
        assertEquals("F", GoogleSheetsService.TIMESTAMP_COLUMN[TimeSlot.MORNING])
        assertEquals("G", GoogleSheetsService.TIMESTAMP_COLUMN[TimeSlot.AFTERNOON])
        assertEquals("H", GoogleSheetsService.TIMESTAMP_COLUMN[TimeSlot.EVENING])
        assertEquals("I", GoogleSheetsService.TIMESTAMP_COLUMN[TimeSlot.NIGHT])
    }

    @Test
    fun `all four time slots have severity and timestamp column mappings`() {
        for (slot in TimeSlot.entries) {
            assertNotNull("Missing severity column for $slot", GoogleSheetsService.SEVERITY_COLUMN[slot])
            assertNotNull("Missing timestamp column for $slot", GoogleSheetsService.TIMESTAMP_COLUMN[slot])
        }
    }
}
