package com.healthtrend.app.ui.daycard

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale

/**
 * Week strip navigation composable — shows 7 day cells (Monday–Sunday) for the week
 * containing [selectedDate], with data indicator dots and left/right week arrows.
 *
 * Tap a day cell to navigate the pager. Arrows shift by one week.
 * All actions are tap-based — this IS the tap alternative for swipe navigation.
 *
 * Layout: [<] [M] [T] [W] [T] [F] [S] [S] [>]
 * Day cells: 44dp x 56dp minimum touch targets. Distributed evenly across width.
 */
@Composable
fun WeekStrip(
    selectedDate: LocalDate,
    today: LocalDate,
    datesWithData: Set<LocalDate>,
    onDaySelected: (LocalDate) -> Unit,
    onNavigateWeek: (forward: Boolean) -> Unit,
    canNavigateForward: Boolean,
    modifier: Modifier = Modifier
) {
    val weekDays = DatePagerUtils.weekDays(selectedDate)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left arrow — previous week
        IconButton(
            onClick = { onNavigateWeek(false) },
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "Previous week"
                    role = Role.Button
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        // 7 day cells — evenly distributed
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically
        ) {
            weekDays.forEach { date ->
                DayCell(
                    date = date,
                    isSelected = date == selectedDate,
                    isToday = date == today,
                    hasData = date in datesWithData,
                    isFuture = date.isAfter(today),
                    onClick = { onDaySelected(date) }
                )
            }
        }

        // Right arrow — next week (disabled when at latest week)
        IconButton(
            onClick = { onNavigateWeek(true) },
            enabled = canNavigateForward,
            modifier = Modifier
                .size(48.dp)
                .semantics {
                    contentDescription = "Next week"
                    role = Role.Button
                }
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = if (canNavigateForward) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.38f)
                }
            )
        }
    }
}

/**
 * Individual day cell in the week strip.
 *
 * Displays:
 * - Abbreviated day name (M, T, W...) from system locale
 * - Date number
 * - Data indicator dot (if [hasData])
 * - Today: bold + primary circle behind date number
 * - Selected: filled primary background behind date number
 * - Future dates: dimmed and non-interactive
 *
 * Minimum size: 44dp x 56dp (meets 48dp combined touch target).
 *
 * TalkBack: "[Day], [Full date], [today], [has data]. Double tap to view."
 */
@Composable
private fun DayCell(
    date: LocalDate,
    isSelected: Boolean,
    isToday: Boolean,
    hasData: Boolean,
    isFuture: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Build TalkBack description (AC #6)
    val a11yDescription = buildDayCellSemantics(date, isToday, hasData, isFuture)
    val a11yState = when {
        isSelected && isToday -> "Selected, today"
        isSelected -> "Selected"
        isToday -> "Today"
        else -> ""
    }

    // Abbreviated day name: M, T, W, T, F, S, S
    val dayAbbrev = date.dayOfWeek.getDisplayName(TextStyle.NARROW, Locale.getDefault())
    val dateNumber = date.dayOfMonth.toString()

    // Colors based on state
    val textColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        isSelected || isToday -> MaterialTheme.colorScheme.onPrimary
        else -> MaterialTheme.colorScheme.onSurface
    }
    val dayNameColor = when {
        isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
        modifier = modifier
            .widthIn(min = 44.dp)
            .height(56.dp)
            .clip(MaterialTheme.shapes.small)
            .then(
                if (!isFuture) {
                    Modifier.clickable(
                        onClick = onClick,
                        onClickLabel = "View day"
                    )
                } else {
                    Modifier
                }
            )
            .semantics(mergeDescendants = true) {
                contentDescription = a11yDescription
                role = Role.Button
                if (a11yState.isNotEmpty()) {
                    stateDescription = a11yState
                }
            }
    ) {
        // Day abbreviation
        Text(
            text = dayAbbrev,
            style = MaterialTheme.typography.labelSmall,
            color = dayNameColor,
            textAlign = TextAlign.Center,
            modifier = Modifier.clearAndSetSemantics { }
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Date number — with circle background for today/selected
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .size(28.dp)
                .then(
                    when {
                        isSelected -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primary)

                        isToday -> Modifier
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer)

                        else -> Modifier
                    }
                )
        ) {
            Text(
                text = dateNumber,
                style = MaterialTheme.typography.bodySmall.copy(
                    fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Normal
                ),
                color = when {
                    isSelected -> MaterialTheme.colorScheme.onPrimary
                    isToday -> MaterialTheme.colorScheme.onPrimaryContainer
                    isFuture -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    else -> MaterialTheme.colorScheme.onSurface
                },
                textAlign = TextAlign.Center,
                modifier = Modifier.clearAndSetSemantics { }
            )
        }

        Spacer(modifier = Modifier.height(2.dp))

        // Data indicator dot
        if (hasData && !isFuture) {
            Box(
                modifier = Modifier
                    .size(4.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary)
                    .clearAndSetSemantics { }
            )
        } else {
            // Spacer to maintain consistent cell height
            Spacer(modifier = Modifier.size(4.dp))
        }
    }
}

/**
 * Builds TalkBack-compliant content description for a day cell.
 * Format: "[Day], [Full date], [today indicator], [has data indicator]. Double tap to view."
 * Example: "Thursday, February 6, today, has data. Double tap to view."
 */
private fun buildDayCellSemantics(
    date: LocalDate,
    isToday: Boolean,
    hasData: Boolean,
    isFuture: Boolean
): String {
    val fullDate = date.format(
        DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
    )
    val todayPart = if (isToday) ", today" else ""
    val dataPart = if (hasData) ", has data" else ""
    val actionPart = if (isFuture) "" else ". Double tap to view."

    return "$fullDate$todayPart$dataPart$actionPart"
}
