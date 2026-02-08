package com.healthtrend.app.ui.daycard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerDefaults
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.ui.theme.HealthTrendAnimation
import kotlinx.coroutines.flow.drop
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Day Card screen — shows daily time slot tiles with horizontal pager for date navigation
 * and a week strip for tap-based navigation with data indicators.
 *
 * Layout (top to bottom):
 * 1. CenterAlignedTopAppBar with full date
 * 2. WeekStrip — 7 day cells + arrows
 * 3. HorizontalPager — swipeable Day Card content
 *
 * Uses hiltViewModel() — one ViewModel per screen.
 * Collects UiState with collectAsStateWithLifecycle() — NEVER collectAsState().
 *
 * @param scrollToTodayTrigger Incrementing counter that triggers pager animation to today.
 *   Controlled by bottom nav "Today" tab re-tap.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCardScreen(
    modifier: Modifier = Modifier,
    viewModel: DayCardViewModel = hiltViewModel(),
    scrollToTodayTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val selectedDate by viewModel.selectedDate.collectAsStateWithLifecycle()
    val weekDatesWithData by viewModel.weekDatesWithData.collectAsStateWithLifecycle()
    val today = viewModel.today
    val view = LocalView.current

    val pagerState = rememberPagerState(
        initialPage = DatePagerUtils.TODAY_PAGE_INDEX,
        pageCount = { DatePagerUtils.pageCount }
    )

    // Pager → ViewModel: sync selected date when pager settles on a new page
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .collect { page ->
                val date = DatePagerUtils.pageIndexToDate(page, today)
                viewModel.onDateSelected(date)
            }
    }

    // ViewModel → Pager: animate pager when selectedDate changes programmatically
    // (from week strip tap or week arrow navigation)
    LaunchedEffect(selectedDate) {
        val targetPage = DatePagerUtils.dateToPageIndex(selectedDate, today)
        if (pagerState.settledPage != targetPage) {
            pagerState.animateScrollToPage(targetPage)
        }
    }

    // "Today" tab re-tap: animate pager to today's page (Story 2.1 AC #4)
    var lastProcessedTrigger by rememberSaveable { mutableIntStateOf(0) }
    LaunchedEffect(scrollToTodayTrigger) {
        if (scrollToTodayTrigger > lastProcessedTrigger) {
            lastProcessedTrigger = scrollToTodayTrigger
            pagerState.animateScrollToPage(DatePagerUtils.TODAY_PAGE_INDEX)
        }
    }

    // TalkBack: announce new date on page change, skip initial (Story 2.1 AC #6)
    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }
            .drop(1) // Skip initial page — only announce on user-initiated changes
            .collect { page ->
                val date = DatePagerUtils.pageIndexToDate(page, today)
                val announcement = date.format(
                    DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                )
                @Suppress("DEPRECATION")
                view.announceForAccessibility(announcement)
            }
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    // Full date format: "Saturday, February 7, 2026"
                    val dateText = when (val state = uiState) {
                        is DayCardUiState.Success -> state.date.format(
                            DateTimeFormatter.ofPattern(
                                "EEEE, MMMM d, yyyy",
                                Locale.getDefault()
                            )
                        )
                        else -> ""
                    }
                    // Subtle visual distinction for past dates
                    val isToday = (uiState as? DayCardUiState.Success)?.isToday ?: true
                    Text(
                        text = dateText,
                        style = MaterialTheme.typography.titleLarge,
                        color = if (isToday) {
                            MaterialTheme.colorScheme.onSurface
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.semantics { heading() }
                    )
                }
            )
        },
        modifier = modifier
    ) { innerPadding ->
        // Focus order: Top App Bar → Week Strip → Time Slot Tiles → Bottom Nav
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Week strip — tap-based navigation with data indicators (Story 2.2)
            WeekStrip(
                selectedDate = selectedDate,
                today = today,
                datesWithData = weekDatesWithData,
                onDaySelected = { date -> viewModel.onDateSelected(date) },
                onNavigateWeek = { forward -> viewModel.onNavigateWeek(forward) },
                canNavigateForward = viewModel.canNavigateWeekForward(),
                modifier = Modifier.fillMaxWidth()
            )

            HorizontalDivider(
                color = MaterialTheme.colorScheme.outlineVariant,
                thickness = 0.5.dp
            )

            // Horizontal pager — swipeable Day Card content
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f),
                beyondViewportPageCount = 1,
                flingBehavior = PagerDefaults.flingBehavior(
                    state = pagerState,
                    snapAnimationSpec = HealthTrendAnimation.daySwipeSpec()
                )
            ) { page ->
                val pageDate = DatePagerUtils.pageIndexToDate(page, today)

                when (val state = uiState) {
                    is DayCardUiState.Loading -> {
                        // No loading spinner per UX rules — Room data is near-instant
                        Box(modifier = Modifier.fillMaxSize())
                    }

                    is DayCardUiState.Success -> {
                        if (pageDate == state.date) {
                            // Active page — show loaded entries
                            DayCardContent(state = state)
                        } else {
                            // Adjacent page during swipe — show empty slots with dashes.
                            // Room is near-instant so this transitions to loaded data
                            // imperceptibly when the pager settles.
                            DayCardContent(
                                state = DayCardUiState.Success(
                                    date = pageDate,
                                    entries = TimeSlot.entries.associateWith { null },
                                    currentTimeSlot = null,
                                    isToday = pageDate == today
                                )
                            )
                        }
                    }

                    is DayCardUiState.Error -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = state.message,
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Day Card content — vertical column of 4 TimeSlotTiles.
 * 12dp gaps between tiles. 16dp horizontal margins. Full-width single-column.
 */
@Composable
private fun DayCardContent(
    state: DayCardUiState.Success,
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = TimeSlot.entries.toList(),
            key = { it.name }
        ) { timeSlot ->
            TimeSlotTile(
                timeSlot = timeSlot,
                entry = state.entries[timeSlot],
                isCurrentTimeSlot = timeSlot == state.currentTimeSlot,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}
