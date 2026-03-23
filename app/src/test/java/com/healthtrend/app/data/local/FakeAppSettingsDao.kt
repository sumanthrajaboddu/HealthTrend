package com.healthtrend.app.data.local

import com.healthtrend.app.data.model.AppSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

/**
 * Fake DAO for unit testing — backed by in-memory MutableStateFlow.
 */
class FakeAppSettingsDao : AppSettingsDao {

    private val _settings = MutableStateFlow<AppSettings?>(null)

    override fun getSettings(): Flow<AppSettings?> = _settings

    override suspend fun insertOrReplace(settings: AppSettings) {
        _settings.value = settings
    }

    fun insertOrReplaceBlocking(settings: AppSettings) {
        _settings.value = settings
    }

    override suspend fun updatePatientName(name: String) {
        _settings.value = _settings.value?.copy(patientName = name)
    }

    override suspend fun updateSheetUrl(url: String) {
        _settings.value = _settings.value?.copy(sheetUrl = url)
    }

    override suspend fun updateGoogleAccount(email: String?) {
        _settings.value = _settings.value?.copy(googleAccountEmail = email)
    }

    override suspend fun updateGlobalRemindersEnabled(enabled: Boolean) {
        _settings.value = _settings.value?.copy(globalRemindersEnabled = enabled)
    }

    override suspend fun getSettingsOnce(): AppSettings? = _settings.value

    override fun getSettingsNow(): AppSettings? = _settings.value
}
