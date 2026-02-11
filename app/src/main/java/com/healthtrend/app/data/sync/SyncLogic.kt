package com.healthtrend.app.data.sync

import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.HealthEntryRepository
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extracted sync protocol logic — testable without Android context.
 *
 * Sync protocol (CRITICAL — must be exact):
 * 1. **Read Sheet once** — single API call, used by both phases.
 * 2. **Push first**: For each local entry where synced=false, compare timestamps.
 *    Write ONLY if local updatedAt > Sheet timestamp. Mark synced=true ONLY after write.
 * 3. **Pull second**: For each Sheet cell, update local ONLY if
 *    Sheet timestamp > local updatedAt. Create entry if doesn't exist locally.
 *
 * NEVER write empty/null to a Sheet cell — cell-level writes only.
 * All errors propagate to SyncWorker for Result.retry().
 * Idempotent: duplicate runs produce same correct state.
 */
@Singleton
class SyncLogic @Inject constructor(
    private val healthEntryRepository: HealthEntryRepository,
    private val sheetsClient: SheetsClient
) {

    /**
     * Execute full two-way sync: single Sheet read, push, then pull.
     * Single readSheet call shared by both phases to halve API quota usage.
     */
    suspend fun executeSync(sheetUrl: String, accountEmail: String) {
        // Single read — used by both push and pull phases.
        // Tradeoff: another device writing during our sync is caught by next periodic sync.
        val sheetRows = sheetsClient.readSheet(sheetUrl, accountEmail)

        pushEntries(sheetUrl, accountEmail, sheetRows)
        pullEntries(sheetRows)
    }

    /**
     * Push phase: send unsynced local entries to Sheet.
     * For each unsynced entry:
     * 1. Read Sheet timestamp for that cell (from cached sheetRows)
     * 2. Write ONLY if local updatedAt > Sheet timestamp
     * 3. Mark entry synced=true ONLY after successful write
     * 4. If timestamps are equal, data is in sync — mark synced
     * 5. If Sheet is newer, leave synced=false for pull phase to handle
     * NEVER write empty/null values to Sheet cells.
     */
    internal suspend fun pushEntries(
        sheetUrl: String,
        accountEmail: String,
        sheetRows: List<SheetRow>
    ) {
        val unsyncedEntries = healthEntryRepository.getUnsyncedEntriesOnce()
        if (unsyncedEntries.isEmpty()) return

        val sheetRowMap = sheetRows.associateBy { it.date }

        for (entry in unsyncedEntries) {
            val sheetRow = sheetRowMap[entry.date]

            if (sheetRow != null) {
                // Row exists — check timestamp before writing
                val sheetSlot = sheetRow.slots[entry.timeSlot]
                val sheetTimestamp = sheetSlot?.timestamp ?: 0L

                if (entry.updatedAt > sheetTimestamp) {
                    // Local is newer — write severity + timestamp (cell-level only)
                    val severityCol = GoogleSheetsService.SEVERITY_COLUMN[entry.timeSlot]!!
                    val timestampCol = GoogleSheetsService.TIMESTAMP_COLUMN[entry.timeSlot]!!
                    val rowNum = sheetRow.rowIndex + 1 // A1 notation = rowIndex + 1

                    sheetsClient.writeCell(
                        sheetUrl, accountEmail,
                        "$severityCol$rowNum", entry.severity.displayName
                    )
                    sheetsClient.writeCell(
                        sheetUrl, accountEmail,
                        "$timestampCol$rowNum", entry.updatedAt
                    )
                    healthEntryRepository.markSynced(entry.id)
                } else if (entry.updatedAt == sheetTimestamp) {
                    // Data already in sync — mark without writing
                    healthEntryRepository.markSynced(entry.id)
                }
                // If Sheet is newer: leave synced=false, pull phase will update local
            } else {
                // Row doesn't exist — create via cell-level writes (NEVER null to Sheet)
                writeNewRowCells(sheetUrl, accountEmail, entry, sheetRows)
                healthEntryRepository.markSynced(entry.id)
            }
        }
    }

    /**
     * Pull phase: read all Sheet data and update local entries where Sheet is newer.
     * For each Sheet cell:
     * - Update local ONLY if Sheet timestamp > local updatedAt
     * - Create local entry if date+slot doesn't exist locally
     * - Keep local entry unchanged if local is newer
     */
    internal suspend fun pullEntries(sheetRows: List<SheetRow>) {
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
     * Write a new date row to the Sheet using cell-level writes only.
     * NEVER writes null/empty to Sheet cells — only writes the populated slot.
     *
     * @param sheetRows Current Sheet data, used to determine next available row number.
     */
    private suspend fun writeNewRowCells(
        sheetUrl: String,
        accountEmail: String,
        entry: HealthEntry,
        sheetRows: List<SheetRow>
    ) {
        // Determine next row in A1 notation.
        // rowIndex is 0-based from API (0=header). A1 row = rowIndex + 1.
        val nextA1Row = if (sheetRows.isEmpty()) {
            2 // First data row (row 1 is header)
        } else {
            sheetRows.maxOf { it.rowIndex } + 2 // Next after last + A1 offset
        }

        // Write date to column A
        sheetsClient.writeCell(sheetUrl, accountEmail, "A$nextA1Row", entry.date)

        // Write severity and timestamp for the entry's slot only
        val severityCol = GoogleSheetsService.SEVERITY_COLUMN[entry.timeSlot]!!
        val timestampCol = GoogleSheetsService.TIMESTAMP_COLUMN[entry.timeSlot]!!
        sheetsClient.writeCell(
            sheetUrl, accountEmail,
            "$severityCol$nextA1Row", entry.severity.displayName
        )
        sheetsClient.writeCell(
            sheetUrl, accountEmail,
            "$timestampCol$nextA1Row", entry.updatedAt
        )
    }
}
