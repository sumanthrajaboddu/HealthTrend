package com.healthtrend.app.data.model

import androidx.compose.ui.graphics.Color

/**
 * Severity levels for health entries.
 * This enum is the SINGLE SOURCE OF TRUTH for severity colors, labels, and numeric values.
 * NEVER hardcode severity colors elsewhere — always reference [Severity.color] or [Severity.softColor].
 */
enum class Severity(
    val numericValue: Int,
    val displayName: String,
    val color: Color,
    val softColor: Color
) {
    NO_PAIN(
        numericValue = 0,
        displayName = "No Pain",
        color = Color(0xFF4CAF50),      // Green
        softColor = Color(0xFFC8E6C9)   // Light Green
    ),
    MILD(
        numericValue = 1,
        displayName = "Mild",
        color = Color(0xFFFFC107),       // Amber
        softColor = Color(0xFFFFF9C4)    // Light Amber
    ),
    MODERATE(
        numericValue = 2,
        displayName = "Moderate",
        color = Color(0xFFFF9800),       // Orange
        softColor = Color(0xFFFFE0B2)    // Light Orange
    ),
    SEVERE(
        numericValue = 3,
        displayName = "Severe",
        color = Color(0xFFF44336),       // Red
        softColor = Color(0xFFFFCDD2)    // Light Red
    );
}
