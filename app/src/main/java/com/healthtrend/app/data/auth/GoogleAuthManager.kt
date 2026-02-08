package com.healthtrend.app.data.auth

import android.content.Context
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
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
    @ApplicationContext private val context: Context
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
            val signInOption = GetSignInWithGoogleOption.Builder(SERVER_CLIENT_ID)
                .build()

            val request = GetCredentialRequest.Builder()
                .addCredentialOption(signInOption)
                .build()

            val result = credentialManager.getCredential(activityContext, request)
            val credential = result.credential

            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val email = googleIdTokenCredential.id
            val idToken = googleIdTokenCredential.idToken

            GoogleSignInResult.Success(email = email, idToken = idToken)
        } catch (e: GetCredentialCancellationException) {
            GoogleSignInResult.Cancelled
        } catch (e: NoCredentialException) {
            GoogleSignInResult.Failure("No Google account available")
        } catch (e: GetCredentialException) {
            GoogleSignInResult.Failure(e.message ?: "Sign-in failed")
        } catch (e: Exception) {
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
        /**
         * Web server client ID from Google Cloud Console.
         * This is the OAuth 2.0 client ID (Web application type).
         * Must be configured in Google Cloud Console for the project.
         *
         * TODO: Move to BuildConfig or google-services.json when project is configured
         * with a Google Cloud project. For now, this is a placeholder that must be
         * replaced with the actual client ID before auth will work.
         */
        const val SERVER_CLIENT_ID = "YOUR_SERVER_CLIENT_ID.apps.googleusercontent.com"
    }
}
