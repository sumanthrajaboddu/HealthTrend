package com.healthtrend.app.data.sync

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages sync scheduling via WorkManager.
 * Enqueues immediate one-time sync and hourly periodic sync as fallback.
 * All requests require network connectivity.
 *
 * ZERO user-facing indicators — sync is completely silent.
 */
@Singleton
class SyncManager @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SyncTrigger {
    private val workManager = WorkManager.getInstance(context)

    /**
     * Network constraint — sync requires connectivity.
     */
    private val networkConstraint = Constraints.Builder()
        .setRequiredNetworkType(NetworkType.CONNECTED)
        .build()

    /**
     * Enqueue immediate one-time sync.
     * Uses ExistingWorkPolicy.KEEP to prevent duplicate sync requests.
     * Exponential backoff with 30-second initial delay.
     */
    override fun enqueueImmediateSync() {
        val request = OneTimeWorkRequestBuilder<SyncWorker>()
            .setConstraints(networkConstraint)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_INITIAL_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniqueWork(
            SyncWorker.WORK_NAME,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Register hourly periodic sync as fallback.
     * Uses ExistingPeriodicWorkPolicy.KEEP to not replace existing periodic work.
     * Exponential backoff with 30-second initial delay.
     */
    fun registerPeriodicSync() {
        val request = PeriodicWorkRequestBuilder<SyncWorker>(
            PERIODIC_INTERVAL_HOURS, TimeUnit.HOURS
        )
            .setConstraints(networkConstraint)
            .setBackoffCriteria(
                BackoffPolicy.EXPONENTIAL,
                BACKOFF_INITIAL_DELAY_SECONDS,
                TimeUnit.SECONDS
            )
            .build()

        workManager.enqueueUniquePeriodicWork(
            SyncWorker.PERIODIC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            request
        )
    }

    /**
     * Trigger sync on app launch.
     * Enqueues an immediate one-time sync.
     */
    fun registerAppLaunchSync() {
        enqueueImmediateSync()
    }

    companion object {
        /** Initial backoff delay for retry (seconds). */
        private const val BACKOFF_INITIAL_DELAY_SECONDS = 30L

        /** Periodic sync interval (hours). */
        private const val PERIODIC_INTERVAL_HOURS = 1L
    }
}
