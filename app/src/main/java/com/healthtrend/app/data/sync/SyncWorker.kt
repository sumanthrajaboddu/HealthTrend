package com.healthtrend.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtrend.app.data.repository.AppSettingsRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker for two-way Google Sheets sync.
 * Delegates sync protocol execution to [SyncLogic] (testable without Android context).
 *
 * Preconditions checked here:
 * - Sheet URL must be configured in AppSettings
 * - Google account must be signed in
 * - If not configured, sync silently skips — no error, no prompt
 *
 * All errors → Result.retry() with exponential backoff. ZERO user-facing errors.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val appSettingsRepository: AppSettingsRepository,
    private val syncLogic: SyncLogic
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Precondition: must have sheet URL and be signed in
            appSettingsRepository.ensureSettingsExist()
            val settings = appSettingsRepository.getSettingsOnce() ?: return Result.success()
            val sheetUrl = settings.sheetUrl
            val email = settings.googleAccountEmail

            if (sheetUrl.isEmpty() || email.isNullOrEmpty()) {
                // Not configured — silently skip, no error
                return Result.success()
            }

            // Delegate to SyncLogic (push-then-pull protocol)
            syncLogic.executeSync(sheetUrl, email)

            Result.success()
        } catch (e: Exception) {
            // ALL sync errors are SILENT — WorkManager exponential backoff handles retries
            Result.retry()
        }
    }

    companion object {
        /** Unique work name for immediate one-time sync. */
        const val WORK_NAME = "health_trend_sync"

        /** Unique work name for periodic sync. */
        const val PERIODIC_WORK_NAME = "health_trend_periodic_sync"
    }
}
