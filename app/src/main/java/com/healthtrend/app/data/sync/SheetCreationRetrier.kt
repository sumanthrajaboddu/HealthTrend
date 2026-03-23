package com.healthtrend.app.data.sync

import com.healthtrend.app.data.repository.AppSettingsRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Encapsulates the "retry sheet creation if needed" logic (Story 3.4 AC #4).
 * Used at app launch: if the user is signed in but no Sheet URL exists
 * (e.g., creation failed during sign-in), creates a "HealthTrend" sheet.
 *
 * This class does NOT catch exceptions — callers are responsible for
 * handling failures and CancellationException.
 */
@Singleton
class SheetCreationRetrier @Inject constructor(
    private val appSettingsRepository: AppSettingsRepository,
    private val sheetsClient: SheetsClient
) {

    /**
     * Check if a Google Sheet needs to be created and create it if so.
     * First searches Google Drive for an existing "HealthTrend" sheet
     * to enable cross-device reuse. Only creates a new sheet if none found.
     *
     * Conditions for action:
     * 1. AppSettings exist
     * 2. A Google account email is stored (user is signed in)
     * 3. No Sheet URL exists yet
     *
     * @return true if a sheet was found or created, false if skipped
     * @throws Exception on API failure (caller should handle)
     */
    suspend fun retryIfNeeded(): Boolean {
        val settings = appSettingsRepository.getSettingsOnce() ?: return false
        val email = settings.googleAccountEmail
        if (email.isNullOrEmpty() || settings.sheetUrl.isNotEmpty()) return false

        // Try to find an existing sheet first (cross-device reuse)
        val existingUrl = sheetsClient.findSheet(email, SheetsClient.DEFAULT_SHEET_TITLE)
        val sheetUrl = existingUrl
            ?: sheetsClient.createSheet(email, SheetsClient.DEFAULT_SHEET_TITLE)

        appSettingsRepository.updateSheetUrl(sheetUrl)
        return true
    }
}
