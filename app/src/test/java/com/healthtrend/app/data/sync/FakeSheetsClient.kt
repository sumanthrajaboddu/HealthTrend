package com.healthtrend.app.data.sync

import com.healthtrend.app.data.model.TimeSlot

/**
 * Fake SheetsClient for unit testing.
 * Stores in-memory rows and tracks write operations.
 * accountEmail parameter is accepted but ignored — tests don't need real auth.
 */
class FakeSheetsClient : SheetsClient {

    /** In-memory Sheet data keyed by date. */
    val rows = mutableListOf<SheetRow>()

    /** Tracks cell writes: list of (cellRange, value) pairs. */
    val cellWrites = mutableListOf<Pair<String, Any>>()

    /** Tracks appended rows. */
    val appendedRows = mutableListOf<List<Any?>>()

    /** If true, readSheet and writeCell throw an exception. */
    var shouldFail = false

    /** If true, createSheet throws an exception. */
    var createSheetShouldFail = false

    /** Tracks createSheet calls: list of (accountEmail, title) pairs. */
    val createdSheets = mutableListOf<Pair<String, String>>()

    /** URL returned by createSheet. */
    var createSheetReturnUrl = "https://docs.google.com/spreadsheets/d/fake-id-123"

    /** If true, findSheet throws an exception. */
    var findSheetShouldFail = false

    /** Tracks findSheet calls: list of (accountEmail, title) pairs. */
    val findSheetCalls = mutableListOf<Pair<String, String>>()

    /** URL returned by findSheet. null means "not found". */
    var findSheetReturnUrl: String? = null

    override suspend fun readSheet(sheetUrl: String, accountEmail: String): List<SheetRow> {
        if (shouldFail) throw RuntimeException("Simulated API failure")
        return rows.toList()
    }

    override suspend fun writeCell(
        sheetUrl: String,
        accountEmail: String,
        cellRange: String,
        value: Any
    ) {
        if (shouldFail) throw RuntimeException("Simulated API failure")
        cellWrites.add(cellRange to value)
    }

    override suspend fun appendRow(
        sheetUrl: String,
        accountEmail: String,
        rowData: List<Any?>
    ) {
        if (shouldFail) throw RuntimeException("Simulated API failure")
        appendedRows.add(rowData)

        // Add to in-memory rows for subsequent reads
        val date = rowData.getOrNull(0) as? String ?: return
        val existingIndex = rows.indexOfFirst { it.date == date }

        val slots = mutableMapOf<TimeSlot, SheetSlotData?>()
        for (slot in TimeSlot.entries) {
            val severityIdx = when (slot) {
                TimeSlot.MORNING -> 1
                TimeSlot.AFTERNOON -> 2
                TimeSlot.EVENING -> 3
                TimeSlot.NIGHT -> 4
            }
            val timestampIdx = severityIdx + 4
            val severity = rowData.getOrNull(severityIdx) as? String
            val timestamp = rowData.getOrNull(timestampIdx) as? Long ?: 0L
            slots[slot] = if (severity != null) SheetSlotData(severity, timestamp) else null
        }

        val newRow = SheetRow(
            date = date,
            rowIndex = if (existingIndex >= 0) rows[existingIndex].rowIndex else rows.size + 1,
            slots = slots
        )

        if (existingIndex >= 0) {
            rows[existingIndex] = newRow
        } else {
            rows.add(newRow)
        }
    }

    override suspend fun createSheet(accountEmail: String, title: String): String {
        if (createSheetShouldFail) throw RuntimeException("Simulated createSheet failure")
        createdSheets.add(accountEmail to title)
        return createSheetReturnUrl
    }

    override suspend fun findSheet(accountEmail: String, title: String): String? {
        if (findSheetShouldFail) throw RuntimeException("Simulated findSheet failure")
        findSheetCalls.add(accountEmail to title)
        return findSheetReturnUrl
    }

    fun reset() {
        rows.clear()
        cellWrites.clear()
        appendedRows.clear()
        createdSheets.clear()
        findSheetCalls.clear()
        shouldFail = false
        createSheetShouldFail = false
        createSheetReturnUrl = "https://docs.google.com/spreadsheets/d/fake-id-123"
        findSheetShouldFail = false
        findSheetReturnUrl = null
    }
}
