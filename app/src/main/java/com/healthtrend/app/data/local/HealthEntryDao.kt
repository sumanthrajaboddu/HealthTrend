package com.healthtrend.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.healthtrend.app.data.model.HealthEntry
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for health entries.
 * Flow queries for observable data; suspend functions for one-shot operations.
 */
@Dao
interface HealthEntryDao {

    /**
     * Observe all entries for a given date.
     */
    @Query("SELECT * FROM health_entries WHERE date = :date ORDER BY time_slot ASC")
    fun getEntriesByDate(date: String): Flow<List<HealthEntry>>

    /**
     * Observe all entries between two dates (inclusive).
     */
    @Query("SELECT * FROM health_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC, time_slot ASC")
    fun getEntriesBetweenDates(startDate: String, endDate: String): Flow<List<HealthEntry>>

    /**
     * Observe all unsynced entries.
     */
    @Query("SELECT * FROM health_entries WHERE is_synced = 0")
    fun getUnsyncedEntries(): Flow<List<HealthEntry>>

    /**
     * Get a single entry by date and time slot (one-shot).
     */
    @Query("SELECT * FROM health_entries WHERE date = :date AND time_slot = :timeSlot LIMIT 1")
    suspend fun getEntry(date: String, timeSlot: String): HealthEntry?

    /**
     * Insert a new entry. Returns the row ID.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: HealthEntry): Long

    /**
     * Update an existing entry.
     */
    @Update
    suspend fun update(entry: HealthEntry)

    /**
     * Upsert (insert or update) an entry.
     */
    @Upsert
    suspend fun upsert(entry: HealthEntry)

    /**
     * Get all entries (one-shot, for sync operations).
     */
    @Query("SELECT * FROM health_entries")
    suspend fun getAllEntries(): List<HealthEntry>

    /**
     * Get all unsynced entries (one-shot, for sync push phase).
     */
    @Query("SELECT * FROM health_entries WHERE is_synced = 0")
    suspend fun getUnsyncedEntriesOnce(): List<HealthEntry>

    /**
     * Mark an entry as synced.
     */
    @Query("UPDATE health_entries SET is_synced = 1 WHERE id = :id")
    suspend fun markSynced(id: Long)

    /**
     * Observe distinct dates that have any entries in a date range.
     * Used for week strip data indicators — more efficient than loading full entries.
     */
    @Query("SELECT DISTINCT date FROM health_entries WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getDistinctDatesBetween(startDate: String, endDate: String): Flow<List<String>>

    /**
     * Delete all entries (for testing/reset).
     */
    @Query("DELETE FROM health_entries")
    suspend fun deleteAll()
}
