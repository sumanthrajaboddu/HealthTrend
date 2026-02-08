package com.healthtrend.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtrend.app.data.repository.AppSettingsRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Handles BOOT_COMPLETED broadcast to re-register alarms after device restart.
 *
 * Flow: Device boots → BOOT_COMPLETED → BootReceiver.onReceive()
 *   → Read AppSettings from Room → NotificationScheduler.scheduleAllActive()
 *   → All enabled alarms re-registered with AlarmManager
 *
 * Uses [goAsync] + minimal coroutine for the Room suspend query.
 * No UI shown — lightweight background operation (AC #5).
 */
@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var reminderScheduler: ReminderScheduler
    @Inject lateinit var appSettingsRepository: AppSettingsRepository

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        val pendingResult = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val settings = appSettingsRepository.getSettingsOnce() ?: return@launch
                reminderScheduler.scheduleAllActive(settings)
            } finally {
                pendingResult.finish()
            }
        }
    }
}
