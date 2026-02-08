package com.healthtrend.app

import android.app.Application
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

@HiltAndroidApp
class HealthTrendApplication : Application() {

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
     * Read current settings and schedule all enabled reminder alarms.
     * Only schedules if globalRemindersEnabled is true.
     * Silent failure if permission not granted — alarms just won't fire.
     */
    private suspend fun initializeReminders() {
        val settings = appSettingsRepository.getSettingsOnce() ?: return
        reminderScheduler.scheduleAllActive(settings)
    }
}
