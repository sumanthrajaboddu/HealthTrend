package com.healthtrend.app.data.export

import android.graphics.Bitmap
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import java.io.File
import java.time.LocalDate

/**
 * Fake PdfGenerator for unit testing AnalyticsViewModel export flow.
 * Returns a configurable File and tracks call count.
 */
class FakePdfGenerator : PdfGenerator {

    /** The file to return from generate(). Set before calling. */
    var resultFile: File = File.createTempFile("test_report", ".pdf")

    /** Set to true to make generate() throw an exception. */
    var shouldFail: Boolean = false

    /** Error message when shouldFail = true. */
    var errorMessage: String = "PDF generation failed"

    /** Number of times generate() was called. */
    var generateCallCount: Int = 0
        private set

    /** Last patientName passed to generate(). */
    var lastPatientName: String? = null
        private set

    /** Last entries passed to generate(). */
    var lastEntries: List<HealthEntry>? = null
        private set

    /** Last slotAverages passed to generate(). */
    var lastSlotAverages: Map<TimeSlot, Severity?>? = null
        private set

    override fun generate(
        patientName: String,
        startDate: LocalDate,
        endDate: LocalDate,
        entries: List<HealthEntry>,
        slotAverages: Map<TimeSlot, Severity?>,
        chartBitmap: Bitmap?
    ): File {
        generateCallCount++
        lastPatientName = patientName
        lastEntries = entries
        lastSlotAverages = slotAverages

        if (shouldFail) {
            throw RuntimeException(errorMessage)
        }

        return resultFile
    }
}
