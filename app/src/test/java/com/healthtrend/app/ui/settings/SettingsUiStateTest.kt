package com.healthtrend.app.ui.settings

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for SettingsUiState and AuthState sealed interfaces.
 */
class SettingsUiStateTest {

    @Test
    fun `Loading is a valid SettingsUiState`() {
        val state: SettingsUiState = SettingsUiState.Loading
        assertTrue(state is SettingsUiState.Loading)
    }

    @Test
    fun `Success default values are correct`() {
        val state = SettingsUiState.Success()
        assertEquals("", state.patientName)
        assertEquals("", state.sheetUrl)
        assertTrue(state.isSheetUrlValid)
        assertTrue(state.authState is AuthState.SignedOut)
    }

    @Test
    fun `Success with custom values and signed in`() {
        val state = SettingsUiState.Success(
            patientName = "Uncle",
            sheetUrl = "https://docs.google.com/spreadsheets/d/abc123",
            isSheetUrlValid = true,
            authState = AuthState.SignedIn("raja@example.com")
        )
        assertEquals("Uncle", state.patientName)
        assertEquals("https://docs.google.com/spreadsheets/d/abc123", state.sheetUrl)
        assertTrue(state.isSheetUrlValid)
        val signedIn = state.authState as AuthState.SignedIn
        assertEquals("raja@example.com", signedIn.email)
    }

    @Test
    fun `Success with invalid URL`() {
        val state = SettingsUiState.Success(
            sheetUrl = "invalid-url",
            isSheetUrlValid = false
        )
        assertFalse(state.isSheetUrlValid)
    }

    // --- AuthState Tests ---

    @Test
    fun `AuthState SignedOut is default`() {
        val state: AuthState = AuthState.SignedOut
        assertTrue(state is AuthState.SignedOut)
    }

    @Test
    fun `AuthState SignedIn contains email`() {
        val state = AuthState.SignedIn("raja@example.com")
        assertEquals("raja@example.com", state.email)
    }

    @Test
    fun `AuthState RefreshFailed is valid state`() {
        val state: AuthState = AuthState.RefreshFailed
        assertTrue(state is AuthState.RefreshFailed)
    }

    @Test
    fun `AuthState Loading is valid state`() {
        val state: AuthState = AuthState.Loading
        assertTrue(state is AuthState.Loading)
    }
}
