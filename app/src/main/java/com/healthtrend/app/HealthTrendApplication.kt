package com.healthtrend.app

import android.Manifest
import android.app.AlarmManager
import android.app.Application
import android.content.pm.PackageManager
import android.os.Build
import androidx.hilt.work.HiltWorkerFactory
import androidx.work.Configuration
import androidx.core.content.ContextCompat
import com.healthtrend.app.data.notification.NotificationHelper
import com.healthtrend.app.data.notification.ReminderScheduler
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.sync.SyncManager
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Application class configured with Hilt and custom WorkManager initialization.
 * Implements Configuration.Provider to use HiltWorkerFactory for @HiltWorker support.
 * Default WorkManager initialization is disabled in AndroidManifest.xml.
 */
@HiltAndroidApp
class HealthTrendApplication : Application(), Configuration.Provider {

    @Inject lateinit var workerFactory: HiltWorkerFactory
    @Inject lateinit var notificationHelper: NotificationHelper
    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var appSettingsRepository: AppSettingsRepository
    @Inject lateinit var syncManager: SyncManager

    /** Application-scoped coroutine scope for one-shot initialization work. */
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onCreate() {
        super.onCreate()

        // Create notification channel (safe to call multiple times)
        notificationHelper.createNotificationChannel()

        // Schedule all active reminders based on saved settings
        applicationScope.launch {
            initializeReminders()
        }

        // Register periodic sync + trigger app launch sync (silent, background)
        syncManager.registerPeriodicSync()
        syncManager.registerAppLaunchSync()
    }

    /**
     * Custom WorkManager configuration using HiltWorkerFactory.
     * Required for @HiltWorker dependency injection into CoroutineWorker.
     */
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder()
            .setWorkerFactory(workerFactory)
            .build()

    /**
     * Read current settings and schedule all enabled reminder alarms.
     * Only schedules if globalRemindersEnabled is true.
     * Silent failure if permission not granted — alarms just won't fire.
     */
    private suspend fun initializeReminders() {
        val settings = appSettingsRepository.getSettingsOnce() ?: return
        if (!hasReminderPermissionsAndCapabilities()) return
        reminderScheduler.scheduleAllActive(settings)
    }

    private fun hasReminderPermissionsAndCapabilities(): Boolean {
        val hasNotificationPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }

        val canScheduleExact = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val alarmManager = getSystemService(ALARM_SERVICE) as AlarmManager
            alarmManager.canScheduleExactAlarms()
        } else {
            true
        }

        return hasNotificationPermission && canScheduleExact
    }
}
