package com.healthtrend.app.data.model

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SeverityTest {

    @Test
    fun `severity enum has exactly four values`() {
        assertEquals(4, Severity.entries.size)
    }

    @Test
    fun `severity values are in correct order`() {
        val values = Severity.entries
        assertEquals(Severity.NO_PAIN, values[0])
        assertEquals(Severity.MILD, values[1])
        assertEquals(Severity.MODERATE, values[2])
        assertEquals(Severity.SEVERE, values[3])
    }

    @Test
    fun `severity numeric values are correct`() {
        assertEquals(0, Severity.NO_PAIN.numericValue)
        assertEquals(1, Severity.MILD.numericValue)
        assertEquals(2, Severity.MODERATE.numericValue)
        assertEquals(3, Severity.SEVERE.numericValue)
    }

    @Test
    fun `severity display names are correct`() {
        assertEquals("No Pain", Severity.NO_PAIN.displayName)
        assertEquals("Mild", Severity.MILD.displayName)
        assertEquals("Moderate", Severity.MODERATE.displayName)
        assertEquals("Severe", Severity.SEVERE.displayName)
    }

    @Test
    fun `severity primary colors match specification`() {
        assertEquals(Color(0xFF4CAF50), Severity.NO_PAIN.color)
        assertEquals(Color(0xFFFFC107), Severity.MILD.color)
        assertEquals(Color(0xFFFF9800), Severity.MODERATE.color)
        assertEquals(Color(0xFFF44336), Severity.SEVERE.color)
    }

    @Test
    fun `severity soft colors are defined and differ from primary`() {
        Severity.entries.forEach { severity ->
            assertNotEquals(
                "Soft color should differ from primary for ${severity.name}",
                severity.color,
                severity.softColor
            )
        }
    }

    @Test
    fun `severity dark soft colors are defined and differ from light soft colors`() {
        Severity.entries.forEach { severity ->
            assertNotEquals(
                "Dark soft color should differ from light soft color for ${severity.name}",
                severity.softColor,
                severity.softColorDark
            )
        }
    }

    @Test
    fun `severity dark soft colors differ from primary colors`() {
        Severity.entries.forEach { severity ->
            assertNotEquals(
                "Dark soft color should differ from primary for ${severity.name}",
                severity.color,
                severity.softColorDark
            )
        }
    }

    @Test
    fun `each severity has unique numeric value`() {
        val numericValues = Severity.entries.map { it.numericValue }
        assertEquals(numericValues.size, numericValues.toSet().size)
    }

    @Test
    fun `each severity has unique display name`() {
        val names = Severity.entries.map { it.displayName }
        assertEquals(names.size, names.toSet().size)
    }
}
