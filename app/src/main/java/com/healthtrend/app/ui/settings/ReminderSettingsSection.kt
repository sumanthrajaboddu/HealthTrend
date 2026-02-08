package com.healthtrend.app.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.healthtrend.app.data.model.TimeSlot

/**
 * Reminder configuration section for the Settings screen.
 *
 * - Global toggle: enables/disables all reminders (AC #1, #2)
 * - Per-slot rows: toggle + time picker for each TimeSlot (AC #1, #3, #4)
 * - Per-slot controls disabled/dimmed when global is off (AC #1)
 * - Labels from TimeSlot.displayName — NEVER hardcoded (AC #1)
 * - Full TalkBack accessibility (AC #6)
 */
@Composable
fun ReminderSettingsSection(
    globalRemindersEnabled: Boolean,
    slotReminders: List<SlotReminderState>,
    onGlobalToggled: (Boolean) -> Unit,
    onSlotToggled: (TimeSlot, Boolean) -> Unit,
    onSlotTimeChanged: (TimeSlot, Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Section heading
        Text(
            text = "Reminders",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.semantics { heading() }
        )

        // Global toggle (AC #1, #2, #6: TalkBack 4.1)
        GlobalReminderToggle(
            enabled = globalRemindersEnabled,
            onToggled = onGlobalToggled
        )

        // Per-slot rows (AC #1, #3, #4, #6: TalkBack 4.2, 4.3, 4.4)
        slotReminders.forEach { slotState ->
            SlotReminderRow(
                slotState = slotState,
                globalEnabled = globalRemindersEnabled,
                onToggled = { enabled -> onSlotToggled(slotState.timeSlot, enabled) },
                onTimeChanged = { hour, minute ->
                    onSlotTimeChanged(slotState.timeSlot, hour, minute)
                }
            )
        }
    }
}

/**
 * Global reminders toggle row.
 * TalkBack AC #6 → 4.1: "Reminders, [enabled/disabled]. Double tap to toggle."
 */
@Composable
private fun GlobalReminderToggle(
    enabled: Boolean,
    onToggled: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val statusText = if (enabled) "enabled" else "disabled"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = "Reminders, $statusText. Double tap to toggle."
            },
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Enable Reminders",
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.clearAndSetSemantics { }
        )
        Switch(
            checked = enabled,
            onCheckedChange = onToggled,
            modifier = Modifier.clearAndSetSemantics { }
        )
    }
}

/**
 * Per-slot reminder row: toggle + time picker button.
 *
 * TalkBack AC #6:
 * - 4.2: "[Slot] reminder, [enabled/disabled], [time]. Double tap to toggle."
 * - 4.3: "[Slot] reminder time, [time]. Double tap to change."
 * - 4.4: "[Slot] reminder, disabled. Enable global reminders first."
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SlotReminderRow(
    slotState: SlotReminderState,
    globalEnabled: Boolean,
    onToggled: (Boolean) -> Unit,
    onTimeChanged: (Int, Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val isInteractable = globalEnabled
    val dimAlpha = if (isInteractable) 1f else 0.38f

    var showTimePicker by remember { mutableStateOf(false) }

    // TalkBack announcement varies by state (AC #6)
    val toggleDescription = if (!globalEnabled) {
        "${slotState.timeSlot.displayName} reminder, disabled. Enable global reminders first."
    } else {
        val statusText = if (slotState.enabled) "enabled" else "disabled"
        "${slotState.timeSlot.displayName} reminder, $statusText, ${slotState.timeDisplay}. Double tap to toggle."
    }

    val timeDescription = if (globalEnabled && slotState.enabled) {
        "${slotState.timeSlot.displayName} reminder time, ${slotState.timeDisplay}. Double tap to change."
    } else {
        "${slotState.timeSlot.displayName} reminder time, ${slotState.timeDisplay}."
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(dimAlpha)
            .padding(start = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Slot name + time button
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = slotState.timeSlot.displayName,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = slotState.timeDisplay,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier
                    .then(
                        if (isInteractable && slotState.enabled) {
                            Modifier.clickable { showTimePicker = true }
                        } else {
                            Modifier
                        }
                    )
                    .semantics {
                        contentDescription = timeDescription
                    }
            )
        }

        // Slot toggle
        Switch(
            checked = slotState.enabled,
            onCheckedChange = { onToggled(it) },
            enabled = isInteractable,
            modifier = Modifier.semantics {
                contentDescription = toggleDescription
            }
        )
    }

    // Time Picker Dialog
    if (showTimePicker) {
        TimePickerDialog(
            initialHour = slotState.hour,
            initialMinute = slotState.minute,
            slotDisplayName = slotState.timeSlot.displayName,
            onConfirm = { hour, minute ->
                onTimeChanged(hour, minute)
                showTimePicker = false
            },
            onDismiss = { showTimePicker = false }
        )
    }
}

/**
 * Material 3 Time Picker wrapped in an AlertDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TimePickerDialog(
    initialHour: Int,
    initialMinute: Int,
    slotDisplayName: String,
    onConfirm: (Int, Int) -> Unit,
    onDismiss: () -> Unit
) {
    val timePickerState = rememberTimePickerState(
        initialHour = initialHour,
        initialMinute = initialMinute,
        is24Hour = false
    )

    androidx.compose.material3.AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = "$slotDisplayName Reminder Time",
                modifier = Modifier.semantics {
                    contentDescription = "Set $slotDisplayName reminder time"
                }
            )
        },
        text = {
            TimePicker(state = timePickerState)
        },
        confirmButton = {
            androidx.compose.material3.TextButton(
                onClick = { onConfirm(timePickerState.hour, timePickerState.minute) }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            androidx.compose.material3.TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
