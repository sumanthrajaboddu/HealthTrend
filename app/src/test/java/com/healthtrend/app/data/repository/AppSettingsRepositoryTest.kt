package com.healthtrend.app.data.repository

import app.cash.turbine.test
import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.model.AppSettings
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for AppSettingsRepository.
 * Uses FakeAppSettingsDao for deterministic testing.
 */
class AppSettingsRepositoryTest {

    private lateinit var fakeDao: FakeAppSettingsDao
    private lateinit var repository: AppSettingsRepository

    @Before
    fun setup() {
        fakeDao = FakeAppSettingsDao()
        repository = AppSettingsRepository(fakeDao)
    }

    @Test
    fun `getSettings returns null when no settings exist`() = runTest {
        repository.getSettings().test {
            assertNull(awaitItem())
        }
    }

    @Test
    fun `ensureSettingsExist creates default row when none exists`() = runTest {
        repository.ensureSettingsExist()
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals(1, settings!!.id)
            assertEquals("", settings.patientName)
            assertEquals("", settings.sheetUrl)
        }
    }

    @Test
    fun `ensureSettingsExist does not overwrite existing settings`() = runTest {
        fakeDao.insertOrReplace(AppSettings(patientName = "Uncle"))
        repository.ensureSettingsExist()
        repository.getSettings().test {
            val settings = awaitItem()
            assertEquals("Uncle", settings!!.patientName)
        }
    }

    @Test
    fun `updatePatientName persists name`() = runTest {
        repository.updatePatientName("Uncle")
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals("Uncle", settings!!.patientName)
        }
    }

    @Test
    fun `updateSheetUrl persists url`() = runTest {
        val url = "https://docs.google.com/spreadsheets/d/abc123"
        repository.updateSheetUrl(url)
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals(url, settings!!.sheetUrl)
        }
    }

    @Test
    fun `updateGoogleAccount persists email`() = runTest {
        repository.updateGoogleAccount("raja@example.com")
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals("raja@example.com", settings!!.googleAccountEmail)
        }
    }

    @Test
    fun `updateGoogleAccount clears email when null`() = runTest {
        repository.updateGoogleAccount("raja@example.com")
        repository.updateGoogleAccount(null)
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertNull(settings!!.googleAccountEmail)
        }
    }

    @Test
    fun `updateGlobalRemindersEnabled persists flag`() = runTest {
        repository.updateGlobalRemindersEnabled(false)
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals(false, settings!!.globalRemindersEnabled)
        }
    }

    @Test
    fun `multiple updates preserve other fields`() = runTest {
        repository.updatePatientName("Uncle")
        repository.updateSheetUrl("https://sheets.google.com/test")
        repository.getSettings().test {
            val settings = awaitItem()
            assertNotNull(settings)
            assertEquals("Uncle", settings!!.patientName)
            assertEquals("https://sheets.google.com/test", settings.sheetUrl)
            assertTrue(settings.globalRemindersEnabled) // default preserved
        }
    }
}
