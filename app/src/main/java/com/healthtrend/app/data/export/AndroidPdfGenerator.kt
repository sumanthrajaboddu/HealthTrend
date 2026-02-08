package com.healthtrend.app.data.export

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import androidx.compose.ui.graphics.toArgb
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.io.FileOutputStream
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Android PdfDocument-based implementation of [PdfGenerator] (Story 6.1 Task 2).
 * Data-layer utility — no UI logic.
 *
 * Sections: header (patient name + date range), trend chart,
 * time-of-day summary, daily log table.
 */
@Singleton
class AndroidPdfGenerator @Inject constructor(
    @ApplicationContext private val context: Context
) : PdfGenerator {

    override fun generate(
        patientName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<HealthEntry>,
        slotAverages: Map<TimeSlot, Severity?>,
        chartBitmap: Bitmap?
    ): File {
        val document = PdfDocument()

        try {
            var pageNumber = 1
            val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
            var page = document.startPage(pageInfo)
            var canvas = page.canvas
            var y = MARGIN

            // --- Header: patient name + date range (subtask 2.3, 2.6) ---
            y = drawHeader(canvas, patientName, startDate, endDate, y)
            y += SECTION_GAP

            // --- Chart (subtask 2.3) ---
            if (chartBitmap != null) {
                y = drawChartBitmap(canvas, chartBitmap, y)
                y += SECTION_GAP
            } else if (entries.isNotEmpty()) {
                // Fallback: draw simple chart from raw entry data
                y = drawSimpleChart(canvas, entries, y)
                y += SECTION_GAP
            }

            // --- Time-of-Day Summary (subtask 2.3) ---
            y = drawSlotSummary(canvas, slotAverages, y)
            y += SECTION_GAP

            // --- Daily Log Table (subtask 2.3, 2.4, 2.5, 2.7) ---
            val tableResult = drawDailyLogTable(document, page, canvas, entries, y, pageNumber)
            page = tableResult.page
            pageNumber = tableResult.pageNumber

            document.finishPage(page)

            // Write to cache/reports/ (subtask 2.9)
            val reportsDir = File(context.cacheDir, "reports")
            reportsDir.mkdirs()
            val fileName = "HealthTrend_Report_${startDate}_$endDate.pdf"
            val pdfFile = File(reportsDir, fileName)

            FileOutputStream(pdfFile).use { outputStream ->
                document.writeTo(outputStream)
            }

            return pdfFile
        } finally {
            document.close()
        }
    }

    // =========================================================================
    // Drawing helpers
    // =========================================================================

    /**
     * Draw report header: patient name + date range.
     * Blank patient name = blank header, NOT an error (AC #4, subtask 2.6).
     */
    private fun drawHeader(
        canvas: Canvas,
        patientName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        startY: Float
    ): Float {
        var y = startY

        // Patient name (large, bold)
        if (patientName.isNotBlank()) {
            canvas.drawText(patientName, MARGIN, y + titlePaint.textSize, titlePaint)
            y += titlePaint.textSize + 8f
        }

        // Date range subtitle
        val rangeText = "${startDate.format(DISPLAY_DATE_FORMATTER)} — ${endDate.format(DISPLAY_DATE_FORMATTER)}"
        canvas.drawText(rangeText, MARGIN, y + subtitlePaint.textSize, subtitlePaint)
        y += subtitlePaint.textSize + 4f

        // "HealthTrend Report" label
        canvas.drawText("HealthTrend Report", MARGIN, y + captionPaint.textSize, captionPaint)
        y += captionPaint.textSize

        // Divider line
        y += 8f
        canvas.drawLine(MARGIN, y, PAGE_WIDTH - MARGIN, y, dividerPaint)
        y += 4f

        return y
    }

    /**
     * Draw pre-rendered chart bitmap scaled to fit content width.
     */
    private fun drawChartBitmap(canvas: Canvas, bitmap: Bitmap, startY: Float): Float {
        val scale = CONTENT_WIDTH / bitmap.width.toFloat()
        val scaledHeight = bitmap.height * scale
        val destRect = Rect(
            MARGIN.toInt(),
            startY.toInt(),
            (MARGIN + CONTENT_WIDTH).toInt(),
            (startY + scaledHeight).toInt()
        )
        canvas.drawBitmap(bitmap, null, destRect, null)
        return startY + scaledHeight
    }

    /**
     * Draw a simple line chart from raw entry data.
     * Fallback when no pre-rendered bitmap is available.
     * Y-axis: 0=No Pain, 1=Mild, 2=Moderate, 3=Severe.
     */
    private fun drawSimpleChart(
        canvas: Canvas,
        entries: List<HealthEntry>,
        startY: Float
    ): Float {
        val chartHeight = 160f
        val chartLeft = MARGIN + 60f
        val chartRight = PAGE_WIDTH - MARGIN
        val chartWidth = chartRight - chartLeft
        val chartTop = startY + 16f
        val chartBottom = chartTop + chartHeight

        // Section title
        canvas.drawText("Severity Trend", MARGIN, startY + sectionTitlePaint.textSize, sectionTitlePaint)

        // Aggregate to daily max severity
        val dailyData = entries
            .groupBy { it.date }
            .map { (dateStr, dayEntries) ->
                LocalDate.parse(dateStr, DateTimeFormatter.ISO_LOCAL_DATE) to
                    dayEntries.maxOf { it.severity.numericValue }.toFloat()
            }
            .sortedBy { it.first }

        if (dailyData.isEmpty()) return chartBottom + 8f

        // Y-axis labels
        val yAxisLabelPaint = Paint(captionPaint).apply { textAlign = Paint.Align.RIGHT }
        for (severity in Severity.entries) {
            val yPos = chartBottom - (severity.numericValue / 3f) * chartHeight
            canvas.drawText(severity.displayName, chartLeft - 8f, yPos + 4f, yAxisLabelPaint)
            canvas.drawLine(chartLeft, yPos, chartRight, yPos, gridPaint)
        }

        // Axes
        canvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, dividerPaint)
        canvas.drawLine(chartLeft, chartBottom, chartRight, chartBottom, dividerPaint)

        // Plot data points
        val xStep = if (dailyData.size > 1) chartWidth / (dailyData.size - 1).toFloat() else 0f
        var prevX = 0f
        var prevY = 0f

        dailyData.forEachIndexed { index, (_, severity) ->
            val x = chartLeft + index * xStep
            val y = chartBottom - (severity / 3f) * chartHeight

            if (index > 0) {
                canvas.drawLine(prevX, prevY, x, y, chartLinePaint)
            }
            canvas.drawCircle(x, y, 3f, chartPointPaint)

            prevX = x
            prevY = y
        }

        // X-axis labels (first and last dates)
        val xLabelPaint = Paint(captionPaint).apply {
            textSize = 7f
            textAlign = Paint.Align.CENTER
        }
        canvas.drawText(
            dailyData.first().first.format(SHORT_DATE_FORMATTER),
            chartLeft, chartBottom + 12f, xLabelPaint
        )
        if (dailyData.size > 1) {
            canvas.drawText(
                dailyData.last().first.format(SHORT_DATE_FORMATTER),
                chartRight, chartBottom + 12f, xLabelPaint
            )
        }

        return chartBottom + 20f
    }

    /**
     * Draw time-of-day summary: 4 slot averages with labels and colors.
     */
    private fun drawSlotSummary(
        canvas: Canvas,
        slotAverages: Map<TimeSlot, Severity?>,
        startY: Float
    ): Float {
        var y = startY

        canvas.drawText("Time-of-Day Summary", MARGIN, y + sectionTitlePaint.textSize, sectionTitlePaint)
        y += sectionTitlePaint.textSize + 12f

        val slotWidth = CONTENT_WIDTH / 4f

        TimeSlot.entries.forEachIndexed { index, slot ->
            val x = MARGIN + index * slotWidth
            val severity = slotAverages[slot]

            // Slot label
            canvas.drawText(slot.displayName, x + 4f, y + tableCellPaint.textSize, tableHeaderPaint)

            // Severity value with color
            val severityText = severity?.displayName ?: "—"
            if (severity != null) {
                val colorPaint = Paint(tableCellPaint).apply {
                    color = severity.color.toArgb()
                    typeface = Typeface.DEFAULT_BOLD
                }
                canvas.drawText(severityText, x + 4f, y + tableCellPaint.textSize + 16f, colorPaint)
            } else {
                canvas.drawText(severityText, x + 4f, y + tableCellPaint.textSize + 16f, tableCellPaint)
            }
        }

        y += tableCellPaint.textSize + 24f
        return y
    }

    /**
     * Draw daily log table with page break support.
     * Columns: Date | Morning | Afternoon | Evening | Night.
     * Severity display names from Severity.displayName (subtask 2.4).
     * Color coding (subtask 2.5). Empty data = header only (subtask 2.7).
     */
    private fun drawDailyLogTable(
        document: PdfDocument,
        currentPage: PdfDocument.Page,
        currentCanvas: Canvas,
        entries: List<HealthEntry>,
        startY: Float,
        startPageNumber: Int
    ): PageState {
        var page = currentPage
        var canvas = currentCanvas
        var y = startY
        var pageNumber = startPageNumber

        canvas.drawText("Daily Log", MARGIN, y + sectionTitlePaint.textSize, sectionTitlePaint)
        y += sectionTitlePaint.textSize + 8f

        val colWidths = floatArrayOf(
            CONTENT_WIDTH * 0.22f,
            CONTENT_WIDTH * 0.195f,
            CONTENT_WIDTH * 0.195f,
            CONTENT_WIDTH * 0.195f,
            CONTENT_WIDTH * 0.195f
        )

        // Table header
        y = drawTableRow(
            canvas, y, colWidths,
            arrayOf("Date", "Morning", "Afternoon", "Evening", "Night"),
            isHeader = true
        )

        // Group entries by date
        val entriesByDate = entries.groupBy { it.date }
        val sortedDates = entriesByDate.keys
            .map { LocalDate.parse(it, DateTimeFormatter.ISO_LOCAL_DATE) }
            .sorted()

        for (date in sortedDates) {
            // Page break if needed
            if (y + ROW_HEIGHT > PAGE_HEIGHT - MARGIN) {
                document.finishPage(page)
                pageNumber++
                val pageInfo = PdfDocument.PageInfo.Builder(PAGE_WIDTH, PAGE_HEIGHT, pageNumber).create()
                page = document.startPage(pageInfo)
                canvas = page.canvas
                y = MARGIN

                // Repeat table header on new page
                y = drawTableRow(
                    canvas, y, colWidths,
                    arrayOf("Date", "Morning", "Afternoon", "Evening", "Night"),
                    isHeader = true
                )
            }

            val dateStr = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
            val dayEntries = entriesByDate[dateStr] ?: emptyList()
            val entryMap = dayEntries.associateBy { it.timeSlot }

            val cells = arrayOf(
                date.format(TABLE_DATE_FORMATTER),
                entryMap[TimeSlot.MORNING]?.severity?.displayName ?: "—",
                entryMap[TimeSlot.AFTERNOON]?.severity?.displayName ?: "—",
                entryMap[TimeSlot.EVENING]?.severity?.displayName ?: "—",
                entryMap[TimeSlot.NIGHT]?.severity?.displayName ?: "—"
            )

            val cellColors = arrayOf(
                null,
                entryMap[TimeSlot.MORNING]?.severity,
                entryMap[TimeSlot.AFTERNOON]?.severity,
                entryMap[TimeSlot.EVENING]?.severity,
                entryMap[TimeSlot.NIGHT]?.severity
            )

            y = drawTableRow(canvas, y, colWidths, cells, isHeader = false, cellSeverities = cellColors)
        }

        return PageState(page, canvas, pageNumber)
    }

    /**
     * Draw a single table row (header or data).
     */
    private fun drawTableRow(
        canvas: Canvas,
        startY: Float,
        colWidths: FloatArray,
        cells: Array<String>,
        isHeader: Boolean,
        cellSeverities: Array<Severity?>? = null
    ): Float {
        var x = MARGIN

        // Header background
        if (isHeader) {
            val bgPaint = Paint().apply {
                color = android.graphics.Color.rgb(240, 240, 240)
                style = Paint.Style.FILL
            }
            canvas.drawRect(MARGIN, startY, PAGE_WIDTH - MARGIN, startY + ROW_HEIGHT, bgPaint)
        }

        // Draw cells
        cells.forEachIndexed { index, text ->
            val paint = if (isHeader) {
                tableHeaderPaint
            } else {
                val severity = cellSeverities?.getOrNull(index)
                if (severity != null) {
                    Paint(tableCellPaint).apply { color = severity.color.toArgb() }
                } else {
                    tableCellPaint
                }
            }

            canvas.drawText(text, x + 4f, startY + ROW_HEIGHT - 6f, paint)

            if (index < cells.size - 1) {
                x += colWidths[index]
                canvas.drawLine(x, startY, x, startY + ROW_HEIGHT, gridPaint)
            }
        }

        // Bottom border
        canvas.drawLine(MARGIN, startY + ROW_HEIGHT, PAGE_WIDTH - MARGIN, startY + ROW_HEIGHT, gridPaint)

        return startY + ROW_HEIGHT
    }

    // =========================================================================
    // Page state for multi-page rendering
    // =========================================================================

    private data class PageState(
        val page: PdfDocument.Page,
        val canvas: Canvas,
        val pageNumber: Int
    )

    // =========================================================================
    // Paint objects (reusable, avoid GC pressure)
    // =========================================================================

    private val titlePaint = Paint().apply {
        textSize = 20f
        typeface = Typeface.DEFAULT_BOLD
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }

    private val subtitlePaint = Paint().apply {
        textSize = 12f
        typeface = Typeface.DEFAULT
        color = android.graphics.Color.DKGRAY
        isAntiAlias = true
    }

    private val captionPaint = Paint().apply {
        textSize = 9f
        typeface = Typeface.DEFAULT
        color = android.graphics.Color.GRAY
        isAntiAlias = true
    }

    private val sectionTitlePaint = Paint().apply {
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }

    private val tableHeaderPaint = Paint().apply {
        textSize = 9f
        typeface = Typeface.DEFAULT_BOLD
        color = android.graphics.Color.BLACK
        isAntiAlias = true
    }

    private val tableCellPaint = Paint().apply {
        textSize = 9f
        typeface = Typeface.DEFAULT
        color = android.graphics.Color.DKGRAY
        isAntiAlias = true
    }

    private val dividerPaint = Paint().apply {
        color = android.graphics.Color.rgb(200, 200, 200)
        strokeWidth = 1f
        isAntiAlias = true
    }

    private val gridPaint = Paint().apply {
        color = android.graphics.Color.rgb(220, 220, 220)
        strokeWidth = 0.5f
        isAntiAlias = true
    }

    private val chartLinePaint = Paint().apply {
        color = Severity.MODERATE.color.toArgb()
        strokeWidth = 2f
        style = Paint.Style.STROKE
        isAntiAlias = true
    }

    private val chartPointPaint = Paint().apply {
        color = Severity.MODERATE.color.toArgb()
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    companion object {
        private const val PAGE_WIDTH = 595
        private const val PAGE_HEIGHT = 842
        private const val MARGIN = 40f
        private const val CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN
        private const val SECTION_GAP = 20f
        private const val ROW_HEIGHT = 20f

        private val DISPLAY_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d, yyyy")
        private val SHORT_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M/d")
        private val TABLE_DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("MMM d")
    }
}
