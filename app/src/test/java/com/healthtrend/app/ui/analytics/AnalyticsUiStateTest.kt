package com.healthtrend.app.ui.analytics

import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.time.LocalDate

/**
 * Unit tests for AnalyticsUiState, DateRange, TrendDirection, and related models.
 */
class AnalyticsUiStateTest {

    // ===================================================================
    // DateRange tests
    // ===================================================================

    @Test
    fun `DateRange ONE_WEEK has 7 days`() {
        assertEquals(7, DateRange.ONE_WEEK.days)
    }

    @Test
    fun `DateRange ONE_MONTH has 30 days`() {
        assertEquals(30, DateRange.ONE_MONTH.days)
    }

    @Test
    fun `DateRange THREE_MONTHS has 90 days`() {
        assertEquals(90, DateRange.THREE_MONTHS.days)
    }

    @Test
    fun `DateRange display names are correct`() {
        assertEquals("1 Week", DateRange.ONE_WEEK.displayName)
        assertEquals("1 Month", DateRange.ONE_MONTH.displayName)
        assertEquals("3 Months", DateRange.THREE_MONTHS.displayName)
    }

    @Test
    fun `DateRange has exactly 3 values`() {
        assertEquals(3, DateRange.entries.size)
    }

    // ===================================================================
    // TrendDirection tests
    // ===================================================================

    @Test
    fun `TrendDirection display names are lowercase`() {
        assertEquals("improving", TrendDirection.IMPROVING.displayName)
        assertEquals("worsening", TrendDirection.WORSENING.displayName)
        assertEquals("stable", TrendDirection.STABLE.displayName)
    }

    // ===================================================================
    // ChartDataPoint tests
    // ===================================================================

    @Test
    fun `ChartDataPoint stores date and severity value`() {
        val point = ChartDataPoint(
            date = LocalDate.of(2026, 2, 8),
            severityValue = 2.0f
        )
        assertEquals(LocalDate.of(2026, 2, 8), point.date)
        assertEquals(2.0f, point.severityValue)
    }

    @Test
    fun `ChartDataPoint equality is value-based`() {
        val p1 = ChartDataPoint(LocalDate.of(2026, 1, 1), 1.0f)
        val p2 = ChartDataPoint(LocalDate.of(2026, 1, 1), 1.0f)
        val p3 = ChartDataPoint(LocalDate.of(2026, 1, 2), 1.0f)
        assertEquals(p1, p2)
        assertNotEquals(p1, p3)
    }

    // ===================================================================
    // TrendSummary tests
    // ===================================================================

    @Test
    fun `TrendSummary stores average severity, direction, and period`() {
        val summary = TrendSummary(
            averageSeverity = Severity.MILD,
            trendDirection = TrendDirection.IMPROVING,
            periodLabel = "1 Week"
        )
        assertEquals(Severity.MILD, summary.averageSeverity)
        assertEquals(TrendDirection.IMPROVING, summary.trendDirection)
        assertEquals("1 Week", summary.periodLabel)
    }

    // ===================================================================
    // AnalyticsUiState sealed interface tests
    // ===================================================================

    @Test
    fun `Loading is a singleton`() {
        val a: AnalyticsUiState = AnalyticsUiState.Loading
        val b: AnalyticsUiState = AnalyticsUiState.Loading
        assertEquals(a, b)
    }

    @Test
    fun `Empty carries selectedRange`() {
        val empty = AnalyticsUiState.Empty(selectedRange = DateRange.ONE_MONTH)
        assertEquals(DateRange.ONE_MONTH, empty.selectedRange)
    }

    @Test
    fun `Success carries chartData, summary, and selectedRange`() {
        val chartData = listOf(
            ChartDataPoint(LocalDate.of(2026, 2, 1), 1.0f)
        )
        val summary = TrendSummary(
            averageSeverity = Severity.MILD,
            trendDirection = TrendDirection.STABLE,
            periodLabel = "1 Week"
        )
        val success = AnalyticsUiState.Success(
            chartData = chartData,
            summary = summary,
            selectedRange = DateRange.ONE_WEEK
        )
        assertEquals(chartData, success.chartData)
        assertEquals(summary, success.summary)
        assertEquals(DateRange.ONE_WEEK, success.selectedRange)
    }

    // ===================================================================
    // Story 5.2: slotAverages in Success
    // ===================================================================

    @Test
    fun `Success default slotAverages is empty map`() {
        val success = AnalyticsUiState.Success(
            chartData = listOf(ChartDataPoint(LocalDate.of(2026, 2, 1), 1.0f)),
            summary = TrendSummary(Severity.MILD, TrendDirection.STABLE, "1 Week"),
            selectedRange = DateRange.ONE_WEEK
        )
        assertTrue(success.slotAverages.isEmpty())
    }

    @Test
    fun `Success carries slotAverages with data`() {
        val slotAverages = mapOf(
            TimeSlot.MORNING to Severity.MILD,
            TimeSlot.AFTERNOON to Severity.MODERATE,
            TimeSlot.EVENING to null,
            TimeSlot.NIGHT to Severity.SEVERE
        )
        val success = AnalyticsUiState.Success(
            chartData = listOf(ChartDataPoint(LocalDate.of(2026, 2, 1), 1.0f)),
            summary = TrendSummary(Severity.MILD, TrendDirection.STABLE, "1 Week"),
            selectedRange = DateRange.ONE_WEEK,
            slotAverages = slotAverages
        )
        assertEquals(Severity.MILD, success.slotAverages[TimeSlot.MORNING])
        assertEquals(Severity.MODERATE, success.slotAverages[TimeSlot.AFTERNOON])
        assertNull(success.slotAverages[TimeSlot.EVENING])
        assertEquals(Severity.SEVERE, success.slotAverages[TimeSlot.NIGHT])
    }

    // ===================================================================
    // Story 6.1: ExportState sealed interface tests
    // ===================================================================

    @Test
    fun `ExportState Idle is a singleton`() {
        val a: ExportState = ExportState.Idle
        val b: ExportState = ExportState.Idle
        assertEquals(a, b)
    }

    @Test
    fun `ExportState Generating is a singleton`() {
        val a: ExportState = ExportState.Generating
        val b: ExportState = ExportState.Generating
        assertEquals(a, b)
    }

    @Test
    fun `ExportState Preview carries pdfFile`() {
        val file = File("/tmp/test.pdf")
        val preview = ExportState.Preview(pdfFile = file)
        assertEquals(file, preview.pdfFile)
    }

    @Test
    fun `ExportState Error carries message`() {
        val error = ExportState.Error(message = "Something went wrong")
        assertEquals("Something went wrong", error.message)
    }

    @Test
    fun `ExportState types are distinct`() {
        val idle: ExportState = ExportState.Idle
        val generating: ExportState = ExportState.Generating
        val preview: ExportState = ExportState.Preview(File("/tmp/test.pdf"))
        val error: ExportState = ExportState.Error("err")

        assertTrue(idle is ExportState.Idle)
        assertTrue(generating is ExportState.Generating)
        assertTrue(preview is ExportState.Preview)
        assertTrue(error is ExportState.Error)
    }
}
