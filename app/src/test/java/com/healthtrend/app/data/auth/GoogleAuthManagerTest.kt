package com.healthtrend.app.data.auth

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for GoogleSignInResult sealed interface.
 * GoogleAuthManager itself requires Android context (Credential Manager)
 * so full auth flow testing requires instrumentation tests.
 * These tests validate the result types and data contracts.
 */
class GoogleAuthManagerTest {

    @Test
    fun `Success result contains email and idToken`() {
        val result = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "test-id-token-123"
        )
        assertEquals("raja@example.com", result.email)
        assertEquals("test-id-token-123", result.idToken)
    }

    @Test
    fun `Failure result contains error message`() {
        val result = GoogleSignInResult.Failure("Network error")
        assertEquals("Network error", result.message)
    }

    @Test
    fun `Cancelled is a valid result type`() {
        val result: GoogleSignInResult = GoogleSignInResult.Cancelled
        assertEquals(GoogleSignInResult.Cancelled, result)
    }

    @Test
    fun `GoogleAuthClient interface has signIn and signOut`() {
        // Verifies the interface contract exists — concrete implementation
        // requires Android context and is tested via instrumentation.
        val fake = FakeGoogleAuthClient()
        assertTrue(!fake.signInCalled)
        assertTrue(!fake.signOutCalled)
    }
}
