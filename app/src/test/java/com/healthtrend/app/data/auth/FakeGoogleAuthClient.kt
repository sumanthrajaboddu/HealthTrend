package com.healthtrend.app.data.auth

import android.content.Context

/**
 * Fake GoogleAuthClient for unit testing.
 * Allows controlling sign-in/sign-out results deterministically.
 */
class FakeGoogleAuthClient : GoogleAuthClient {

    /** The result to return from signIn. Set before calling. */
    var signInResult: GoogleSignInResult = GoogleSignInResult.Cancelled

    /** Track whether signOut was called. */
    var signOutCalled: Boolean = false
        private set

    /** Track whether signIn was called. */
    var signInCalled: Boolean = false
        private set

    override suspend fun signIn(activityContext: Context): GoogleSignInResult {
        signInCalled = true
        return signInResult
    }

    override suspend fun signOut() {
        signOutCalled = true
    }

    fun reset() {
        signInResult = GoogleSignInResult.Cancelled
        signOutCalled = false
        signInCalled = false
    }
}
