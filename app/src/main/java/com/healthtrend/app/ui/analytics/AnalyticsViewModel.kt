package com.healthtrend.app.ui.analytics

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.graphics.Bitmap
import com.healthtrend.app.data.export.PdfGenerator
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.repository.HealthEntryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Analytics screen (Stories 5.1, 5.2).
 * One ViewModel per screen — separate from DayCard and Settings.
 * Uses StateFlow + collectAsStateWithLifecycle() — NEVER LiveData.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class AnalyticsViewModel @Inject constructor(
    private val repository: HealthEntryRepository,
    private val appSettingsRepository: AppSettingsRepository,
    private val pdfGenerator: PdfGenerator
) : ViewModel() {

    private val _selectedRange = MutableStateFlow(DateRange.ONE_WEEK)
    val selectedRange: StateFlow<DateRange> = _selectedRange.asStateFlow()

    /**
     * Export state for PDF generation (Story 6.1 AC #1).
     * Idle → Generating → Preview(pdfFile) or Error.
     */
    private val _exportState = MutableStateFlow<ExportState>(ExportState.Idle)
    val exportState: StateFlow<ExportState> = _exportState.asStateFlow()

    /**
     * Analytics UI state — reacts to date range changes.
     * Queries Room → transforms to chart data → calculates summary.
     */
    val uiState: StateFlow<AnalyticsUiState> = _selectedRange
        .flatMapLatest { range ->
            val endDate = LocalDate.now()
            // Inclusive range query in Room: include today plus the previous (days - 1) dates.
            val startDate = endDate.minusDays((range.days - 1).toLong())
            val startStr = startDate.format(DATE_FORMATTER)
            val endStr = endDate.format(DATE_FORMATTER)

            repository.getEntriesBetweenDates(startStr, endStr)
                .map { entries -> transformToUiState(entries, range) }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = AnalyticsUiState.Loading
        )

    /**
     * Update the selected date range.
     * Triggers re-query of entries and chart re-draw (AC #3).
     */
    fun selectRange(range: DateRange) {
        _selectedRange.value = range
    }

    /**
     * Transform raw entries to UI state.
     * Daily aggregation: max severity per day (simpler approach per Dev Notes).
     * Missing days: skipped (gap in line).
     */
    private fun transformToUiState(
        entries: List<HealthEntry>,
        range: DateRange
    ): AnalyticsUiState {
        if (entries.isEmpty()) {
            return AnalyticsUiState.Empty(selectedRange = range)
        }

        val chartData = aggregateDaily(entries)
        val summary = calculateSummary(entries, chartData, range)
        val slotAverages = calculateSlotAverages(entries)

        return AnalyticsUiState.Success(
            chartData = chartData,
            summary = summary,
            selectedRange = range,
            slotAverages = slotAverages
        )
    }

    /**
     * Aggregate entries to one data point per day (maximum severity).
     * Returns sorted by date ascending.
     */
    private fun aggregateDaily(entries: List<HealthEntry>): List<ChartDataPoint> {
        return entries
            .groupBy { it.date }
            .map { (dateStr, dayEntries) ->
                val date = LocalDate.parse(dateStr, DATE_FORMATTER)
                val maxSeverity = dayEntries.maxOf { it.severity.numericValue }
                ChartDataPoint(
                    date = date,
                    severityValue = maxSeverity.toFloat()
                )
            }
            .sortedBy { it.date }
    }

    /**
     * Calculate summary stats: average severity and trend direction.
     * Average: mean of all entry severity values, rounded to nearest Severity.
     * Trend: compare first-half average to second-half average.
     */
    private fun calculateSummary(
        entries: List<HealthEntry>,
        chartData: List<ChartDataPoint>,
        range: DateRange
    ): TrendSummary {
        // Average severity across ALL entries (not just daily max)
        val avgNumeric = entries.map { it.severity.numericValue }.average()
        val averageSeverity = numericToSeverity(avgNumeric)

        // Trend direction: compare first half to second half of chart data
        val trendDirection = calculateTrendDirection(chartData)

        return TrendSummary(
            averageSeverity = averageSeverity,
            trendDirection = trendDirection,
            periodLabel = range.displayName
        )
    }

    /**
     * Calculate trend direction by comparing first-half average to second-half average.
     * Improving = second half lower. Worsening = second half higher. Stable = same.
     */
    private fun calculateTrendDirection(chartData: List<ChartDataPoint>): TrendDirection {
        if (chartData.size < 2) return TrendDirection.STABLE

        val midpoint = chartData.size / 2
        val firstHalf = chartData.subList(0, midpoint)
        val secondHalf = chartData.subList(midpoint, chartData.size)

        val firstAvg = firstHalf.map { it.severityValue }.average()
        val secondAvg = secondHalf.map { it.severityValue }.average()

        val diff = secondAvg - firstAvg
        return when {
            diff < -TREND_THRESHOLD -> TrendDirection.IMPROVING
            diff > TREND_THRESHOLD -> TrendDirection.WORSENING
            else -> TrendDirection.STABLE
        }
    }

    /**
     * Calculate average severity per TimeSlot across all entries in the range.
     * Null for slots with no data (Story 5.2 AC #4).
     * Rounding: 0–0.49=NO_PAIN, 0.5–1.49=MILD, 1.5–2.49=MODERATE, 2.5–3.0=SEVERE (AC #2).
     * Reactively recalculates when date range changes via flatMapLatest (AC #3).
     */
    private fun calculateSlotAverages(entries: List<HealthEntry>): Map<TimeSlot, Severity?> {
        val entriesBySlot = entries.groupBy { it.timeSlot }
        return TimeSlot.entries.associateWith { slot ->
            val slotEntries = entriesBySlot[slot]
            if (slotEntries.isNullOrEmpty()) {
                null
            } else {
                val avg = slotEntries.map { it.severity.numericValue }.average()
                numericToSeverity(avg)
            }
        }
    }

    /**
     * Trigger PDF export for the current date range (Story 6.1 AC #1, #2).
     * Runs on Dispatchers.IO — NOT Main thread (subtask 3.4).
     * Collects current entries, slot averages, patient name, and passes to PdfGenerator.
     */
    fun onExportPdf(chartBitmap: Bitmap? = null) {
        if (_exportState.value is ExportState.Generating) return // prevent double-tap (subtask 3.3)
        _exportState.value = ExportState.Generating // set immediately on Main to block double-tap

        viewModelScope.launch(Dispatchers.IO) {
            try {
                val range = _selectedRange.value
                val endDate = LocalDate.now()
                // Keep export date window aligned with on-screen analytics range.
                val startDate = endDate.minusDays((range.days - 1).toLong())
                val startStr = startDate.format(DATE_FORMATTER)
                val endStr = endDate.format(DATE_FORMATTER)

                // Collect current entries (one-shot snapshot via Flow.first())
                val entries = repository.getEntriesBetweenDates(startStr, endStr).first()

                // Get patient name from settings
                val settings = appSettingsRepository.getSettingsOnce()
                val patientName = settings?.patientName ?: ""

                // Calculate slot averages for the PDF
                val slotAverages = calculateSlotAverages(entries)

                // Generate PDF
                val pdfFile = pdfGenerator.generate(
                    patientName = patientName,
                    startDate = startDate,
                    endDate = endDate,
                    entries = entries,
                    slotAverages = slotAverages,
                    chartBitmap = chartBitmap
                )

                _exportState.value = ExportState.Preview(pdfFile)
            } catch (e: Exception) {
                _exportState.value = ExportState.Error(e.message ?: "PDF generation failed")
            }
        }
    }

    /**
     * Reset export state to Idle (e.g., when user navigates back from preview).
     */
    fun resetExportState() {
        _exportState.value = ExportState.Idle
    }

    companion object {
        private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        private const val TREND_THRESHOLD = 0.25

        /**
         * Convert a numeric average to the nearest Severity enum value.
         * Rounding: 0–0.49=NO_PAIN, 0.5–1.49=MILD, 1.5–2.49=MODERATE, 2.5–3.0=SEVERE
         */
        fun numericToSeverity(value: Double): Severity {
            return when {
                value < 0.5 -> Severity.NO_PAIN
                value < 1.5 -> Severity.MILD
                value < 2.5 -> Severity.MODERATE
                else -> Severity.SEVERE
            }
        }
    }
}
