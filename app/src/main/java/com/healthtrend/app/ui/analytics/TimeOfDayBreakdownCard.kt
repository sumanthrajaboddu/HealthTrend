package com.healthtrend.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.model.adaptiveSoftColor

/**
 * A single breakdown card for one TimeSlot, showing the average severity
 * across the selected date range (Story 5.2).
 *
 * Triple encoding: severity softColor background + text label + icon (AC #2).
 * Empty state: "—" with neutral styling — no alarm, no encouragement (AC #4).
 *
 * @param timeSlot the time slot this card represents.
 * @param averageSeverity the computed average, or null if no entries for this slot.
 * @param periodDays the selected date range length in days for TalkBack.
 */
@Composable
fun TimeOfDayBreakdownCard(
    timeSlot: TimeSlot,
    averageSeverity: Severity?,
    periodDays: Int,
    modifier: Modifier = Modifier
) {
    // TalkBack: "[Slot] average: [Severity] over [period]." or "[Slot]: No data for this period."
    val talkBackDescription = if (averageSeverity != null) {
        "${timeSlot.displayName} average: ${averageSeverity.displayName} over $periodDays days."
    } else {
        "${timeSlot.displayName}: No data for this period."
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = talkBackDescription
            },
        colors = CardDefaults.cardColors(
            containerColor = averageSeverity?.adaptiveSoftColor
                ?: MaterialTheme.colorScheme.surfaceContainerLow
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
                .clearAndSetSemantics { },
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Slot icon
            Icon(
                imageVector = timeSlot.icon,
                contentDescription = null, // Handled by card-level semantics
                modifier = Modifier.size(24.dp),
                tint = averageSeverity?.color
                    ?: MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Slot name — from TimeSlot.displayName, NEVER hardcoded
                Text(
                    text = timeSlot.displayName,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                Spacer(modifier = Modifier.height(2.dp))

                if (averageSeverity != null) {
                    // Severity label — from Severity.displayName, NEVER hardcoded
                    Text(
                        text = averageSeverity.displayName,
                        style = MaterialTheme.typography.titleMedium,
                        color = averageSeverity.color
                    )
                } else {
                    // Empty state: neutral dash — no alarm, no encouragement
                    Text(
                        text = "—",
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Severity icon (triple encoding: color + text + icon)
            if (averageSeverity != null) {
                Icon(
                    imageVector = averageSeverity.icon,
                    contentDescription = null,
                    modifier = Modifier.size(20.dp),
                    tint = averageSeverity.color
                )
            }
        }
    }
}

/**
 * Layout for all four time-of-day breakdown cards in a 2x2 grid.
 * Placed below the trend chart on the Analytics screen (AC #1).
 * Cards sized consistently, responsive to screen width within 16dp margins.
 */
@Composable
fun TimeOfDayBreakdownSection(
    slotAverages: Map<TimeSlot, Severity?>,
    periodDays: Int,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // 2x2 grid layout
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeOfDayBreakdownCard(
                timeSlot = TimeSlot.MORNING,
                averageSeverity = slotAverages[TimeSlot.MORNING],
                periodDays = periodDays,
                modifier = Modifier.weight(1f)
            )
            TimeOfDayBreakdownCard(
                timeSlot = TimeSlot.AFTERNOON,
                averageSeverity = slotAverages[TimeSlot.AFTERNOON],
                periodDays = periodDays,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            TimeOfDayBreakdownCard(
                timeSlot = TimeSlot.EVENING,
                averageSeverity = slotAverages[TimeSlot.EVENING],
                periodDays = periodDays,
                modifier = Modifier.weight(1f)
            )
            TimeOfDayBreakdownCard(
                timeSlot = TimeSlot.NIGHT,
                averageSeverity = slotAverages[TimeSlot.NIGHT],
                periodDays = periodDays,
                modifier = Modifier.weight(1f)
            )
        }
    }
}
