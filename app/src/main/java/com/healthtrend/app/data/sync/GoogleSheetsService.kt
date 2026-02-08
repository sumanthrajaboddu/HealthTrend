package com.healthtrend.app.data.sync

import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing a single row from the Google Sheet.
 * Column layout: A=Date, B=Morning, C=Afternoon, D=Evening, E=Night,
 * F=MorningTimestamp, G=AfternoonTimestamp, H=EveningTimestamp, I=NightTimestamp.
 */
data class SheetRow(
    val date: String, // YYYY-MM-DD (Column A)
    val rowIndex: Int, // 0-based row index in Sheet (row 0 = header, row 1+ = data)
    val slots: Map<TimeSlot, SheetSlotData?>
)

/**
 * Data for a single time slot cell in the Sheet.
 */
data class SheetSlotData(
    val severity: String, // Display name: "No Pain", "Mild", "Moderate", "Severe"
    val timestamp: Long // Epoch millis from timestamp column
)

/**
 * Interface for Google Sheets API operations — enables testing with fakes.
 * Cell-level reads/writes only. NEVER full-row overwrites.
 */
interface SheetsClient {

    /**
     * Read all data rows from the Sheet.
     * Reads columns A through I (Date, 4 severities, 4 timestamps).
     *
     * @param sheetUrl Full Google Sheet URL
     * @return List of SheetRow, one per data row in the Sheet
     */
    suspend fun readSheet(sheetUrl: String): List<SheetRow>

    /**
     * Write a single cell value to the Sheet.
     * Cell-level write — never overwrite entire rows.
     *
     * @param sheetUrl Full Google Sheet URL
     * @param cellRange A1 notation (e.g., "B5" for Morning of row 5)
     * @param value The value to write
     */
    suspend fun writeCell(sheetUrl: String, cellRange: String, value: Any)

    /**
     * Append a new row to the Sheet for a new date.
     * Format: [date, morning, afternoon, evening, night, morningTs, afternoonTs, eveningTs, nightTs]
     *
     * @param sheetUrl Full Google Sheet URL
     * @param rowData List of values for the new row
     */
    suspend fun appendRow(sheetUrl: String, rowData: List<Any?>)
}

/**
 * Google Sheets API v4 service for cell-level reads and writes.
 * Uses credentials from GoogleAuthManager.
 * NEVER overwrites entire rows — cell-level writes only.
 *
 * Sheet column mapping:
 * - A = Date (YYYY-MM-DD)
 * - B = Morning severity (display name)
 * - C = Afternoon severity (display name)
 * - D = Evening severity (display name)
 * - E = Night severity (display name)
 * - F = Morning timestamp (epoch millis)
 * - G = Afternoon timestamp (epoch millis)
 * - H = Evening timestamp (epoch millis)
 * - I = Night timestamp (epoch millis)
 */
@Singleton
class GoogleSheetsService @Inject constructor() : SheetsClient {

    override suspend fun readSheet(sheetUrl: String): List<SheetRow> {
        // TODO: Implement with Google Sheets API v4 client
        // For now, returns empty list — actual API integration requires
        // GoogleAccountCredential which depends on runtime context.
        //
        // Implementation will:
        // 1. Extract spreadsheet ID from URL
        // 2. Use Sheets API to read range "Sheet1!A:I"
        // 3. Parse rows into SheetRow objects
        // 4. Skip header row (index 0)
        return emptyList()
    }

    override suspend fun writeCell(sheetUrl: String, cellRange: String, value: Any) {
        // TODO: Implement with Google Sheets API v4 client
        // Implementation will:
        // 1. Extract spreadsheet ID from URL
        // 2. Use Sheets API to write single cell value
        // 3. Use ValueInputOption.RAW for numeric timestamps, USER_ENTERED for text
    }

    override suspend fun appendRow(sheetUrl: String, rowData: List<Any?>) {
        // TODO: Implement with Google Sheets API v4 client
        // Implementation will:
        // 1. Extract spreadsheet ID from URL
        // 2. Use Sheets API to append row to "Sheet1!A:I"
    }

    companion object {
        /**
         * Column mapping for TimeSlot to Sheet column letters.
         * Severity columns: B=Morning, C=Afternoon, D=Evening, E=Night
         * Timestamp columns: F=Morning, G=Afternoon, H=Evening, I=Night
         */
        val SEVERITY_COLUMN: Map<TimeSlot, String> = mapOf(
            TimeSlot.MORNING to "B",
            TimeSlot.AFTERNOON to "C",
            TimeSlot.EVENING to "D",
            TimeSlot.NIGHT to "E"
        )

        val TIMESTAMP_COLUMN: Map<TimeSlot, String> = mapOf(
            TimeSlot.MORNING to "F",
            TimeSlot.AFTERNOON to "G",
            TimeSlot.EVENING to "H",
            TimeSlot.NIGHT to "I"
        )

        /**
         * Extract spreadsheet ID from a Google Sheets URL.
         * URL format: https://docs.google.com/spreadsheets/d/{spreadsheetId}/...
         */
        fun extractSpreadsheetId(url: String): String? {
            val regex = Regex("/spreadsheets/d/([a-zA-Z0-9_-]+)")
            return regex.find(url)?.groupValues?.getOrNull(1)
        }

        /**
         * Convert severity display name from Sheet to Severity enum.
         * Returns null if the display name doesn't match any known severity.
         */
        fun parseSeverity(displayName: String): Severity? {
            return Severity.entries.find { it.displayName == displayName }
        }

        /**
         * Parse timestamp from Sheet cell value.
         * Returns 0L if the value is empty or not a valid long.
         */
        fun parseTimestamp(value: String?): Long {
            return value?.toLongOrNull() ?: 0L
        }
    }
}
