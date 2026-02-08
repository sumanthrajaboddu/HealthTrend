package com.healthtrend.app.data.repository

import com.healthtrend.app.data.local.HealthEntryDao
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.sync.SyncTrigger
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository for health entries.
 * All functions are suspend — never launches coroutines internally.
 * ViewModels call via viewModelScope.launch { }.
 */
class HealthEntryRepository @Inject constructor(
    private val healthEntryDao: HealthEntryDao,
    private val syncTrigger: SyncTrigger
) {
    /**
     * Observe entries for a given date (Flow — observable).
     */
    fun getEntriesByDate(date: String): Flow<List<HealthEntry>> =
        healthEntryDao.getEntriesByDate(date)

    /**
     * Observe entries between two dates (Flow — observable).
     */
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HealthEntry>> =
        healthEntryDao.getEntriesBetweenDates(startDate, endDate)

    /**
     * Observe unsynced entries (Flow — observable).
     */
    fun getUnsyncedEntries(): Flow<List<HealthEntry>> =
        healthEntryDao.getUnsyncedEntries()

    /**
     * Get a single entry by date and time slot (one-shot).
     */
    suspend fun getEntry(date: String, timeSlot: String): HealthEntry? =
        healthEntryDao.getEntry(date, timeSlot)

    /**
     * Insert a new entry. Returns the row ID.
     */
    suspend fun insert(entry: HealthEntry): Long =
        healthEntryDao.insert(entry)

    /**
     * Update an existing entry.
     */
    suspend fun update(entry: HealthEntry) =
        healthEntryDao.update(entry)

    /**
     * Upsert (insert or update) an entry.
     */
    suspend fun upsert(entry: HealthEntry) =
        healthEntryDao.upsert(entry)

    /**
     * Get all entries (one-shot, for sync operations).
     */
    suspend fun getAllEntries(): List<HealthEntry> =
        healthEntryDao.getAllEntries()

    /**
     * Get all unsynced entries (one-shot, for sync push phase).
     */
    suspend fun getUnsyncedEntriesOnce(): List<HealthEntry> =
        healthEntryDao.getUnsyncedEntriesOnce()

    /**
     * Mark an entry as synced.
     */
    suspend fun markSynced(id: Long) =
        healthEntryDao.markSynced(id)

    /**
     * Observe which dates in a range have any logged entries.
     * Returns distinct date strings (ISO format). Used for week strip data indicators.
     */
    fun getDatesWithEntries(startDate: String, endDate: String): Flow<List<String>> =
        healthEntryDao.getDistinctDatesBetween(startDate, endDate)

    /**
     * Upsert an entry by date and time slot.
     * Creates a new entry or updates an existing one.
     * Always sets synced = false and updatedAt = System.currentTimeMillis().
     * Uses composite key (date, timeSlot) for lookup.
     * After save, enqueues immediate sync (silent, background).
     */
    suspend fun upsertEntry(date: String, timeSlot: TimeSlot, severity: Severity) {
        val existing = healthEntryDao.getEntry(date, timeSlot.name)
        if (existing != null) {
            healthEntryDao.update(
                existing.copy(
                    severity = severity,
                    synced = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        } else {
            healthEntryDao.insert(
                HealthEntry(
                    date = date,
                    timeSlot = timeSlot,
                    severity = severity,
                    synced = false,
                    updatedAt = System.currentTimeMillis()
                )
            )
        }

        // Trigger sync after local save — silent, background
        syncTrigger.enqueueImmediateSync()
    }
}
