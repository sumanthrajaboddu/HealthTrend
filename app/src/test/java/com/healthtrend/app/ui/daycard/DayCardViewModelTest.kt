package com.healthtrend.app.ui.daycard

import app.cash.turbine.test
import com.healthtrend.app.data.local.FakeHealthEntryDao
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.HealthEntryRepository
import com.healthtrend.app.data.sync.FakeSyncTrigger
import com.healthtrend.app.util.FakeTimeProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate

/**
 * Unit tests for DayCardViewModel.
 * Uses FakeHealthEntryDao + FakeTimeProvider for deterministic testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DayCardViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeHealthEntryDao
    private lateinit var repository: HealthEntryRepository

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeHealthEntryDao()
        repository = HealthEntryRepository(fakeDao, FakeSyncTrigger())
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    // ===================================================================
    // Initial State Tests
    // ===================================================================

    @Test
    fun `initial state is Success with empty entries for today`() = runTest {
        val timeProvider = FakeTimeProvider(
            date = LocalDate.of(2026, 2, 8),
            hour = 14
        )
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem()
            assertTrue("Expected Success, got $state", state is DayCardUiState.Success)
            val success = state as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 2, 8), success.date)
            assertEquals(4, success.entries.size)
            assertTrue(success.entries.values.all { it == null })
        }
    }

    @Test
    fun `success state contains all four time slots`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 10)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertTrue(state.entries.containsKey(TimeSlot.MORNING))
            assertTrue(state.entries.containsKey(TimeSlot.AFTERNOON))
            assertTrue(state.entries.containsKey(TimeSlot.EVENING))
            assertTrue(state.entries.containsKey(TimeSlot.NIGHT))
        }
    }

    // ===================================================================
    // Current Time Slot Highlight Tests
    // ===================================================================

    @Test
    fun `morning time slot highlighted at 8 AM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 8)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.MORNING, state.currentTimeSlot)
        }
    }

    @Test
    fun `afternoon time slot highlighted at 2 30 PM (hour 14)`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.AFTERNOON, state.currentTimeSlot)
        }
    }

    @Test
    fun `evening time slot highlighted at 6 PM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 18)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.EVENING, state.currentTimeSlot)
        }
    }

    @Test
    fun `night time slot highlighted at 10 PM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 22)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.NIGHT, state.currentTimeSlot)
        }
    }

    @Test
    fun `night time slot highlighted at 3 AM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 3)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.NIGHT, state.currentTimeSlot)
        }
    }

    @Test
    fun `morning boundary at 6 AM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 6)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.MORNING, state.currentTimeSlot)
        }
    }

    @Test
    fun `afternoon boundary at 12 PM`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 12)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.AFTERNOON, state.currentTimeSlot)
        }
    }

    @Test
    fun `evening boundary at 17`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 17)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.EVENING, state.currentTimeSlot)
        }
    }

    @Test
    fun `night boundary at 21`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 21)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(TimeSlot.NIGHT, state.currentTimeSlot)
        }
    }

    // ===================================================================
    // Entry Loading Tests
    // ===================================================================

    @Test
    fun `entries from repository appear in ui state`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 10)

        fakeDao.insert(
            HealthEntry(
                date = "2026-02-08",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.MILD
            )
        )

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            val morningEntry = state.entries[TimeSlot.MORNING]
            assertNotNull(morningEntry)
            assertEquals(Severity.MILD, morningEntry!!.severity)
        }
    }

    @Test
    fun `entries for different date do not appear`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 10)

        fakeDao.insert(
            HealthEntry(
                date = "2026-02-07",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.SEVERE
            )
        )

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertNull(state.entries[TimeSlot.MORNING])
        }
    }

    @Test
    fun `multiple entries for same date are mapped correctly`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 10)

        fakeDao.insert(HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD))
        fakeDao.insert(HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.EVENING, severity = Severity.SEVERE))

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(Severity.MILD, state.entries[TimeSlot.MORNING]?.severity)
            assertNull(state.entries[TimeSlot.AFTERNOON])
            assertEquals(Severity.SEVERE, state.entries[TimeSlot.EVENING]?.severity)
            assertNull(state.entries[TimeSlot.NIGHT])
        }
    }

    @Test
    fun `date is formatted as ISO local date for repository query`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 1, 5), hour = 10)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 1, 5), state.date)
        }
    }

    // ===================================================================
    // Multi-Date Navigation Tests (Story 2.1)
    // ===================================================================

    @Test
    fun `selectedDate initial value is today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 2, 8), awaitItem())
        }
    }

    @Test
    fun `today property matches timeProvider date`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 3, 15), hour = 10)
        val viewModel = DayCardViewModel(repository, timeProvider)

        assertEquals(LocalDate.of(2026, 3, 15), viewModel.today)
    }

    @Test
    fun `onDateSelected changes selectedDate`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)
        val yesterday = LocalDate.of(2026, 2, 7)

        viewModel.onDateSelected(yesterday)

        viewModel.selectedDate.test {
            assertEquals(yesterday, awaitItem())
        }
    }

    @Test
    fun `onDateSelected clamps future date to today`() = runTest {
        val today = LocalDate.of(2026, 2, 8)
        val timeProvider = FakeTimeProvider(date = today, hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(today.plusDays(1))

        viewModel.selectedDate.test {
            assertEquals(today, awaitItem())
        }
    }

    @Test
    fun `onDateSelected with same date is no-op`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 8))

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 2, 8), awaitItem())
        }
    }

    @Test
    fun `onDateSelected loads entries for selected date`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)

        fakeDao.insert(
            HealthEntry(
                date = "2026-02-07",
                timeSlot = TimeSlot.MORNING,
                severity = Severity.SEVERE
            )
        )

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val todayState = awaitItem() as DayCardUiState.Success
            assertNull(todayState.entries[TimeSlot.MORNING])
        }

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 2, 7), state.date)
            assertEquals(Severity.SEVERE, state.entries[TimeSlot.MORNING]?.severity)
        }
    }

    @Test
    fun `navigating to empty past date shows all null entries`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 1, 1))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 1, 1), state.date)
            assertTrue(state.entries.values.all { it == null })
        }
    }

    @Test
    fun `navigating back to today shows today entries`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)

        fakeDao.insert(
            HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.AFTERNOON, severity = Severity.MILD)
        )

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 5))
        viewModel.onDateSelected(LocalDate.of(2026, 2, 8))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 2, 8), state.date)
            assertEquals(Severity.MILD, state.entries[TimeSlot.AFTERNOON]?.severity)
        }
    }

    // --- isToday Flag Tests ---

    @Test
    fun `isToday is true when viewing today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertTrue(state.isToday)
        }
    }

    @Test
    fun `isToday is false for past date`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertFalse(state.isToday)
        }
    }

    @Test
    fun `isToday becomes true again when navigating back to today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 5))
        viewModel.onDateSelected(LocalDate.of(2026, 2, 8))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertTrue(state.isToday)
        }
    }

    // --- Current Time Slot for Multi-Date ---

    @Test
    fun `currentTimeSlot is null for past dates`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertNull(state.currentTimeSlot)
        }
    }

    @Test
    fun `currentTimeSlot is non-null for today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.uiState.test {
            val state = awaitItem() as DayCardUiState.Success
            assertNotNull(state.currentTimeSlot)
            assertEquals(TimeSlot.AFTERNOON, state.currentTimeSlot)
        }
    }

    @Test
    fun `currentTimeSlot restored when navigating back to today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 10)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 5))
        viewModel.onDateSelected(LocalDate.of(2026, 2, 8))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(TimeSlot.MORNING, state.currentTimeSlot)
        }
    }

    @Test
    fun `picker closes on date change`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onTileClick(TimeSlot.MORNING)

        viewModel.uiState.test {
            val stateWithPicker = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(TimeSlot.MORNING, stateWithPicker.pickerOpenForSlot)
        }

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertNull(state.pickerOpenForSlot)
        }
    }

    @Test
    fun `severity selected on past date saves correctly`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))
        viewModel.onSeveritySelected(TimeSlot.MORNING, Severity.MODERATE)

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 2, 7), state.date)
            assertEquals(Severity.MODERATE, state.entries[TimeSlot.MORNING]?.severity)
        }
    }

    @Test
    fun `severity saved on past date does not appear on today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 7))
        viewModel.onSeveritySelected(TimeSlot.MORNING, Severity.SEVERE)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 8))

        viewModel.uiState.test {
            val state = expectMostRecentItem() as DayCardUiState.Success
            assertEquals(LocalDate.of(2026, 2, 8), state.date)
            assertNull(state.entries[TimeSlot.MORNING])
        }
    }

    // ===================================================================
    // Week Data Indicators Tests (Story 2.2)
    // ===================================================================

    @Test
    fun `weekDatesWithData initially empty`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.weekDatesWithData.test {
            val dates = awaitItem()
            assertTrue(dates.isEmpty())
        }
    }

    @Test
    fun `weekDatesWithData includes dates with entries in current week`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)

        // Feb 8 is Sunday. Week is Mon Feb 2 - Sun Feb 8.
        fakeDao.insert(HealthEntry(date = "2026-02-03", timeSlot = TimeSlot.MORNING, severity = Severity.MILD))
        fakeDao.insert(HealthEntry(date = "2026-02-05", timeSlot = TimeSlot.AFTERNOON, severity = Severity.SEVERE))

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.weekDatesWithData.test {
            val dates = expectMostRecentItem()
            assertTrue(dates.contains(LocalDate.of(2026, 2, 3)))
            assertTrue(dates.contains(LocalDate.of(2026, 2, 5)))
            assertEquals(2, dates.size)
        }
    }

    @Test
    fun `weekDatesWithData excludes dates outside current week`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)

        // Entry from previous week
        fakeDao.insert(HealthEntry(date = "2026-01-30", timeSlot = TimeSlot.MORNING, severity = Severity.MILD))

        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.weekDatesWithData.test {
            val dates = expectMostRecentItem()
            assertFalse(dates.contains(LocalDate.of(2026, 1, 30)))
        }
    }

    @Test
    fun `weekDatesWithData updates when navigating to different week`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)

        // Entry in previous week (Jan 26 - Feb 1)
        fakeDao.insert(HealthEntry(date = "2026-01-28", timeSlot = TimeSlot.MORNING, severity = Severity.MILD))

        val viewModel = DayCardViewModel(repository, timeProvider)

        // Navigate to previous week
        viewModel.onDateSelected(LocalDate.of(2026, 1, 28))

        viewModel.weekDatesWithData.test {
            val dates = expectMostRecentItem()
            assertTrue(dates.contains(LocalDate.of(2026, 1, 28)))
        }
    }

    @Test
    fun `weekDatesWithData updates reactively when entry added`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        // Initially empty
        viewModel.weekDatesWithData.test {
            assertTrue(expectMostRecentItem().isEmpty())
        }

        // Add entry for today (which is in the current week)
        fakeDao.insert(HealthEntry(date = "2026-02-08", timeSlot = TimeSlot.MORNING, severity = Severity.MILD))

        viewModel.weekDatesWithData.test {
            val dates = expectMostRecentItem()
            assertTrue(dates.contains(LocalDate.of(2026, 2, 8)))
        }
    }

    // ===================================================================
    // Week Navigation Tests (Story 2.2)
    // ===================================================================

    @Test
    fun `onNavigateWeek backward shifts selectedDate by 7 days`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onNavigateWeek(forward = false)

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 2, 1), awaitItem())
        }
    }

    @Test
    fun `onNavigateWeek forward shifts selectedDate by 7 days when possible`() = runTest {
        // Start on Feb 1, today is Feb 8
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 1, 25))
        viewModel.onNavigateWeek(forward = true)

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 2, 1), awaitItem())
        }
    }

    @Test
    fun `onNavigateWeek forward caps at today`() = runTest {
        // Today is Feb 8 (Sunday). Navigate forward from Feb 5 (Thursday).
        // Feb 5 + 7 = Feb 12 > today (Feb 8), so cap at Feb 8.
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 5))
        viewModel.onNavigateWeek(forward = true)

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 2, 8), awaitItem())
        }
    }

    @Test
    fun `onNavigateWeek forward is no-op when already on today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onNavigateWeek(forward = true)

        viewModel.selectedDate.test {
            // Should still be today — capped and same date
            assertEquals(LocalDate.of(2026, 2, 8), awaitItem())
        }
    }

    @Test
    fun `canNavigateWeekForward is false when on today`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        assertFalse(viewModel.canNavigateWeekForward())
    }

    @Test
    fun `canNavigateWeekForward is true when on past date in different week`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 1, 25))

        assertTrue(viewModel.canNavigateWeekForward())
    }

    @Test
    fun `canNavigateWeekForward is true when on past date in same week`() = runTest {
        // Feb 5 (Thursday) is in same week as today Feb 8 (Sunday)
        // Feb 5 + 7 = Feb 12 > today → capped to Feb 8 which != Feb 5 → can navigate
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 2, 5))

        assertTrue(viewModel.canNavigateWeekForward())
    }

    @Test
    fun `multiple backward navigations work correctly`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onNavigateWeek(forward = false) // Feb 8 → Feb 1
        viewModel.onNavigateWeek(forward = false) // Feb 1 → Jan 25

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 1, 25), awaitItem())
        }
    }

    @Test
    fun `backward then forward returns to original date when in past`() = runTest {
        val timeProvider = FakeTimeProvider(date = LocalDate.of(2026, 2, 8), hour = 14)
        val viewModel = DayCardViewModel(repository, timeProvider)

        viewModel.onDateSelected(LocalDate.of(2026, 1, 25))
        viewModel.onNavigateWeek(forward = false) // Jan 25 → Jan 18
        viewModel.onNavigateWeek(forward = true)  // Jan 18 → Jan 25

        viewModel.selectedDate.test {
            assertEquals(LocalDate.of(2026, 1, 25), awaitItem())
        }
    }
}
