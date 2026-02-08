package com.healthtrend.app.data.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.WbTwilight
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.ui.graphics.vector.ImageVector
import java.time.LocalTime

/**
 * Time slots for daily health entries.
 * This enum is the SINGLE SOURCE OF TRUTH for time slot labels, icons, and default reminder times.
 * NEVER hardcode time slot labels elsewhere — always reference [TimeSlot.displayName].
 */
enum class TimeSlot(
    val displayName: String,
    val icon: ImageVector,
    val defaultReminderTime: LocalTime
) {
    MORNING(
        displayName = "Morning",
        icon = Icons.Filled.WbSunny,
        defaultReminderTime = LocalTime.of(8, 0)
    ),
    AFTERNOON(
        displayName = "Afternoon",
        icon = Icons.Filled.LightMode,
        defaultReminderTime = LocalTime.of(13, 0)
    ),
    EVENING(
        displayName = "Evening",
        icon = Icons.Filled.WbTwilight,
        defaultReminderTime = LocalTime.of(18, 0)
    ),
    NIGHT(
        displayName = "Night",
        icon = Icons.Filled.DarkMode,
        defaultReminderTime = LocalTime.of(22, 0)
    );
}
