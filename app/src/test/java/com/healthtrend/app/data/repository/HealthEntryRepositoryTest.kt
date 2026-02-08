package com.healthtrend.app.data.repository

import com.healthtrend.app.data.local.HealthEntryDao
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.sync.FakeSyncTrigger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

class HealthEntryRepositoryTest {

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
    fun `getEntriesByDate delegates to dao`() = runTest {
        val date = "2026-02-07"
        val entry = createEntry(date, TimeSlot.MORNING, Severity.MILD)
        fakeDao.entries.add(entry)

        val result = repository.getEntriesByDate(date).first()
        assertEquals(1, result.size)
        assertEquals(date, result[0].date)
    }

    @Test
    fun `insert delegates to dao and returns id`() = runTest {
        val entry = createEntry("2026-02-07", TimeSlot.MORNING, Severity.MILD)
        val id = repository.insert(entry)
        assertEquals(1L, id)
        assertEquals(1, fakeDao.entries.size)
    }

    @Test
    fun `upsert delegates to dao`() = runTest {
        val entry = createEntry("2026-02-07", TimeSlot.MORNING, Severity.MILD)
        repository.upsert(entry)
        assertEquals(1, fakeDao.entries.size)
    }

    @Test
    fun `getEntry delegates to dao`() = runTest {
        val entry = createEntry("2026-02-07", TimeSlot.MORNING, Severity.MILD)
        fakeDao.entries.add(entry)

        val result = repository.getEntry("2026-02-07", TimeSlot.MORNING.name)
        assertNotNull(result)
        assertEquals(Severity.MILD, result?.severity)
    }

    @Test
    fun `getEntry returns null when not found`() = runTest {
        val result = repository.getEntry("2026-02-07", TimeSlot.MORNING.name)
        assertNull(result)
    }

    @Test
    fun `markSynced delegates to dao`() = runTest {
        val entry = createEntry("2026-02-07", TimeSlot.MORNING, Severity.MILD, id = 1L)
        fakeDao.entries.add(entry)
        repository.markSynced(1L)
        assertEquals(true, fakeDao.entries.find { it.id == 1L }?.synced)
    }

    private fun createEntry(
        date: String,
        timeSlot: TimeSlot,
        severity: Severity,
        id: Long = 0L
    ) = HealthEntry(
        id = id,
        date = date,
        timeSlot = timeSlot,
        severity = severity
    )

    /**
     * Fake DAO for unit testing the repository layer.
     */
    private class FakeHealthEntryDao : HealthEntryDao {
        val entries = mutableListOf<HealthEntry>()
        private var nextId = 1L

        override fun getEntriesByDate(date: String): Flow<List<HealthEntry>> =
            flowOf(entries.filter { it.date == date })

        override fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HealthEntry>> =
            flowOf(entries.filter { it.date in startDate..endDate })

        override fun getUnsyncedEntries(): Flow<List<HealthEntry>> =
            flowOf(entries.filter { !it.synced })

        override suspend fun getEntry(date: String, timeSlot: String): HealthEntry? =
            entries.find { it.date == date && it.timeSlot.name == timeSlot }

        override suspend fun insert(entry: HealthEntry): Long {
            val withId = entry.copy(id = nextId++)
            entries.add(withId)
            return withId.id
        }

        override suspend fun update(entry: HealthEntry) {
            val index = entries.indexOfFirst { it.id == entry.id }
            if (index >= 0) entries[index] = entry
        }

        override suspend fun upsert(entry: HealthEntry) {
            val existing = entries.indexOfFirst {
                it.date == entry.date && it.timeSlot == entry.timeSlot
            }
            if (existing >= 0) {
                entries[existing] = entry.copy(id = entries[existing].id)
            } else {
                entries.add(entry.copy(id = nextId++))
            }
        }

        override suspend fun getAllEntries(): List<HealthEntry> = entries.toList()

        override suspend fun getUnsyncedEntriesOnce(): List<HealthEntry> =
            entries.filter { !it.synced }

        override suspend fun markSynced(id: Long) {
            val index = entries.indexOfFirst { it.id == id }
            if (index >= 0) entries[index] = entries[index].copy(synced = true)
        }

        override suspend fun deleteAll() = entries.clear()
    }
}
