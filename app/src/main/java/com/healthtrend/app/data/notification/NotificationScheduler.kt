package com.healthtrend.app.data.notification

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Interface for alarm scheduling operations.
 * Enables testing with [FakeReminderScheduler] — follows the same pattern as [GoogleAuthClient].
 */
interface ReminderScheduler {
    fun scheduleAlarm(timeSlot: TimeSlot, hour: Int, minute: Int)
    fun cancelAlarm(timeSlot: TimeSlot)
    fun scheduleAllActive(settings: AppSettings)
    fun cancelAll()
}

/**
 * Schedules and cancels exact alarms for each [TimeSlot] reminder.
 *
 * Uses [AlarmManager.setExactAndAllowWhileIdle] for precise delivery.
 * Each slot gets an independent alarm with a unique [PendingIntent].
 * If the alarm time has already passed today, schedules for tomorrow.
 *
 * Data-layer utility — injected via Hilt.
 */
@Singleton
class NotificationScheduler @Inject constructor(
    @ApplicationContext private val context: Context
) : ReminderScheduler {

    private val alarmManager: AlarmManager =
        context.getSystemService(Context.ALARM_SERVICE) as AlarmManager

    /**
     * Schedule an alarm for a specific [TimeSlot] at the given [hour]:[minute].
     * If the time has passed today, the alarm is set for tomorrow.
     */
    override fun scheduleAlarm(timeSlot: TimeSlot, hour: Int, minute: Int) {
        val alarmTime = LocalTime.of(hour, minute)
        val nextAlarm = calculateNextAlarmDateTime(alarmTime, LocalDateTime.now())
        val millis = nextAlarm.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

        val intent = createAlarmIntent(timeSlot, hour, minute)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getAlarmRequestCode(timeSlot),
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        alarmManager.setExactAndAllowWhileIdle(
            AlarmManager.RTC_WAKEUP,
            millis,
            pendingIntent
        )
    }

    /**
     * Cancel the alarm for a specific [TimeSlot].
     */
    override fun cancelAlarm(timeSlot: TimeSlot) {
        val intent = Intent(context, ReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            getAlarmRequestCode(timeSlot),
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        pendingIntent?.let { alarmManager.cancel(it) }
    }

    /**
     * Schedule all enabled alarms based on current [AppSettings].
     * Cancels all first, then schedules only the enabled slots.
     */
    override fun scheduleAllActive(settings: AppSettings) {
        if (!settings.globalRemindersEnabled) {
            cancelAll()
            return
        }

        TimeSlot.entries.forEach { slot ->
            if (getEnabledForSlot(settings, slot)) {
                val timeStr = getTimeForSlot(settings, slot)
                val time = parseAlarmTime(timeStr)
                scheduleAlarm(slot, time.hour, time.minute)
            } else {
                cancelAlarm(slot)
            }
        }
    }

    /**
     * Cancel all 4 slot alarms.
     */
    override fun cancelAll() {
        TimeSlot.entries.forEach { cancelAlarm(it) }
    }

    /**
     * Create an [Intent] targeting [ReminderReceiver] with TimeSlot and time extras.
     */
    private fun createAlarmIntent(timeSlot: TimeSlot, hour: Int, minute: Int): Intent {
        return Intent(context, ReminderReceiver::class.java).apply {
            putExtra(EXTRA_TIME_SLOT, timeSlot.name)
            putExtra(EXTRA_ALARM_HOUR, hour)
            putExtra(EXTRA_ALARM_MINUTE, minute)
        }
    }

    companion object {
        const val EXTRA_TIME_SLOT = "extra_time_slot"
        const val EXTRA_ALARM_HOUR = "extra_alarm_hour"
        const val EXTRA_ALARM_MINUTE = "extra_alarm_minute"
    }
}

// ── Pure utility functions (testable without Android) ───────────────

/**
 * Calculate the next [LocalDateTime] to fire an alarm at [alarmTime].
 * If [alarmTime] is at or before [now], schedules for tomorrow.
 *
 * Pure function — no side effects, fully unit-testable.
 */
fun calculateNextAlarmDateTime(
    alarmTime: LocalTime,
    now: LocalDateTime
): LocalDateTime {
    val todayAlarm = now.toLocalDate().atTime(alarmTime)
    return if (todayAlarm.isAfter(now)) {
        todayAlarm
    } else {
        todayAlarm.plusDays(1)
    }
}

/**
 * Unique alarm request code per [TimeSlot].
 * Uses base offset to avoid collision with other PendingIntents.
 */
fun getAlarmRequestCode(timeSlot: TimeSlot): Int =
    ALARM_REQUEST_CODE_BASE + timeSlot.ordinal

/**
 * Unique notification ID per [TimeSlot].
 * Uses different base than request codes to avoid any overlap.
 */
fun getNotificationId(timeSlot: TimeSlot): Int =
    NOTIFICATION_ID_BASE + timeSlot.ordinal

/**
 * Reminder notification text per AC #1: "Time to log your [displayName] entry".
 * Uses [TimeSlot.displayName] — NEVER hardcoded slot names.
 */
fun getReminderText(timeSlot: TimeSlot): String =
    "Time to log your ${timeSlot.displayName} entry"

/**
 * Get the per-slot enabled flag from [AppSettings].
 */
fun getEnabledForSlot(settings: AppSettings, timeSlot: TimeSlot): Boolean =
    when (timeSlot) {
        TimeSlot.MORNING -> settings.morningReminderEnabled
        TimeSlot.AFTERNOON -> settings.afternoonReminderEnabled
        TimeSlot.EVENING -> settings.eveningReminderEnabled
        TimeSlot.NIGHT -> settings.nightReminderEnabled
    }

/**
 * Get the per-slot reminder time string from [AppSettings].
 */
fun getTimeForSlot(settings: AppSettings, timeSlot: TimeSlot): String =
    when (timeSlot) {
        TimeSlot.MORNING -> settings.morningReminderTime
        TimeSlot.AFTERNOON -> settings.afternoonReminderTime
        TimeSlot.EVENING -> settings.eveningReminderTime
        TimeSlot.NIGHT -> settings.nightReminderTime
    }

/**
 * Parse "HH:mm" string to [LocalTime]. Falls back to midnight on invalid input.
 */
fun parseAlarmTime(timeStr: String): LocalTime =
    try {
        val parts = timeStr.split(":")
        LocalTime.of(parts[0].toInt(), parts[1].toInt())
    } catch (_: Exception) {
        LocalTime.of(0, 0)
    }

/** Base offset for alarm PendingIntent request codes. */
private const val ALARM_REQUEST_CODE_BASE = 4100

/** Base offset for notification IDs. */
private const val NOTIFICATION_ID_BASE = 4200
