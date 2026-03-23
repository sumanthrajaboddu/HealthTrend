package com.healthtrend.app.data.sync

import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.repository.AppSettingsRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SheetCreationRetrier — app-launch retry logic (Story 3.4 AC #4).
 */
class SheetCreationRetrierTest {

    private lateinit var fakeDao: FakeAppSettingsDao
    private lateinit var repository: AppSettingsRepository
    private lateinit var fakeSheetsClient: FakeSheetsClient
    private lateinit var retrier: SheetCreationRetrier

    @Before
    fun setup() {
        fakeDao = FakeAppSettingsDao()
        repository = AppSettingsRepository(fakeDao)
        fakeSheetsClient = FakeSheetsClient()
        retrier = SheetCreationRetrier(repository, fakeSheetsClient)
    }

    @Test
    fun `retryIfNeeded creates sheet when signed in with no URL`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))

        val result = retrier.retryIfNeeded()

        assertTrue(result)
        assertEquals(1, fakeSheetsClient.createdSheets.size)
        assertEquals("raja@example.com", fakeSheetsClient.createdSheets[0].first)
        assertEquals(SheetsClient.DEFAULT_SHEET_TITLE, fakeSheetsClient.createdSheets[0].second)
        assertEquals(fakeSheetsClient.createSheetReturnUrl, fakeDao.getSettingsOnce()?.sheetUrl)
    }

    @Test
    fun `retryIfNeeded skips when no settings exist`() = runTest {
        // No settings inserted
        val result = retrier.retryIfNeeded()

        assertFalse(result)
        assertEquals(0, fakeSheetsClient.createdSheets.size)
    }

    @Test
    fun `retryIfNeeded skips when email is empty`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "", sheetUrl = ""))

        val result = retrier.retryIfNeeded()

        assertFalse(result)
        assertEquals(0, fakeSheetsClient.createdSheets.size)
    }

    @Test
    fun `retryIfNeeded skips when email is null`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = null, sheetUrl = ""))

        val result = retrier.retryIfNeeded()

        assertFalse(result)
        assertEquals(0, fakeSheetsClient.createdSheets.size)
    }

    @Test
    fun `retryIfNeeded skips when sheetUrl already exists`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(
                googleAccountEmail = "raja@example.com",
                sheetUrl = "https://docs.google.com/spreadsheets/d/existing"
            )
        )

        val result = retrier.retryIfNeeded()

        assertFalse(result)
        assertEquals(0, fakeSheetsClient.createdSheets.size)
    }

    @Test
    fun `retryIfNeeded saves URL to repository on success`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))

        retrier.retryIfNeeded()

        val saved = fakeDao.getSettingsOnce()
        assertEquals(fakeSheetsClient.createSheetReturnUrl, saved?.sheetUrl)
    }

    @Test
    fun `retryIfNeeded throws on sheet creation failure`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))
        fakeSheetsClient.createSheetShouldFail = true

        try {
            retrier.retryIfNeeded()
            fail("Expected exception from createSheet failure")
        } catch (e: RuntimeException) {
            assertEquals("Simulated createSheet failure", e.message)
        }
    }

    @Test
    fun `retryIfNeeded uses DEFAULT_SHEET_TITLE constant`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))

        retrier.retryIfNeeded()

        assertEquals("HealthTrend", fakeSheetsClient.createdSheets[0].second)
    }

    // ── Cross-device Sheet Reuse: findSheet before createSheet ────

    @Test
    fun `retryIfNeeded reuses existing sheet found via findSheet`() = runTest {
        val existingDriveUrl = "https://docs.google.com/spreadsheets/d/drive-found-id"
        fakeSheetsClient.findSheetReturnUrl = existingDriveUrl
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))

        val result = retrier.retryIfNeeded()

        assertTrue(result)
        // findSheet was called
        assertEquals(1, fakeSheetsClient.findSheetCalls.size)
        assertEquals("raja@example.com", fakeSheetsClient.findSheetCalls[0].first)
        assertEquals(SheetsClient.DEFAULT_SHEET_TITLE, fakeSheetsClient.findSheetCalls[0].second)
        // No createSheet call — reused existing sheet
        assertEquals(0, fakeSheetsClient.createdSheets.size)
        // URL from findSheet saved to repository
        assertEquals(existingDriveUrl, fakeDao.getSettingsOnce()?.sheetUrl)
    }

    @Test
    fun `retryIfNeeded calls findSheet then createSheet when not found`() = runTest {
        fakeSheetsClient.findSheetReturnUrl = null // no existing sheet
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))

        val result = retrier.retryIfNeeded()

        assertTrue(result)
        // findSheet was called first
        assertEquals(1, fakeSheetsClient.findSheetCalls.size)
        // Then createSheet was called
        assertEquals(1, fakeSheetsClient.createdSheets.size)
        assertEquals(fakeSheetsClient.createSheetReturnUrl, fakeDao.getSettingsOnce()?.sheetUrl)
    }

    @Test
    fun `retryIfNeeded throws on findSheet failure`() = runTest {
        fakeDao.insertOrReplace(AppSettings(googleAccountEmail = "raja@example.com", sheetUrl = ""))
        fakeSheetsClient.findSheetShouldFail = true

        try {
            retrier.retryIfNeeded()
            fail("Expected exception from findSheet failure")
        } catch (e: RuntimeException) {
            assertEquals("Simulated findSheet failure", e.message)
        }
    }

    @Test
    fun `retryIfNeeded does not call findSheet when sheetUrl already exists`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(
                googleAccountEmail = "raja@example.com",
                sheetUrl = "https://docs.google.com/spreadsheets/d/existing"
            )
        )

        val result = retrier.retryIfNeeded()

        assertFalse(result)
        assertEquals(0, fakeSheetsClient.findSheetCalls.size)
        assertEquals(0, fakeSheetsClient.createdSheets.size)
    }
}
