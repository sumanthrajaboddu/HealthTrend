package com.healthtrend.app.data.notification

import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.repository.AppSettingsRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BootReminderRegistrarTest {

    @Test
    fun `registerActiveReminders schedules all active alarms when settings exist`() {
        val fakeDao = FakeAppSettingsDao()
        val repository = AppSettingsRepository(fakeDao)
        val scheduler = FakeReminderScheduler()
        val registrar = BootReminderRegistrar(scheduler, repository)

        fakeDao.insertOrReplaceBlocking(AppSettings(globalRemindersEnabled = true))
        registrar.registerActiveReminders()

        assertEquals(4, scheduler.scheduledAlarms.size)
        assertEquals(0, scheduler.cancelAllCount)
    }

    @Test
    fun `registerActiveReminders does nothing when no settings row exists`() {
        val fakeDao = FakeAppSettingsDao()
        val repository = AppSettingsRepository(fakeDao)
        val scheduler = FakeReminderScheduler()
        val registrar = BootReminderRegistrar(scheduler, repository)

        registrar.registerActiveReminders()

        assertNull(scheduler.lastScheduleAllSettings)
        assertEquals(0, scheduler.scheduledAlarms.size)
        assertEquals(0, scheduler.cancelAllCount)
    }
}
