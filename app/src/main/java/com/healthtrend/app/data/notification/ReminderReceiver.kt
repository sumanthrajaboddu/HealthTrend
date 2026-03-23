package com.healthtrend.app.data.notification

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.healthtrend.app.data.model.TimeSlot

/**
 * BroadcastReceiver that fires when an alarm triggers for a [TimeSlot] reminder.
 *
 * On receive:
 * 1. Extracts [TimeSlot] from intent extras
 * 2. Shows a notification via [NotificationHelper]
 * 3. Reschedules the same alarm for the next day via [NotificationScheduler]
 *
 * Lightweight — no coroutines, no database access.
 * One notification per slot, no follow-up, no batching (AC #3, #4).
 */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val slotName = intent.getStringExtra(NotificationScheduler.EXTRA_TIME_SLOT) ?: return
        val timeSlot = try {
            TimeSlot.valueOf(slotName)
        } catch (_: IllegalArgumentException) {
            return
        }

        val hour = intent.getIntExtra(NotificationScheduler.EXTRA_ALARM_HOUR, -1)
        val minute = intent.getIntExtra(NotificationScheduler.EXTRA_ALARM_MINUTE, -1)

        val appContext = context.applicationContext

        // Show the notification
        val notificationHelper = NotificationHelper(appContext)
        try {
            notificationHelper.showReminderNotification(timeSlot)
        } catch (_: SecurityException) {
            // Silent failure by design when notification permission is unavailable.
        }

        // Reschedule for next day (daily repeating pattern)
        if (hour >= 0 && minute >= 0) {
            val scheduler = NotificationScheduler(appContext)
            try {
                scheduler.scheduleAlarm(timeSlot, hour, minute)
            } catch (_: SecurityException) {
                // Silent failure by design when exact alarm capability is unavailable.
            }
        }
    }
}
