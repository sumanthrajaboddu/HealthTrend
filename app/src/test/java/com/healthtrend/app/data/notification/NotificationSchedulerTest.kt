package com.healthtrend.app.data.notification

import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * Unit tests for [NotificationScheduler] pure time-calculation logic.
 * Tests [calculateNextAlarmDateTime] — the testable core of alarm scheduling.
 */
class NotificationSchedulerTest {

    // ── calculateNextAlarmDateTime ──────────────────────────────────

    @Test
    fun `alarm time in future today returns today`() {
        val now = LocalDateTime.of(2026, 2, 8, 7, 0) // 7:00 AM
        val alarmTime = LocalTime.of(8, 0) // 8:00 AM

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 8, 8, 0), result)
    }

    @Test
    fun `alarm time already passed today returns tomorrow`() {
        val now = LocalDateTime.of(2026, 2, 8, 9, 0) // 9:00 AM
        val alarmTime = LocalTime.of(8, 0) // 8:00 AM

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 9, 8, 0), result)
    }

    @Test
    fun `alarm time exactly now returns tomorrow`() {
        val now = LocalDateTime.of(2026, 2, 8, 8, 0) // exactly 8:00 AM
        val alarmTime = LocalTime.of(8, 0)

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 9, 8, 0), result)
    }

    @Test
    fun `alarm at midnight when current time is 23_59 returns tomorrow`() {
        val now = LocalDateTime.of(2026, 2, 8, 23, 59)
        val alarmTime = LocalTime.of(0, 0) // midnight

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 9, 0, 0), result)
    }

    @Test
    fun `alarm at 22_00 when current time is 21_59 returns today`() {
        val now = LocalDateTime.of(2026, 2, 8, 21, 59)
        val alarmTime = LocalTime.of(22, 0)

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 8, 22, 0), result)
    }

    @Test
    fun `alarm with minutes - 7_30 AM when current is 7_00 AM returns today`() {
        val now = LocalDateTime.of(2026, 2, 8, 7, 0)
        val alarmTime = LocalTime.of(7, 30)

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 8, 7, 30), result)
    }

    @Test
    fun `alarm with minutes - 7_30 AM when current is 7_31 AM returns tomorrow`() {
        val now = LocalDateTime.of(2026, 2, 8, 7, 31)
        val alarmTime = LocalTime.of(7, 30)

        val result = calculateNextAlarmDateTime(alarmTime, now)

        assertEquals(LocalDateTime.of(2026, 2, 9, 7, 30), result)
    }

    // ── getAlarmRequestCode ────────────────────────────────────────

    @Test
    fun `each TimeSlot gets unique request code`() {
        val codes = TimeSlot.entries.map { getAlarmRequestCode(it) }.toSet()
        assertEquals(TimeSlot.entries.size, codes.size)
    }

    @Test
    fun `request codes are deterministic`() {
        val code1 = getAlarmRequestCode(TimeSlot.MORNING)
        val code2 = getAlarmRequestCode(TimeSlot.MORNING)
        assertEquals(code1, code2)
    }

    // ── getNotificationId ──────────────────────────────────────────

    @Test
    fun `each TimeSlot gets unique notification ID`() {
        val ids = TimeSlot.entries.map { getNotificationId(it) }.toSet()
        assertEquals(TimeSlot.entries.size, ids.size)
    }

    @Test
    fun `notification IDs differ from request codes`() {
        TimeSlot.entries.forEach { slot ->
            val reqCode = getAlarmRequestCode(slot)
            val notifId = getNotificationId(slot)
            assertTrue(
                "Request code and notification ID should differ for $slot",
                reqCode != notifId
            )
        }
    }

    // ── getReminderText ────────────────────────────────────────────

    @Test
    fun `reminder text uses TimeSlot displayName`() {
        TimeSlot.entries.forEach { slot ->
            val text = getReminderText(slot)
            assertTrue(
                "Reminder text should contain '${slot.displayName}'",
                text.contains(slot.displayName)
            )
        }
    }

    @Test
    fun `morning reminder text is correct`() {
        assertEquals(
            "Time to log your Morning entry",
            getReminderText(TimeSlot.MORNING)
        )
    }

    @Test
    fun `afternoon reminder text is correct`() {
        assertEquals(
            "Time to log your Afternoon entry",
            getReminderText(TimeSlot.AFTERNOON)
        )
    }

    @Test
    fun `evening reminder text is correct`() {
        assertEquals(
            "Time to log your Evening entry",
            getReminderText(TimeSlot.EVENING)
        )
    }

    @Test
    fun `night reminder text is correct`() {
        assertEquals(
            "Time to log your Night entry",
            getReminderText(TimeSlot.NIGHT)
        )
    }

    // ── getEnabledForSlot / getTimeForSlot ─────────────────────────

    @Test
    fun `getEnabledForSlot returns correct field for each slot`() {
        val settings = com.healthtrend.app.data.model.AppSettings(
            morningReminderEnabled = true,
            afternoonReminderEnabled = false,
            eveningReminderEnabled = true,
            nightReminderEnabled = false
        )
        assertEquals(true, getEnabledForSlot(settings, TimeSlot.MORNING))
        assertEquals(false, getEnabledForSlot(settings, TimeSlot.AFTERNOON))
        assertEquals(true, getEnabledForSlot(settings, TimeSlot.EVENING))
        assertEquals(false, getEnabledForSlot(settings, TimeSlot.NIGHT))
    }

    @Test
    fun `getTimeForSlot returns correct field for each slot`() {
        val settings = com.healthtrend.app.data.model.AppSettings(
            morningReminderTime = "07:30",
            afternoonReminderTime = "12:00",
            eveningReminderTime = "17:45",
            nightReminderTime = "21:00"
        )
        assertEquals("07:30", getTimeForSlot(settings, TimeSlot.MORNING))
        assertEquals("12:00", getTimeForSlot(settings, TimeSlot.AFTERNOON))
        assertEquals("17:45", getTimeForSlot(settings, TimeSlot.EVENING))
        assertEquals("21:00", getTimeForSlot(settings, TimeSlot.NIGHT))
    }

    @Test
    fun `parseAlarmTime parses HH_mm string correctly`() {
        assertEquals(LocalTime.of(8, 0), parseAlarmTime("08:00"))
        assertEquals(LocalTime.of(13, 0), parseAlarmTime("13:00"))
        assertEquals(LocalTime.of(7, 30), parseAlarmTime("07:30"))
        assertEquals(LocalTime.of(22, 0), parseAlarmTime("22:00"))
    }

    @Test
    fun `parseAlarmTime falls back to default for invalid input`() {
        // Invalid strings should fall back to midnight (00:00)
        assertEquals(LocalTime.of(0, 0), parseAlarmTime("invalid"))
        assertEquals(LocalTime.of(0, 0), parseAlarmTime(""))
    }
}
