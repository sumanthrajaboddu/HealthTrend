package com.healthtrend.app.data.export

import android.graphics.Bitmap
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import java.io.File
import java.time.LocalDate

/**
 * Generates PDF reports of symptom data (Story 6.1 Task 2).
 * Interface for testability — implementation in [AndroidPdfGenerator].
 */
interface PdfGenerator {
    /**
     * Generate a PDF report and save to cache directory.
     *
     * @param patientName patient name from AppSettings — blank if not set (AC #4)
     * @param startDate report start date
     * @param endDate report end date
     * @param entries health entries in the date range
     * @param slotAverages per-slot average severity (null = no data for slot)
     * @param chartBitmap optional pre-rendered chart bitmap (from Vico chart capture)
     * @return File pointing to the generated PDF in cache/reports/
     */
    fun generate(
        patientName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<HealthEntry>,
        slotAverages: Map<TimeSlot, Severity?>,
        chartBitmap: Bitmap? = null
    ): File
}
