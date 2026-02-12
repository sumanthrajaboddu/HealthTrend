package com.healthtrend.app.data.sync

import android.content.Context
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.sheets.v4.SheetsScopes
import com.google.api.services.sheets.v4.model.Spreadsheet
import com.google.api.services.sheets.v4.model.SpreadsheetProperties
import com.google.api.services.sheets.v4.model.ValueRange
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing a single row from the Google Sheet.
 * Column layout: A=Date, B=Morning, C=Afternoon, D=Evening, E=Night,
 * F=MorningTimestamp, G=AfternoonTimestamp, H=EveningTimestamp, I=NightTimestamp.
 */
data class SheetRow(
    val date: String, // YYYY-MM-DD (Column A)
    val rowIndex: Int, // 0-based index from API response (header=0, first data=1). A1 row = rowIndex + 1.
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
 *
 * @param accountEmail Google account email for OAuth2 authentication.
 */
interface SheetsClient {

    companion object {
        /** Canonical sheet title used for auto-creation (Story 3.4). */
        const val DEFAULT_SHEET_TITLE = "HealthTrend"
    }

    /**
     * Read all data rows from the Sheet.
     * Reads columns A through I (Date, 4 severities, 4 timestamps).
     *
     * @param sheetUrl Full Google Sheet URL
     * @param accountEmail Google account email for authentication
     * @return List of SheetRow, one per data row in the Sheet
     */
    suspend fun readSheet(sheetUrl: String, accountEmail: String): List<SheetRow>

    /**
     * Write a single cell value to the Sheet.
     * Cell-level write — never overwrite entire rows.
     *
     * @param sheetUrl Full Google Sheet URL
     * @param accountEmail Google account email for authentication
     * @param cellRange A1 notation (e.g., "B5" for Morning of row 5)
     * @param value The value to write
     */
    suspend fun writeCell(sheetUrl: String, accountEmail: String, cellRange: String, value: Any)

    /**
     * Append a new row to the Sheet for a new date.
     * Format: [date, morning, afternoon, evening, night, morningTs, afternoonTs, eveningTs, nightTs]
     *
     * @param sheetUrl Full Google Sheet URL
     * @param accountEmail Google account email for authentication
     * @param rowData List of values for the new row
     */
    suspend fun appendRow(sheetUrl: String, accountEmail: String, rowData: List<Any?>)

    /**
     * Create a new Google Sheet with the given title and header row.
     * Header row: Date, Morning, Afternoon, Evening, Night.
     *
     * @param accountEmail Google account email for authentication
     * @param title Sheet title (e.g., "HealthTrend")
     * @return Full Google Sheet URL
     */
    suspend fun createSheet(accountEmail: String, title: String): String

    /**
     * Search Google Drive for an existing spreadsheet with the given title.
     * Returns the most recently modified match, or null if none found.
     * Used to reuse an existing sheet across devices instead of creating duplicates.
     *
     * @param accountEmail Google account email for authentication
     * @param title Sheet title to search for (e.g., "HealthTrend")
     * @return Full Google Sheet URL if found, null otherwise
     */
    suspend fun findSheet(accountEmail: String, title: String): String?
}

/**
 * Google Sheets API v4 service for cell-level reads and writes.
 * Uses GoogleAccountCredential with OAuth2 for authentication.
 * Credentials come from the signed-in Google account (Credential Manager).
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
class GoogleSheetsService @Inject constructor(
    @param:ApplicationContext private val context: Context
) : SheetsClient {

    /**
     * OAuth2 scopes used for all API calls.
     * - SPREADSHEETS: read/write sheet data
     * - DRIVE_METADATA_READONLY: search Drive for existing sheets by name
     */
    private val oauthScopes = listOf(
        SheetsScopes.SPREADSHEETS,
        DriveScopes.DRIVE_METADATA_READONLY
    )

    /**
     * Build authenticated credential for the given account.
     * GoogleAccountCredential uses AccountManager for silent token refresh.
     */
    private fun buildCredential(accountEmail: String): GoogleAccountCredential {
        val credential = GoogleAccountCredential.usingOAuth2(context, oauthScopes)
        credential.selectedAccountName = accountEmail
        return credential
    }

    /**
     * Build authenticated Sheets API client for the given account.
     */
    private fun buildSheetsService(accountEmail: String): Sheets {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Sheets.Builder(transport, jsonFactory, buildCredential(accountEmail))
            .setApplicationName(APP_NAME)
            .build()
    }

    /**
     * Build authenticated Drive API client for the given account.
     * Used for searching existing spreadsheets by name.
     */
    private fun buildDriveService(accountEmail: String): Drive {
        val transport = NetHttpTransport()
        val jsonFactory = GsonFactory.getDefaultInstance()

        return Drive.Builder(transport, jsonFactory, buildCredential(accountEmail))
            .setApplicationName(APP_NAME)
            .build()
    }

    override suspend fun readSheet(sheetUrl: String, accountEmail: String): List<SheetRow> {
        return withContext(Dispatchers.IO) {
            val spreadsheetId = extractSpreadsheetId(sheetUrl)
                ?: return@withContext emptyList()
            val service = buildSheetsService(accountEmail)

            val response = service.spreadsheets().values()
                .get(spreadsheetId, DATA_RANGE)
                .execute()

            val rows = response.getValues() ?: return@withContext emptyList()

            // Skip header row (index 0), parse data rows
            rows.mapIndexedNotNull { index, row ->
                if (index == 0) return@mapIndexedNotNull null
                parseSheetRow(row, index)
            }
        }
    }

    override suspend fun writeCell(
        sheetUrl: String,
        accountEmail: String,
        cellRange: String,
        value: Any
    ) {
        withContext(Dispatchers.IO) {
            val spreadsheetId = extractSpreadsheetId(sheetUrl) ?: return@withContext
            val service = buildSheetsService(accountEmail)

            val range = "$SHEET_NAME!$cellRange"
            val body = ValueRange().setValues(listOf(listOf(value)))

            service.spreadsheets().values()
                .update(spreadsheetId, range, body)
                .setValueInputOption("RAW")
                .execute()
        }
    }

    override suspend fun appendRow(
        sheetUrl: String,
        accountEmail: String,
        rowData: List<Any?>
    ) {
        withContext(Dispatchers.IO) {
            val spreadsheetId = extractSpreadsheetId(sheetUrl) ?: return@withContext
            val service = buildSheetsService(accountEmail)

            val body = ValueRange().setValues(listOf(rowData))

            service.spreadsheets().values()
                .append(spreadsheetId, "$SHEET_NAME!A:I", body)
                .setValueInputOption("RAW")
                .execute()
        }
    }

    override suspend fun createSheet(accountEmail: String, title: String): String {
        return withContext(Dispatchers.IO) {
            val service = buildSheetsService(accountEmail)

            // Create a new spreadsheet with the given title
            val spreadsheet = Spreadsheet().apply {
                properties = SpreadsheetProperties().apply {
                    this.title = title
                }
            }

            val created = service.spreadsheets().create(spreadsheet).execute()
            val spreadsheetId = created.spreadsheetId

            // Write header row: Date + TimeSlot display names (never hardcode slot labels)
            val headerRow = listOf("Date") + TimeSlot.entries.map { it.displayName }
            val headerBody = ValueRange().setValues(listOf(headerRow))
            service.spreadsheets().values()
                .update(spreadsheetId, "$SHEET_NAME!A1:E1", headerBody)
                .setValueInputOption("RAW")
                .execute()

            "$SHEET_URL_PREFIX$spreadsheetId"
        }
    }

    override suspend fun findSheet(accountEmail: String, title: String): String? {
        return withContext(Dispatchers.IO) {
            val driveService = buildDriveService(accountEmail)

            // Search for spreadsheets matching the exact title that are not trashed.
            // Order by modifiedTime desc to prefer the most recently modified one.
            val query = "name = '$title' " +
                "and mimeType = 'application/vnd.google-apps.spreadsheet' " +
                "and trashed = false"

            val result = driveService.files().list()
                .setQ(query)
                .setFields("files(id, name, modifiedTime)")
                .setOrderBy("modifiedTime desc")
                .setPageSize(1)
                .execute()

            val file = result.files?.firstOrNull() ?: return@withContext null
            "$SHEET_URL_PREFIX${file.id}"
        }
    }

    /**
     * Parse a raw API row into a SheetRow.
     * @param row Raw row data from the API (List of cell values)
     * @param rowIndex 0-based index in the API response (0=header)
     */
    private fun parseSheetRow(row: List<Any?>, rowIndex: Int): SheetRow? {
        val date = (row.getOrNull(0) as? String)?.takeIf { it.isNotBlank() }
            ?: return null

        val slots = mutableMapOf<TimeSlot, SheetSlotData?>()
        val slotMapping = mapOf(
            TimeSlot.MORNING to Pair(1, 5),     // B=severity, F=timestamp
            TimeSlot.AFTERNOON to Pair(2, 6),   // C=severity, G=timestamp
            TimeSlot.EVENING to Pair(3, 7),     // D=severity, H=timestamp
            TimeSlot.NIGHT to Pair(4, 8)        // E=severity, I=timestamp
        )

        for ((slot, indices) in slotMapping) {
            val severityStr = (row.getOrNull(indices.first) as? String)
                ?.takeIf { it.isNotBlank() }
            val timestampStr = row.getOrNull(indices.second)?.toString()
            val timestamp = parseTimestamp(timestampStr)

            slots[slot] = if (severityStr != null && timestamp > 0L) {
                SheetSlotData(severityStr, timestamp)
            } else {
                null
            }
        }

        return SheetRow(date = date, rowIndex = rowIndex, slots = slots)
    }

    companion object {
        private const val APP_NAME = "HealthTrend"
        private const val SHEET_NAME = "Sheet1"
        private const val DATA_RANGE = "Sheet1!A:I"
        private const val SHEET_URL_PREFIX = "https://docs.google.com/spreadsheets/d/"

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
