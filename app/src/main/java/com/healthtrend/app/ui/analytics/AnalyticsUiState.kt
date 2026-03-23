package com.healthtrend.app.ui.analytics

import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import java.io.File
import java.time.LocalDate

/**
 * Date range options for the Analytics trend chart.
 * Default selection: ONE_WEEK (AC #1).
 */
enum class DateRange(val days: Int, val displayName: String) {
    ONE_WEEK(7, "1 Week"),
    ONE_MONTH(30, "1 Month"),
    THREE_MONTHS(90, "3 Months")
}

/**
 * Trend direction over the selected period.
 * Calculated by comparing first-half average to second-half average.
 */
enum class TrendDirection(val displayName: String) {
    IMPROVING("improving"),
    WORSENING("worsening"),
    STABLE("stable")
}

/**
 * A single data point on the severity trend chart.
 * One per day — uses the maximum severity logged that day.
 */
data class ChartDataPoint(
    val date: LocalDate,
    val severityValue: Float
)

/**
 * Summary statistics for the selected date range.
 * Used in TalkBack announcements (AC #6).
 */
data class TrendSummary(
    val averageSeverity: Severity,
    val trendDirection: TrendDirection,
    val periodLabel: String
)

/**
 * UI state for the Analytics screen.
 * Sealed interface per project-context.md — NEVER a data class or open class.
 */
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState

    data class Success(
        val chartData: List<ChartDataPoint>,
        val summary: TrendSummary,
        val selectedRange: DateRange,
        val slotAverages: Map<TimeSlot, Severity?> = emptyMap()
    ) : AnalyticsUiState

    data class Empty(
        val selectedRange: DateRange
    ) : AnalyticsUiState
}

/**
 * Export state for PDF generation (Story 6.1 AC #1, #2).
 * Sealed interface per project-context.md.
 * Idle → Generating → Preview(pdfFile) or Error.
 */
sealed interface ExportState {
    data object Idle : ExportState
    data object Generating : ExportState
    data class Preview(val pdfFile: File) : ExportState
    data class Error(val message: String) : ExportState
}
