package com.healthtrend.app.ui.analytics

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp

/**
 * Date range selector with three Material 3 filter chips: 1 Week, 1 Month, 3 Months.
 * Default selection: 1 Week (AC #1).
 * Tap chip → updates selectedRange in ViewModel → chart re-queries and redraws (AC #3).
 * Selected chip is visually distinct: filled vs outlined (AC #3).
 *
 * @param selectedRange the currently selected date range.
 * @param onRangeSelected callback when user taps a chip.
 */
@Composable
fun DateRangeSelector(
    selectedRange: DateRange,
    onRangeSelected: (DateRange) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        DateRange.entries.forEach { range ->
            val isSelected = range == selectedRange
            FilterChip(
                selected = isSelected,
                onClick = { onRangeSelected(range) },
                label = {
                    Text(
                        text = range.displayName,
                        style = MaterialTheme.typography.labelLarge
                    )
                },
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.semantics {
                    contentDescription = "${range.displayName}, ${if (isSelected) "selected" else "not selected"}. Double tap to select."
                    role = Role.RadioButton
                    stateDescription = if (isSelected) "selected" else "not selected"
                }
            )
        }
    }
}
