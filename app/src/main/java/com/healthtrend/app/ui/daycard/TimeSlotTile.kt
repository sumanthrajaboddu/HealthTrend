package com.healthtrend.app.ui.daycard

import android.provider.Settings
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.model.adaptiveSoftColor
import com.healthtrend.app.ui.theme.HealthTrendAnimation

/**
 * A single time slot tile in the Day Card.
 *
 * Displays:
 * - Empty state: dash "—", neutral background, 64dp+ height
 * - Logged state: severity color background with 150ms bloom, severity text + icon (triple encoding)
 * - Current time slot: subtle border highlight
 * - Picker: inline expansion within the tile (200ms ease-out enter, 0ms exit)
 *
 * All text in sp. All dimensions in dp. Minimum height 64dp.
 * Touch target meets 48dp minimum (tile exceeds this).
 */
@Composable
fun TimeSlotTile(
    timeSlot: TimeSlot,
    entry: HealthEntry?,
    isCurrentTimeSlot: Boolean,
    isPickerOpen: Boolean,
    onTileClick: () -> Unit,
    onSeveritySelected: (Severity) -> Unit,
    onDismissPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isLogged = entry != null
    val neutralColor = MaterialTheme.colorScheme.surfaceContainerLow
    val targetColor = if (isLogged) entry!!.severity.adaptiveSoftColor else neutralColor

    // Check system animations setting
    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }

    // Color fill bloom: 150ms animated transition (or instant if animations disabled)
    val backgroundColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = if (animationsEnabled) {
            HealthTrendAnimation.colorFillBloomSpec()
        } else {
            snap()
        },
        label = "tileColorBloom"
    )

    val borderStroke = if (isCurrentTimeSlot) {
        BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
    } else {
        null
    }

    // TalkBack semantics — full tile gets a single descriptive announcement
    val semanticsDescription = buildTileSemantics(timeSlot, entry, isCurrentTimeSlot)

    Card(
        onClick = onTileClick,
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = 64.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = semanticsDescription
                if (isCurrentTimeSlot) {
                    stateDescription = "Current time slot"
                }
            },
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        border = borderStroke,
        shape = MaterialTheme.shapes.medium
    ) {
        // Normal tile content
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Left: Time slot icon + name
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clearAndSetSemantics { }
            ) {
                Icon(
                    imageVector = timeSlot.icon,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = if (isCurrentTimeSlot) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = timeSlot.displayName,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            // Right: Severity indicator or empty dash
            if (isLogged) {
                SeverityIndicator(severity = entry!!.severity)
            } else {
                Text(
                    text = "\u2014", // Em-dash
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.clearAndSetSemantics { }
                )
            }
        }

        // Inline severity picker — animated visibility
        AnimatedVisibility(
            visible = isPickerOpen,
            enter = if (animationsEnabled) {
                expandVertically(
                    animationSpec = tween(
                        durationMillis = HealthTrendAnimation.PICKER_EXPAND_MS,
                        easing = androidx.compose.animation.core.EaseOut
                    )
                )
            } else {
                expandVertically(animationSpec = snap())
            },
            exit = shrinkVertically(
                animationSpec = snap() // Always instant collapse (0ms) per spec
            )
        ) {
            SeverityPicker(
                currentSeverity = entry?.severity,
                onSeveritySelected = onSeveritySelected,
                onDismiss = onDismissPicker
            )
        }
    }
}

/**
 * Triple-encoded severity display: color + text label + icon.
 * NEVER color alone.
 */
@Composable
private fun SeverityIndicator(
    severity: Severity,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.clearAndSetSemantics { }
    ) {
        Icon(
            imageVector = severity.icon,
            contentDescription = null,
            modifier = Modifier.size(20.dp),
            tint = severity.color
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = severity.displayName,
            style = MaterialTheme.typography.labelLarge,
            color = severity.color
        )
    }
}

/**
 * Builds TalkBack-compliant content description for the tile.
 * Conveys purpose, state, and available action.
 */
private fun buildTileSemantics(
    timeSlot: TimeSlot,
    entry: HealthEntry?,
    isCurrentTimeSlot: Boolean
): String {
    val slotName = timeSlot.displayName
    val currentSlotPrefix = if (isCurrentTimeSlot) "Current time slot. " else ""

    return if (entry != null) {
        "${currentSlotPrefix}${slotName}, currently ${entry.severity.displayName}. Tap to change severity."
    } else {
        "${currentSlotPrefix}${slotName}, not logged. Double tap to log severity."
    }
}
