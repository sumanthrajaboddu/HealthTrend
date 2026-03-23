package com.healthtrend.app.data.local

import com.healthtrend.app.data.model.HealthEntry
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/**
 * Fake DAO for unit testing — backed by in-memory MutableStateFlow.
 */
class FakeHealthEntryDao : HealthEntryDao {

    private val _entries = MutableStateFlow<List<HealthEntry>>(emptyList())

    override fun getEntriesByDate(date: String): Flow<List<HealthEntry>> =
        _entries.map { list -> list.filter { it.date == date } }

    override fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HealthEntry>> =
        _entries.map { list -> list.filter { it.date in startDate..endDate } }

    override fun getUnsyncedEntries(): Flow<List<HealthEntry>> =
        _entries.map { list -> list.filter { !it.synced } }

    override suspend fun getEntry(date: String, timeSlot: String): HealthEntry? =
        _entries.value.find { it.date == date && it.timeSlot.name == timeSlot }

    override suspend fun insert(entry: HealthEntry): Long {
        val id = (_entries.value.maxOfOrNull { it.id } ?: 0L) + 1
        val newEntry = entry.copy(id = id)
        _entries.value = _entries.value + newEntry
        return id
    }

    override suspend fun update(entry: HealthEntry) {
        _entries.value = _entries.value.map { if (it.id == entry.id) entry else it }
    }

    override suspend fun upsert(entry: HealthEntry) {
        val existing = _entries.value.find { it.date == entry.date && it.timeSlot == entry.timeSlot }
        if (existing != null) {
            _entries.value = _entries.value.map {
                if (it.date == entry.date && it.timeSlot == entry.timeSlot) entry.copy(id = it.id) else it
            }
        } else {
            insert(entry)
        }
    }

    override fun getDistinctDatesBetween(startDate: String, endDate: String): Flow<List<String>> =
        _entries.map { list ->
            list.filter { it.date in startDate..endDate }
                .map { it.date }
                .distinct()
                .sorted()
        }

    override suspend fun getAllEntries(): List<HealthEntry> = _entries.value

    override suspend fun getUnsyncedEntriesOnce(): List<HealthEntry> =
        _entries.value.filter { !it.synced }

    override suspend fun markSynced(id: Long) {
        _entries.value = _entries.value.map { if (it.id == id) it.copy(synced = true) else it }
    }

    override suspend fun deleteAll() {
        _entries.value = emptyList()
    }
}
