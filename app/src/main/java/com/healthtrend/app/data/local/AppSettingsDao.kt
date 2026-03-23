package com.healthtrend.app.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.healthtrend.app.data.model.AppSettings
import kotlinx.coroutines.flow.Flow

/**
 * Data Access Object for app settings.
 * Single-row table (id = 1 always).
 * Flow query for observable reads; suspend functions for writes.
 */
@Dao
interface AppSettingsDao {

    /**
     * Observe the settings row. Returns null if not yet created.
     */
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<AppSettings?>

    /**
     * Insert or replace settings row.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplace(settings: AppSettings)

    /**
     * Update patient name.
     */
    @Query("UPDATE app_settings SET patient_name = :name WHERE id = 1")
    suspend fun updatePatientName(name: String)

    /**
     * Update Google Sheet URL.
     */
    @Query("UPDATE app_settings SET sheet_url = :url WHERE id = 1")
    suspend fun updateSheetUrl(url: String)

    /**
     * Update Google account email.
     */
    @Query("UPDATE app_settings SET google_account_email = :email WHERE id = 1")
    suspend fun updateGoogleAccount(email: String?)

    /**
     * Update global reminders enabled flag.
     */
    @Query("UPDATE app_settings SET global_reminders_enabled = :enabled WHERE id = 1")
    suspend fun updateGlobalRemindersEnabled(enabled: Boolean)

    /**
     * Get settings as a one-shot (non-observable) query.
     */
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsOnce(): AppSettings?

    /**
     * Synchronous one-shot query for lightweight boot-time receivers.
     * Used from a background thread (never on main thread).
     */
    @Query("SELECT * FROM app_settings WHERE id = 1 LIMIT 1")
    fun getSettingsNow(): AppSettings?
}
