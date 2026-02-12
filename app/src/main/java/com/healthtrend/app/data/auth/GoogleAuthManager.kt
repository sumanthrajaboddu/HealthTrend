package com.healthtrend.app.data.auth

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.healthtrend.app.BuildConfig
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Result of a Google sign-in attempt.
 */
sealed interface GoogleSignInResult {
    data class Success(val email: String, val idToken: String) : GoogleSignInResult
    data class Failure(val message: String) : GoogleSignInResult
    data object Cancelled : GoogleSignInResult
}

/**
 * Interface for Google authentication — enables testing with fakes.
 */
interface GoogleAuthClient {
    suspend fun signIn(activityContext: Context): GoogleSignInResult
    suspend fun signOut()
}

/**
 * Manages Google authentication via Credential Manager 1.5.0.
 * One sign-in, persistent forever — user NEVER sees re-auth after initial setup.
 * Token refresh is automatic and silent via Credential Manager.
 *
 * This is a data-layer class, NOT UI — injected via Hilt as singleton.
 * SettingsViewModel orchestrates auth flow — no auth logic in composables.
 */
@Singleton
class GoogleAuthManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : GoogleAuthClient {

    private val credentialManager = CredentialManager.create(context)

    /**
     * Launch Credential Manager sign-in flow.
     * Uses GetSignInWithGoogleOption for direct Google account selection.
     *
     * @param activityContext Must be an Activity context for the Credential Manager UI.
     * @return [GoogleSignInResult] with email and ID token on success.
     */
    override suspend fun signIn(activityContext: Context): GoogleSignInResult {
        return try {
            if (BuildConfig.GOOGLE_SERVER_CLIENT_ID == MISSING_SERVER_CLIENT_ID_VALUE) {
                Log.e(TAG, "Server client ID is not configured — still using placeholder value")
                return GoogleSignInResult.Failure("Server client ID is not configured")
            }

            Log.d(TAG, "Starting sign-in with server client ID: ${BuildConfig.GOOGLE_SERVER_CLIENT_ID.take(20)}…")

            val signInOption = GetSignInWithGoogleOption.Builder(BuildConfig.GOOGLE_SERVER_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            Log.d(TAG, "Credential received — type: ${credential.type}")

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = googleIdTokenCredential.id
            val idToken = googleIdTokenCredential.idToken

            Log.d(TAG, "Sign-in successful — email: $email")
            GoogleSignInResult.Success(email = email, idToken = idToken)
        } catch (e: GetCredentialCancellationException) {
            Log.w(TAG, "Sign-in cancelled by user or system", e)
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            Log.e(TAG, "No credential available", e)
            GoogleSignInResult.Failure("No Google account available")
        } catch (e: GetCredentialException) {
            Log.e(TAG, "GetCredentialException — type: ${e.type}, message: ${e.message}", e)
            GoogleSignInResult.Failure(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected exception during sign-in", e)
            GoogleSignInResult.Failure(e.message ?: "Unexpected error during sign-in")
        }
    }

    /**
     * Clear stored credentials — effectively sign out.
     * After this, user must sign in again to use sync.
     */
    override suspend fun signOut() {
        try {
            credentialManager.clearCredentialState(ClearCredentialStateRequest())
        } catch (_: Exception) {
            // Sign-out errors are non-critical — credential state may already be cleared
        }
    }

    companion object {
        private const val TAG = "GoogleAuthManager"
        private const val MISSING_SERVER_CLIENT_ID_VALUE =
            "YOUR_SERVER_CLIENT_ID.apps.googleusercontent.com"
    }
}
