package com.healthtrend.app.data.sync

import com.healthtrend.app.data.local.FakeHealthEntryDao
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.HealthEntryRepository
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SyncLogic — tests the push/pull protocol
 * through the extracted SyncLogic class (no Android context required).
 *
 * Sync protocol:
 * 1. Push first: local unsynced → Sheet (only if local is newer)
 * 2. Pull second: Sheet → local (only if Sheet is newer)
 */
class SyncWorkerLogicTest {

    private lateinit var fakeHealthDao: FakeHealthEntryDao
    private lateinit var healthRepo: HealthEntryRepository
    private lateinit var fakeSheetsClient: FakeSheetsClient
    private lateinit var fakeSyncTrigger: FakeSyncTrigger
    private lateinit var syncLogic: SyncLogic

    private val testSheetUrl = "https://docs.google.com/spreadsheets/d/test123/edit"
    private val testEmail = "test@gmail.com"

    @Before
    fun setup() {
        fakeHealthDao = FakeHealthEntryDao()
        fakeSyncTrigger = FakeSyncTrigger()
        healthRepo = HealthEntryRepository(fakeHealthDao, fakeSyncTrigger)
        fakeSheetsClient = FakeSheetsClient()
        syncLogic = SyncLogic(healthRepo, fakeSheetsClient)
    }

    // ===================================================================
    // Push Phase Tests
    // ===================================================================

    @Test
    fun `push writes to Sheet when local entry is newer than Sheet`() = runTest {
        // Sheet has older data (timestamp 1000L)
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

        // Local entry is newer (updatedAt = 2000L > Sheet 1000L)
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 2000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Verify two cell writes: severity + timestamp
        assertEquals(2, fakeSheetsClient.cellWrites.size)
        assertEquals("B2" to "Mild", fakeSheetsClient.cellWrites[0])
        assertEquals("F2" to 2000L, fakeSheetsClient.cellWrites[1])

        // Verify entry marked synced
        val entry = fakeHealthDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertTrue(entry!!.synced)
    }

    @Test
    fun `push does NOT write when Sheet timestamp is newer`() = runTest {
        // Sheet has newer data (timestamp 5000L)
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

        // Local entry is older (updatedAt = 1000L < Sheet 5000L)
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 1000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Verify NO severity/timestamp writes happened (only pull reads existing data)
        assertEquals(0, fakeSheetsClient.cellWrites.size)

        // Entry should NOT be marked synced (pull phase will update it)
        // After pull: local was updated from Sheet because Sheet is newer
        val entry = fakeHealthDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertNotNull(entry)
        assertEquals(Severity.SEVERE, entry!!.severity)
        assertEquals(5000L, entry.updatedAt)
        assertTrue(entry.synced)
    }

    @Test
    fun `push marks synced without writing when timestamps are equal`() = runTest {
        // Sheet and local have same timestamp
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Mild", 2000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 2000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // No writes — timestamps are equal
        assertEquals(0, fakeSheetsClient.cellWrites.size)

        // Entry marked synced (already in sync)
        val entry = fakeHealthDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertTrue(entry!!.synced)
    }

    @Test
    fun `push creates new row via cell-level writes when date not in Sheet`() = runTest {
        // No existing Sheet rows
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-10",
                timeSlot = TimeSlot.EVENING,
                severity = Severity.SEVERE,
                synced = false,
                updatedAt = 3000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Should write 3 cells: date (A2), severity (D2), timestamp (H2)
        assertEquals(3, fakeSheetsClient.cellWrites.size)
        assertEquals("A2" to "2026-02-10", fakeSheetsClient.cellWrites[0])
        assertEquals("D2" to "Severe", fakeSheetsClient.cellWrites[1])
        assertEquals("H2" to 3000L, fakeSheetsClient.cellWrites[2])

        // No nulls sent via appendRow
        assertEquals(0, fakeSheetsClient.appendedRows.size)

        // Entry marked synced
        val entry = fakeHealthDao.getEntry("2026-02-10", TimeSlot.EVENING.name)
        assertTrue(entry!!.synced)
    }

    @Test
    fun `push new row computes correct row number after existing rows`() = runTest {
        // Sheet has rows at indices 1,2,3 (header is 0)
        fakeSheetsClient.rows.addAll(
            listOf(
                SheetRow("2026-02-08", 1, mapOf(TimeSlot.MORNING to SheetSlotData("Mild", 100L), TimeSlot.AFTERNOON to null, TimeSlot.EVENING to null, TimeSlot.NIGHT to null)),
                SheetRow("2026-02-09", 2, mapOf(TimeSlot.MORNING to null, TimeSlot.AFTERNOON to null, TimeSlot.EVENING to null, TimeSlot.NIGHT to null)),
                SheetRow("2026-02-10", 3, mapOf(TimeSlot.MORNING to null, TimeSlot.AFTERNOON to null, TimeSlot.EVENING to null, TimeSlot.NIGHT to null))
            )
        )

        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-11",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = false,
                updatedAt = 4000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Next row: maxRowIndex(3) + 2 = 5 in A1 notation
        assertEquals("A5" to "2026-02-11", fakeSheetsClient.cellWrites[0])
        assertEquals("B5" to "Mild", fakeSheetsClient.cellWrites[1])
        assertEquals("F5" to 4000L, fakeSheetsClient.cellWrites[2])
    }

