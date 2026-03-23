package com.healthtrend.app.data.model

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SentimentNeutral
import androidx.compose.material.icons.filled.SentimentSatisfied
import androidx.compose.material.icons.filled.SentimentVeryDissatisfied
import androidx.compose.material.icons.filled.SentimentVerySatisfied
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Severity levels for health entries.
 * This enum is the SINGLE SOURCE OF TRUTH for severity colors, labels, and numeric values.
 * NEVER hardcode severity colors elsewhere — always reference [Severity.color] or [Severity.softColor].
 *
 * For theme-aware tile backgrounds, use [Severity.adaptiveSoftColor] in @Composable functions
 * instead of accessing [softColor] directly — it returns [softColorDark] when in dark theme.
 */
enum class Severity(
    val numericValue: Int,
    val displayName: String,
    val color: Color,
    val softColor: Color,
    val softColorDark: Color,
    val icon: ImageVector
) {
    NO_PAIN(
        numericValue = 0,
        displayName = "No Pain",
        color = Color(0xFF4CAF50),           // Green
        softColor = Color(0xFFE8F5E9),        // Soft Green (light)
        softColorDark = Color(0xFF1B3A2A),    // Deep forest green (dark)
        icon = Icons.Filled.SentimentVerySatisfied
    ),
    MILD(
        numericValue = 1,
        displayName = "Mild",
        color = Color(0xFFFFC107),            // Amber
        softColor = Color(0xFFFFF8E1),         // Soft Amber (light)
        softColorDark = Color(0xFF3A3420),     // Warm dark amber (dark)
        icon = Icons.Filled.SentimentSatisfied
    ),
    MODERATE(
        numericValue = 2,
        displayName = "Moderate",
        color = Color(0xFFFF9800),            // Orange
        softColor = Color(0xFFFFF3E0),         // Soft Orange (light)
        softColorDark = Color(0xFF3A2A1B),     // Dark clay (dark)
        icon = Icons.Filled.SentimentNeutral
    ),
    SEVERE(
        numericValue = 3,
        displayName = "Severe",
        color = Color(0xFFF44336),            // Red
        softColor = Color(0xFFFFEBEE),         // Soft Red (light)
        softColorDark = Color(0xFF3A1C1C),     // Deep maroon (dark)
        icon = Icons.Filled.SentimentVeryDissatisfied
    );
}

/**
 * Returns [softColor] in light theme or [softColorDark] in dark theme.
 * Use this in @Composable functions for theme-aware tile/card backgrounds.
 */
val Severity.adaptiveSoftColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) softColorDark else softColor
