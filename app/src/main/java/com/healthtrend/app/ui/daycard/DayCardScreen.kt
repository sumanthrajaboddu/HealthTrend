package com.healthtrend.app.ui.daycard

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.snap
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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.ui.theme.HealthTrendAnimation
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Day Card screen — shows daily time slot tiles for today's date.
 *
 * Layout (top to bottom):
 * 1. CenterAlignedTopAppBar with full date
 * 2. Vertical list of time slot tiles
 *
 * Uses hiltViewModel() — one ViewModel per screen.
 * Collects UiState with collectAsStateWithLifecycle() — NEVER collectAsState().
 *
 * @param scrollToTodayTrigger Incrementing counter from bottom nav "Today" re-tap.
 *   Triggers pager animation back to today (Story 2.1).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DayCardScreen(
    modifier: Modifier = Modifier,
    viewModel: DayCardViewModel = hiltViewModel(),
    scrollToTodayTrigger: Int = 0
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val weekDatesWithData by viewModel.weekDatesWithData.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val view = LocalView.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
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
        // Focus order: Top App Bar → Time Slot Tiles → Bottom Nav
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when (val state = uiState) {
                is DayCardUiState.Loading -> {
                    // No loading spinner per UX rules — Room data is near-instant
                    Box(modifier = Modifier.fillMaxSize())
                }

                is DayCardUiState.Success -> {
                    val today = viewModel.today
                    val initialPage = DatePagerUtils.dateToPageIndex(state.date, today)
                        .coerceIn(0, DatePagerUtils.pageCount - 1)
                    val pagerState = rememberPagerState(
                        initialPage = initialPage
                    ) { DatePagerUtils.pageCount }

                    val settledPage = pagerState.settledPage
                    var lastAnnouncedPage by rememberSaveable { mutableIntStateOf(settledPage) }

                    // Sync pager -> ViewModel selectedDate
                    LaunchedEffect(settledPage, today) {
                        viewModel.onDateSelected(
                            DatePagerUtils.pageIndexToDate(settledPage, today)
                        )
                    }

                    // TalkBack announces new date when page changes
                    LaunchedEffect(settledPage, today, view) {
                        if (settledPage != lastAnnouncedPage) {
                            val date = DatePagerUtils.pageIndexToDate(settledPage, today)
                            val spokenDate = date.format(
                                DateTimeFormatter.ofPattern("EEEE, MMMM d", Locale.getDefault())
                            )
                            view.announceForAccessibility(spokenDate)
                            lastAnnouncedPage = settledPage
                        }
                    }

                    // Sync ViewModel -> pager (e.g., week strip tap in Story 2.2)
                    LaunchedEffect(state.date, today, animationsEnabled) {
                        val targetPage = DatePagerUtils.dateToPageIndex(state.date, today)
                        if (pagerState.currentPage != targetPage) {
                            if (animationsEnabled) {
                                pagerState.animateScrollToPage(
                                    targetPage,
                                    animationSpec = HealthTrendAnimation.daySwipeSpec()
                                )
                            } else {
                                pagerState.scrollToPage(targetPage)
                            }
                        }
                    }

                    // "Today" tab re-tap: animate to today page
                    var lastProcessedTrigger by rememberSaveable { mutableIntStateOf(scrollToTodayTrigger) }
                    LaunchedEffect(scrollToTodayTrigger, animationsEnabled) {
                        if (scrollToTodayTrigger != lastProcessedTrigger) {
                            lastProcessedTrigger = scrollToTodayTrigger
                            if (pagerState.currentPage != DatePagerUtils.TODAY_PAGE_INDEX) {
                                if (animationsEnabled) {
                                    pagerState.animateScrollToPage(
                                        DatePagerUtils.TODAY_PAGE_INDEX,
                                        animationSpec = HealthTrendAnimation.daySwipeSpec()
                                    )
                                } else {
                                    pagerState.scrollToPage(DatePagerUtils.TODAY_PAGE_INDEX)
                                }
                            }
                        }
                    }

                    WeekStrip(
                        selectedDate = state.date,
                        today = today,
                        datesWithData = weekDatesWithData,
                        onDaySelected = { date -> viewModel.onDateSelected(date) },
                        onNavigateWeek = { forward -> viewModel.onNavigateWeek(forward) },
                        canNavigateForward = viewModel.canNavigateWeekForward(),
                        modifier = Modifier.fillMaxWidth()
                    )

                    HorizontalPager(
                        state = pagerState,
                        beyondViewportPageCount = 1,
                        flingBehavior = PagerDefaults.flingBehavior(
                            state = pagerState,
                            snapAnimationSpec = if (animationsEnabled) {
                                HealthTrendAnimation.daySwipeSpec()
                            } else {
                                snap()
                            }
                        ),
                        modifier = Modifier.fillMaxSize()
                    ) { page ->
                        val pageDate = DatePagerUtils.pageIndexToDate(page, today)
                        val pageState = if (pageDate == state.date) {
                            state
                        } else {
                            emptyDayCardState(pageDate, today)
                        }

                        DayCardContent(
                            state = pageState,
                            onTileClick = { slot -> viewModel.onTileClick(slot) },
                            onSeveritySelected = { slot, severity ->
                                viewModel.onSeveritySelected(slot, severity)
                            },
                            onDismissPicker = { viewModel.onDismissPicker() },
                            modifier = Modifier.fillMaxSize()
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

/**
 * Day Card content — vertical column of 4 TimeSlotTiles.
 * 12dp gaps between tiles. 16dp horizontal margins. Full-width single-column.
 */
@Composable
private fun DayCardContent(
    state: DayCardUiState.Success,
    onTileClick: (TimeSlot) -> Unit,
    onSeveritySelected: (TimeSlot, com.healthtrend.app.data.model.Severity) -> Unit,
    onDismissPicker: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val animationsEnabled = remember {
        Settings.Global.getFloat(
            context.contentResolver,
            Settings.Global.ANIMATOR_DURATION_SCALE,
            1f
        ) > 0f
    }

    val bloomScale = remember { Animatable(1f) }
    LaunchedEffect(state.allCompleteBloom, animationsEnabled) {
        if (state.allCompleteBloom && animationsEnabled) {
            bloomScale.snapTo(1f)
            bloomScale.animateTo(
                targetValue = 1f,
                animationSpec = keyframes {
                    durationMillis = HealthTrendAnimation.ALL_COMPLETE_BLOOM_MS
                    1.03f at (HealthTrendAnimation.ALL_COMPLETE_BLOOM_MS / 2)
                }
            )
        } else {
            bloomScale.snapTo(1f)
        }
    }

    val activeSlot = state.pickerOpenForSlot
    val dimNonActive = activeSlot != null

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(
            items = TimeSlot.entries.toList(),
            key = { it.name }
        ) { timeSlot ->
            val tileAlpha = if (dimNonActive && activeSlot != timeSlot) 0.3f else 1f
            TimeSlotTile(
                timeSlot = timeSlot,
                entry = state.entries[timeSlot],
                isCurrentTimeSlot = timeSlot == state.currentTimeSlot,
                isPickerOpen = state.pickerOpenForSlot == timeSlot,
                onTileClick = { onTileClick(timeSlot) },
                onSeveritySelected = { severity -> onSeveritySelected(timeSlot, severity) },
                onDismissPicker = onDismissPicker,
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
                        alpha = tileAlpha
                        scaleX = bloomScale.value
                        scaleY = bloomScale.value
                    }
            )
        }
    }
}

private fun emptyDayCardState(date: LocalDate, today: LocalDate): DayCardUiState.Success {
    val entries = TimeSlot.entries.associateWith { null }
    return DayCardUiState.Success(
        date = date,
        entries = entries,
        currentTimeSlot = null,
        pickerOpenForSlot = null,
        allCompleteBloom = false,
        isToday = date == today
    )
}
