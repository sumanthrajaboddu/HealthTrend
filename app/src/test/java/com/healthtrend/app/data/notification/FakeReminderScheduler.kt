package com.healthtrend.app.data.notification

import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot

/**
 * Fake [ReminderScheduler] for unit testing.
 * Tracks all scheduling/cancellation calls for assertions.
 */
class FakeReminderScheduler : ReminderScheduler {

    /** Alarms currently scheduled: TimeSlot → Pair(hour, minute). */
    val scheduledAlarms = mutableMapOf<TimeSlot, Pair<Int, Int>>()

    /** History of all cancelAlarm calls. */
    val cancelledSlots = mutableListOf<TimeSlot>()

    /** Number of times cancelAll was called. */
    var cancelAllCount = 0
        private set

    /** Last settings passed to scheduleAllActive. */
    var lastScheduleAllSettings: AppSettings? = null
        private set

    override fun scheduleAlarm(timeSlot: TimeSlot, hour: Int, minute: Int) {
        scheduledAlarms[timeSlot] = hour to minute
    }

    override fun cancelAlarm(timeSlot: TimeSlot) {
        cancelledSlots.add(timeSlot)
        scheduledAlarms.remove(timeSlot)
    }

    override fun scheduleAllActive(settings: AppSettings) {
        lastScheduleAllSettings = settings

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

    override fun cancelAll() {
        cancelAllCount++
        scheduledAlarms.clear()
    }

    fun reset() {
        scheduledAlarms.clear()
        cancelledSlots.clear()
        cancelAllCount = 0
        lastScheduleAllSettings = null
    }
}
