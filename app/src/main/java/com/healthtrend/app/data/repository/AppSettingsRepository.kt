package com.healthtrend.app.data.repository

import com.healthtrend.app.data.local.AppSettingsDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Repository for app settings.
 * All write functions are suspend — never launches coroutines internally.
 * ViewModels call via viewModelScope.launch { }.
 */
class AppSettingsRepository @Inject constructor(
    private val appSettingsDao: AppSettingsDao
) {
    /**
     * Observe settings (Flow — observable). Returns null if not yet initialized.
     */
    fun getSettings(): Flow<AppSettings?> =
        appSettingsDao.getSettings()

    /**
     * Ensure settings row exists. Creates default row if missing.
     */
    suspend fun ensureSettingsExist() {
        if (appSettingsDao.getSettingsOnce() == null) {
            appSettingsDao.insertOrReplace(AppSettings())
        }
    }

    /**
     * Update patient name.
     */
    suspend fun updatePatientName(name: String) {
        ensureSettingsExist()
        appSettingsDao.updatePatientName(name)
    }

    /**
     * Update Google Sheet URL.
     */
    suspend fun updateSheetUrl(url: String) {
        ensureSettingsExist()
        appSettingsDao.updateSheetUrl(url)
    }

    /**
     * Update Google account email.
     */
    suspend fun updateGoogleAccount(email: String?) {
        ensureSettingsExist()
        appSettingsDao.updateGoogleAccount(email)
    }

    /**
     * Update global reminders enabled flag.
     */
    suspend fun updateGlobalRemindersEnabled(enabled: Boolean) {
        ensureSettingsExist()
        appSettingsDao.updateGlobalRemindersEnabled(enabled)
    }

    /**
     * Update per-slot reminder enabled flag.
     * Reads current settings, updates the specific slot, writes back.
     */
    suspend fun updateSlotReminderEnabled(timeSlot: TimeSlot, enabled: Boolean) {
        ensureSettingsExist()
        val current = appSettingsDao.getSettingsOnce() ?: return
        val updated = when (timeSlot) {
            TimeSlot.MORNING -> current.copy(morningReminderEnabled = enabled)
            TimeSlot.AFTERNOON -> current.copy(afternoonReminderEnabled = enabled)
            TimeSlot.EVENING -> current.copy(eveningReminderEnabled = enabled)
            TimeSlot.NIGHT -> current.copy(nightReminderEnabled = enabled)
        }
        appSettingsDao.insertOrReplace(updated)
    }

    /**
     * Update per-slot reminder time.
     * Reads current settings, updates the specific slot time, writes back.
     *
     * @param timeSlot The slot to update.
     * @param time Time string in "HH:mm" format.
     */
    suspend fun updateSlotReminderTime(timeSlot: TimeSlot, time: String) {
        ensureSettingsExist()
        val current = appSettingsDao.getSettingsOnce() ?: return
        val updated = when (timeSlot) {
            TimeSlot.MORNING -> current.copy(morningReminderTime = time)
            TimeSlot.AFTERNOON -> current.copy(afternoonReminderTime = time)
            TimeSlot.EVENING -> current.copy(eveningReminderTime = time)
            TimeSlot.NIGHT -> current.copy(nightReminderTime = time)
        }
        appSettingsDao.insertOrReplace(updated)
    }

    /**
     * Get settings as a one-shot query (non-Flow, for sync worker and boot receiver).
     */
    suspend fun getSettingsOnce() = appSettingsDao.getSettingsOnce()
}
