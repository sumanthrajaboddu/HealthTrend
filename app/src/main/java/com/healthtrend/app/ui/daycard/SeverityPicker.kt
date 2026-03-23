package com.healthtrend.app.ui.daycard

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.adaptiveSoftColor

/**
 * Inline severity picker — NOT a modal, NOT a bottom sheet.
 * Shows 4 severity options with triple encoding (color + label + icon).
 * Touch targets 48dp+ per option, 8dp minimum gaps.
 * Selection registers on tap-up (default Compose clickable behavior — pointer cancellation).
 */
@Composable
fun SeverityPicker(
    currentSeverity: Severity?,
    onSeveritySelected: (Severity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val hapticFeedback = LocalHapticFeedback.current

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        // Dismiss row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            IconButton(
                onClick = onDismiss,
                modifier = Modifier
                    .size(48.dp)
                    .semantics {
                        contentDescription = "Close severity picker. Double tap to dismiss."
                        role = Role.Button
                    }
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // Severity options — distributed evenly
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Severity.entries.forEach { severity ->
                SeverityOption(
                    severity = severity,
                    isSelected = severity == currentSeverity,
                    onClick = {
                        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
                        onSeveritySelected(severity)
                    }
                )
            }
        }
    }
}

/**
 * Single severity option in the picker.
 * Triple encoding: color swatch (card bg) + label + icon.
 * 48dp+ touch target. Highlight border if currently selected.
 *
 * TalkBack: "{Severity}. Double tap to select." / "Selected" state for current.
 */
@Composable
private fun SeverityOption(
    severity: Severity,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val a11yDescription = "${severity.displayName}. Double tap to select."
    val a11yState = if (isSelected) "Selected" else ""

    Card(
        onClick = onClick,
        modifier = modifier
            .size(width = 64.dp, height = 64.dp)
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
                role = Role.Button
                if (isSelected) {
                    stateDescription = a11yState
                }
            },
        colors = CardDefaults.cardColors(containerColor = severity.adaptiveSoftColor),
        border = if (isSelected) {
            BorderStroke(2.dp, severity.color)
        } else {
            null
        },
        shape = MaterialTheme.shapes.small
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(4.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = severity.icon,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = severity.color
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = severity.displayName,
                style = MaterialTheme.typography.labelSmall,
                color = severity.color,
                maxLines = 1,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }
    }
}
