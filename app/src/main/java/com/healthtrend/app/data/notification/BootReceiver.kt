package com.healthtrend.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtrend.app.data.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

/**
 * Handles BOOT_COMPLETED broadcast to re-register alarms after device restart.
 *
 * Flow: Device boots → BOOT_COMPLETED → BootReceiver.onReceive()
 *   → Read AppSettings from Room → NotificationScheduler.scheduleAllActive()
 *   → All enabled alarms re-registered with AlarmManager
 *
 * Uses [goAsync] + a lightweight background thread for the one-shot Room query.
 * No UI shown — lightweight background operation (AC #5), without coroutines.
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var bootReminderRegistrar: BootReminderRegistrar

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        Thread {
            try {
                bootReminderRegistrar.registerActiveReminders()
            } finally {
                pendingResult.finish()
            }
        }.start()
    }
}

/**
 * Boot-time alarm re-registration handler.
 * Extracted for deterministic unit testing of AC #5 boot persistence behavior.
 */
class BootReminderRegistrar @Inject constructor(
    private val reminderScheduler: ReminderScheduler,
    private val appSettingsRepository: AppSettingsRepository
) {
    fun registerActiveReminders() {
        val settings = appSettingsRepository.getSettingsNow() ?: return
        reminderScheduler.scheduleAllActive(settings)
    }
}
