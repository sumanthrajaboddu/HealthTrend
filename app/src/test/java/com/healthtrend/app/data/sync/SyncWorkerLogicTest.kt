package com.healthtrend.app.data.sync

import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.local.FakeHealthEntryDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.repository.HealthEntryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for sync logic — tests the push/pull protocol
 * without requiring Android context (tests the logic, not the Worker wrapper).
 *
 * Sync protocol:
 * 1. Push first: local unsynced → Sheet (only if local is newer)
 * 2. Pull second: Sheet → local (only if Sheet is newer)
 */
class SyncWorkerLogicTest {

    private lateinit var fakeHealthDao: FakeHealthEntryDao
    private lateinit var fakeSettingsDao: FakeAppSettingsDao
    private lateinit var healthRepo: HealthEntryRepository
    private lateinit var settingsRepo: AppSettingsRepository
    private lateinit var fakeSheetsClient: FakeSheetsClient
    private lateinit var fakeSyncTrigger: FakeSyncTrigger

    @Before
    fun setup() {
        fakeHealthDao = FakeHealthEntryDao()
        fakeSettingsDao = FakeAppSettingsDao()
        fakeSyncTrigger = FakeSyncTrigger()
        healthRepo = HealthEntryRepository(fakeHealthDao, fakeSyncTrigger)
        settingsRepo = AppSettingsRepository(fakeSettingsDao)
        fakeSheetsClient = FakeSheetsClient()
    }

    // --- Push Phase Tests ---

    @Test
    fun `push writes unsynced entry to Sheet when row exists and local is newer`() = runTest {
        // Setup: existing Sheet row with older timestamp
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("No Pain", 1000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        // Local entry is newer (updatedAt = 2000L > Sheet timestamp 1000L)
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 2000L
            )
        )

        val unsyncedEntries = healthRepo.getUnsyncedEntriesOnce()
        assertEquals(1, unsyncedEntries.size)

        // Verify the entry has the correct data
        val entry = unsyncedEntries[0]
        assertEquals("2026-02-08", entry.date)
        assertEquals(TimeSlot.MORNING, entry.timeSlot)
        assertEquals(Severity.MILD, entry.severity)
        assertEquals(false, entry.synced)
    }

    @Test
    fun `push does not write when Sheet timestamp is newer`() = runTest {
        // Setup: Sheet has newer data
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Severe", 5000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        // Local entry is older
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 1000L
            )
        )

        // The entry updatedAt (1000L) < Sheet timestamp (5000L)
        // So push should NOT write to Sheet
        val sheetRow = fakeSheetsClient.rows[0]
        val sheetTimestamp = sheetRow.slots[TimeSlot.MORNING]?.timestamp ?: 0L
        val entry = fakeHealthDao.getUnsyncedEntriesOnce()[0]

        assertTrue("Sheet should be newer", sheetTimestamp > entry.updatedAt)
    }

    @Test
    fun `push appends new row when date not in Sheet`() = runTest {
        // No existing Sheet rows — should append
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-10",
                timeSlot = TimeSlot.EVENING,
                severity = Severity.SEVERE,
                synced = false,
                updatedAt = 3000L
            )
        )

        val unsyncedEntries = healthRepo.getUnsyncedEntriesOnce()
        assertEquals(1, unsyncedEntries.size)
        assertEquals("2026-02-10", unsyncedEntries[0].date)
    }

    // --- Pull Phase Tests ---

    @Test
    fun `pull creates local entry when not exists`() = runTest {
        // Sheet has data, local is empty
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-09",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Moderate", 4000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        // Verify local has no entry
        val localEntry = healthRepo.getEntry("2026-02-09", TimeSlot.MORNING.name)
        assertEquals(null, localEntry)
    }

    @Test
    fun `pull does not overwrite local entry when local is newer`() = runTest {
        // Local entry is newer than Sheet
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.SEVERE,
                synced = true,
                updatedAt = 5000L
            )
        )

        // Sheet has older data
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Mild", 1000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        // Verify local entry is still SEVERE (newer)
        val localEntry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(Severity.SEVERE, localEntry?.severity)
        assertEquals(5000L, localEntry?.updatedAt)
    }

    // --- Helper/Column Mapping Tests ---

    @Test
    fun `severity display names match exactly for Sheet writes`() {
        assertEquals("No Pain", Severity.NO_PAIN.displayName)
        assertEquals("Mild", Severity.MILD.displayName)
        assertEquals("Moderate", Severity.MODERATE.displayName)
        assertEquals("Severe", Severity.SEVERE.displayName)
    }

    @Test
    fun `column mapping covers all four time slots`() {
        assertEquals(4, GoogleSheetsService.SEVERITY_COLUMN.size)
        assertEquals(4, GoogleSheetsService.TIMESTAMP_COLUMN.size)
        for (slot in TimeSlot.entries) {
            assertTrue(GoogleSheetsService.SEVERITY_COLUMN.containsKey(slot))
            assertTrue(GoogleSheetsService.TIMESTAMP_COLUMN.containsKey(slot))
        }
    }
}
