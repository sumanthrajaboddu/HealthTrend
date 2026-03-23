package com.healthtrend.app.ui.settings

import android.content.Intent

/**
 * Builds the share intent for the Google Sheet URL.
 * AC #5 (Story 3.1): Intent.ACTION_SEND with Sheet URL as content.
 * Extracted for unit testing.
 */
object ShareUtils {

    /**
     * Creates an Intent suitable for sharing the Sheet URL via the system share sheet.
     */
    fun createShareSheetIntent(sheetUrl: String): Intent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, sheetUrl)
        type = "text/plain"
    }
}