    @Test
    fun `push skips entries that are already synced`() = runTest {
        // Only unsynced entries should be pushed
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = true, // Already synced
                updatedAt = 2000L
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // No writes — entry was already synced
        assertEquals(0, fakeSheetsClient.cellWrites.size)
    }

    // ===================================================================
    // Pull Phase Tests
    // ===================================================================

    @Test
    fun `pull creates local entry when date+slot not exists locally`() = runTest {
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

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Verify local entry was created from Sheet data
        val entry = healthRepo.getEntry("2026-02-09", TimeSlot.MORNING.name)
        assertNotNull(entry)
        assertEquals(Severity.MODERATE, entry!!.severity)
        assertEquals(4000L, entry.updatedAt)
        assertTrue(entry.synced)
    }

    @Test
    fun `pull updates local entry when Sheet is newer`() = runTest {
        // Local entry with older timestamp
        fakeHealthDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD,
                synced = true,
                updatedAt = 1000L
            )
        )

        // Sheet has newer data
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

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Verify local was updated
        val entry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(Severity.SEVERE, entry!!.severity)
        assertEquals(5000L, entry.updatedAt)
        assertTrue(entry.synced)
    }

    @Test
    fun `pull does NOT overwrite local when local is newer`() = runTest {
        // Local entry with newer timestamp
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

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Verify local was NOT changed
        val entry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(Severity.SEVERE, entry!!.severity)
        assertEquals(5000L, entry.updatedAt)
    }

    @Test
    fun `pull skips Sheet cells with zero timestamp`() = runTest {
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Mild", 0L), // Invalid timestamp
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Should NOT create local entry for invalid timestamp
        val entry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(null, entry)
    }

    @Test
    fun `pull skips Sheet cells with unknown severity`() = runTest {
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Unknown", 4000L), // Bad severity
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Should NOT create local entry for unrecognized severity
        val entry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(null, entry)
    }

    // ===================================================================
    // Integration / Idempotency Tests
    // ===================================================================

    @Test
    fun `full sync is idempotent — duplicate runs produce same state`() = runTest {
        fakeSheetsClient.rows.add(
            SheetRow(
                date = "2026-02-08",
                rowIndex = 1,
                slots = mapOf(
                    TimeSlot.MORNING to SheetSlotData("Moderate", 3000L),
                    TimeSlot.AFTERNOON to null,
                    TimeSlot.EVENING to null,
                    TimeSlot.NIGHT to null
                )
            )
        )

        // Run sync twice
        syncLogic.executeSync(testSheetUrl, testEmail)
        fakeSheetsClient.cellWrites.clear() // Reset write tracking
        syncLogic.executeSync(testSheetUrl, testEmail)

        // No writes on second run — data already in sync
        assertEquals(0, fakeSheetsClient.cellWrites.size)

        // Local data correct
        val entry = healthRepo.getEntry("2026-02-08", TimeSlot.MORNING.name)
        assertEquals(Severity.MODERATE, entry!!.severity)
        assertEquals(3000L, entry.updatedAt)
        assertTrue(entry.synced)
    }

    @Test
    fun `sync with no unsynced entries and no Sheet data does nothing`() = runTest {
        syncLogic.executeSync(testSheetUrl, testEmail)

        assertEquals(0, fakeSheetsClient.cellWrites.size)
        assertEquals(0, fakeSheetsClient.appendedRows.size)
    }

    @Test
    fun `push multiple unsynced entries processes all of them`() = runTest {
        // Two unsynced entries for different dates
        fakeHealthDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD, synced = false, updatedAt = 2000L)
        )
        fakeHealthDao.insert(
            HealthEntry(date = "2026-02-09", timeSlot = TimeSlot.EVENING, severity = Severity.SEVERE, synced = false, updatedAt = 3000L)
        )

        syncLogic.executeSync(testSheetUrl, testEmail)

        // Both entries should be pushed (new rows: 3 cell writes each = 6 total)
        assertEquals(6, fakeSheetsClient.cellWrites.size)

        // Both marked synced
        val entry1 = fakeHealthDao.getEntry("2026-02-08", TimeSlot.MORNING.name)
        val entry2 = fakeHealthDao.getEntry("2026-02-09", TimeSlot.EVENING.name)
        assertTrue(entry1!!.synced)
        assertTrue(entry2!!.synced)
    }

    // ===================================================================
    // Error Handling Tests
    // ===================================================================

    @Test(expected = RuntimeException::class)
    fun `API failure propagates as exception for Result_retry`() = runTest {
        fakeSheetsClient.shouldFail = true

        fakeHealthDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD, synced = false, updatedAt = 2000L)
        )

        // Should throw — SyncWorker catches this and returns Result.retry()
        syncLogic.executeSync(testSheetUrl, testEmail)
    }

    // ===================================================================
    // Helper/Column Mapping Tests
    // ===================================================================

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
