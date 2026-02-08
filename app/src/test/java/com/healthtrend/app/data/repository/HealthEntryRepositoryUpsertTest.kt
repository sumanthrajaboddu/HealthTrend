package com.healthtrend.app.data.repository

import com.healthtrend.app.data.local.FakeHealthEntryDao
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.sync.FakeSyncTrigger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test

/**
 * Tests for HealthEntryRepository.upsertEntry() — Story 1.3 persistence logic.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class HealthEntryRepositoryUpsertTest {

    private lateinit var fakeDao: FakeHealthEntryDao
    private lateinit var repository: HealthEntryRepository
    private lateinit var fakeSyncTrigger: FakeSyncTrigger

    @Before
    fun setup() {
        fakeDao = FakeHealthEntryDao()
        fakeSyncTrigger = FakeSyncTrigger()
        repository = HealthEntryRepository(fakeDao, fakeSyncTrigger)
    }

    @Test
    fun `upsertEntry inserts new entry when none exists`() = runTest {
        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.MILD)

        val entry = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(entry)
        assertEquals(Severity.MILD, entry!!.severity)
        assertEquals(TimeSlot.MORNING, entry.timeSlot)
        assertEquals("2026-02-08", entry.date)
        assertFalse(entry.synced)
    }

    @Test
    fun `upsertEntry updates existing entry severity`() = runTest {
        fakeDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD)
        )

        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.SEVERE)

        val entry = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(entry)
        assertEquals(Severity.SEVERE, entry!!.severity)
    }

    @Test
    fun `upsertEntry sets synced to false on update`() = runTest {
        val id = fakeDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD)
        )
        fakeDao.markSynced(id)

        // Verify it was synced
        val synced = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(synced)
        assertEquals(true, synced!!.synced)

        // Now upsert — should reset synced
        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.MODERATE)

        val updated = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(updated)
        assertFalse(updated!!.synced)
    }

    @Test
    fun `upsertEntry preserves entry id on update`() = runTest {
        val originalId = fakeDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD)
        )

        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.SEVERE)

        val updated = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(updated)
        assertEquals(originalId, updated!!.id)
    }

    @Test
    fun `upsertEntry works for same date different time slots`() = runTest {
        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.MILD)
        repository.upsertEntry("2026-02-08", TimeSlot.EVENING, Severity.SEVERE)

        val morning = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        val evening = fakeDao.getEntry("2026-02-08", TimeSlot.EVENING.name)

        assertEquals(Severity.MILD, morning!!.severity)
        assertEquals(Severity.SEVERE, evening!!.severity)
    }

    @Test
    fun `upsertEntry updates updatedAt timestamp`() = runTest {
        val before = System.currentTimeMillis()
        repository.upsertEntry("2026-02-08", TimeSlot.MORNING, Severity.MILD)
        val after = System.currentTimeMillis()

        val entry = fakeDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(entry)
        assert(entry!!.updatedAt in before..after)
    }
}
