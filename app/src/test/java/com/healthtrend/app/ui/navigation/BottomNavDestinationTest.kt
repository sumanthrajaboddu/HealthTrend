package com.healthtrend.app.ui.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Tests for BottomNavDestination — verifies routes, labels, and structure.
 */
class BottomNavDestinationTest {

    @Test
    fun `bottom nav has exactly 3 destinations`() {
        assertEquals(3, BottomNavDestination.entries.size)
    }

    @Test
    fun `today destination has daycard route`() {
        assertEquals("daycard", BottomNavDestination.TODAY.route)
    }

    @Test
    fun `analytics destination has analytics route`() {
        assertEquals("analytics", BottomNavDestination.ANALYTICS.route)
    }

    @Test
    fun `settings destination has settings route`() {
        assertEquals("settings", BottomNavDestination.SETTINGS.route)
    }

    @Test
    fun `today destination has correct label`() {
        assertEquals("Today", BottomNavDestination.TODAY.label)
    }

    @Test
    fun `analytics destination has correct label`() {
        assertEquals("Analytics", BottomNavDestination.ANALYTICS.label)
    }

    @Test
    fun `settings destination has correct label`() {
        assertEquals("Settings", BottomNavDestination.SETTINGS.label)
    }

    @Test
    fun `all destinations have icons`() {
        BottomNavDestination.entries.forEach { destination ->
            assertNotNull("${destination.name} should have an icon", destination.icon)
            assertNotNull("${destination.name} should have a selected icon", destination.selectedIcon)
        }
    }

    @Test
    fun `destinations are in correct display order`() {
        val entries = BottomNavDestination.entries
        assertEquals(BottomNavDestination.TODAY, entries[0])
        assertEquals(BottomNavDestination.ANALYTICS, entries[1])
        assertEquals(BottomNavDestination.SETTINGS, entries[2])
    }
}
