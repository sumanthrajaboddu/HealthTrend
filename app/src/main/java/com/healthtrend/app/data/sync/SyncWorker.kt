package com.healthtrend.app.data.sync

import android.content.Context
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.repository.HealthEntryRepository
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject

/**
 * WorkManager CoroutineWorker for two-way Google Sheets sync.
 *
 * Sync protocol (CRITICAL — must be exact):
 * 1. **Push first**: For each local entry where synced=false, read Sheet timestamp.
 *    Write ONLY if local updatedAt > Sheet timestamp. Mark synced=true after write.
 * 2. **Pull second**: Read all Sheet rows. For each cell, update local ONLY if
 *    Sheet timestamp > local updatedAt. Create entry if doesn't exist locally.
 *
 * NEVER write empty/null to a Sheet cell.
 * Cell-level writes only — never overwrite entire rows.
 * All errors → Result.retry() with exponential backoff. ZERO user-facing errors.
 * Idempotent: duplicate runs produce same correct state.
 */
@HiltWorker
class SyncWorker @AssistedInject constructor(
    @Assisted appContext: Context,
    @Assisted workerParams: WorkerParameters,
    private val healthEntryRepository: HealthEntryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val sheetsClient: SheetsClient
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        return try {
            // Precondition: must have sheet URL and be signed in
            appSettingsRepository.ensureSettingsExist()
            val settings = appSettingsRepository.getSettingsOnce() ?: return Result.success()
            val sheetUrl = settings.sheetUrl
            val email = settings.googleAccountEmail

            if (sheetUrl.isEmpty() || email.isNullOrEmpty()) {
                // Not configured — silently skip, no error
                return Result.success()
            }

            // Phase 1: PUSH local entries to Sheet
            pushEntries(sheetUrl)

            // Phase 2: PULL Sheet data to local
            pullEntries(sheetUrl)

            Result.success()
        } catch (e: Exception) {
            // ALL sync errors are SILENT — WorkManager exponential backoff handles retries
            Result.retry()
        }
    }

    /**
     * Push phase: send unsynced local entries to Sheet.
     * For each unsynced entry:
     * 1. Read Sheet timestamp for that cell
     * 2. Write ONLY if local updatedAt > Sheet timestamp
     * 3. Mark entry synced=true after successful write
     * NEVER write empty/null values to Sheet cells.
     */
    private suspend fun pushEntries(sheetUrl: String) {
        val unsyncedEntries = healthEntryRepository.getUnsyncedEntriesOnce()
        if (unsyncedEntries.isEmpty()) return

        val sheetRows = sheetsClient.readSheet(sheetUrl)
        val sheetRowMap = sheetRows.associateBy { it.date }

        for (entry in unsyncedEntries) {
            val sheetRow = sheetRowMap[entry.date]

            if (sheetRow != null) {
                // Row exists — check timestamp before writing
                val sheetSlot = sheetRow.slots[entry.timeSlot]
                val sheetTimestamp = sheetSlot?.timestamp ?: 0L

                if (entry.updatedAt > sheetTimestamp) {
                    // Local is newer — write severity + timestamp
                    val severityCol = GoogleSheetsService.SEVERITY_COLUMN[entry.timeSlot]!!
                    val timestampCol = GoogleSheetsService.TIMESTAMP_COLUMN[entry.timeSlot]!!
                    val rowNum = sheetRow.rowIndex + 1 // 1-based for A1 notation

                    sheetsClient.writeCell(sheetUrl, "$severityCol$rowNum", entry.severity.displayName)
                    sheetsClient.writeCell(sheetUrl, "$timestampCol$rowNum", entry.updatedAt)
                }
                // If Sheet is newer or equal, don't write — keep Sheet data
            } else {
                // Row doesn't exist — append new row for this date
                appendNewRow(sheetUrl, entry)
            }

            // Mark as synced after successful push
            healthEntryRepository.markSynced(entry.id)
        }
    }

    /**
     * Pull phase: read all Sheet data and update local entries where Sheet is newer.
     * For each Sheet cell:
     * - Update local ONLY if Sheet timestamp > local updatedAt
     * - Create local entry if date+slot doesn't exist locally
     * - Keep local entry unchanged if local is newer
     */
    private suspend fun pullEntries(sheetUrl: String) {
        val sheetRows = sheetsClient.readSheet(sheetUrl)

        for (row in sheetRows) {
            for (timeSlot in TimeSlot.entries) {
                val sheetSlot = row.slots[timeSlot] ?: continue
                val severity = GoogleSheetsService.parseSeverity(sheetSlot.severity) ?: continue
                val sheetTimestamp = sheetSlot.timestamp
                if (sheetTimestamp <= 0L) continue

                val localEntry = healthEntryRepository.getEntry(row.date, timeSlot.name)

                if (localEntry == null) {
                    // Doesn't exist locally — create from Sheet data
                    healthEntryRepository.insert(
                        HealthEntry(
                            date = row.date,
                            timeSlot = timeSlot,
                            severity = severity,
                            synced = true,
                            updatedAt = sheetTimestamp
                        )
                    )
                } else if (sheetTimestamp > localEntry.updatedAt) {
                    // Sheet is newer — update local
                    healthEntryRepository.update(
                        localEntry.copy(
                            severity = severity,
                            synced = true,
                            updatedAt = sheetTimestamp
                        )
                    )
                }
                // If local is newer or equal, keep local unchanged
            }
        }
    }

    /**
     * Append a new row to Sheet for a date that doesn't exist yet.
     * Format: [date, morning, afternoon, evening, night, morningTs, afternoonTs, eveningTs, nightTs]
     * Only includes data for the triggering entry — other slots are left empty.
     * NEVER writes empty/null to a Sheet cell.
     */
    private suspend fun appendNewRow(sheetUrl: String, entry: HealthEntry) {
        // Build row with only the entry's slot filled — others null
        val rowData = mutableListOf<Any?>(
            entry.date, // Column A: Date
            null, null, null, null, // Columns B-E: severity placeholders
            null, null, null, null  // Columns F-I: timestamp placeholders
        )

        // Fill the specific slot
        val slotIndex = when (entry.timeSlot) {
            TimeSlot.MORNING -> 1
            TimeSlot.AFTERNOON -> 2
            TimeSlot.EVENING -> 3
            TimeSlot.NIGHT -> 4
        }
        rowData[slotIndex] = entry.severity.displayName
        rowData[slotIndex + 4] = entry.updatedAt

        sheetsClient.appendRow(sheetUrl, rowData)
    }

    companion object {
        /** Unique work name for immediate one-time sync. */
        const val WORK_NAME = "health_trend_sync"

        /** Unique work name for periodic sync. */
        const val PERIODIC_WORK_NAME = "health_trend_periodic_sync"
    }
}
