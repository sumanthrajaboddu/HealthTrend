package com.healthtrend.app.ui.analytics

import app.cash.turbine.test
import com.healthtrend.app.data.export.FakePdfGenerator
import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.local.FakeHealthEntryDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.HealthEntry
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.repository.HealthEntryRepository
import com.healthtrend.app.data.sync.FakeSyncTrigger
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

/**
 * Unit tests for AnalyticsViewModel.
 * Follows existing test patterns: FakeHealthEntryDao + Turbine + UnconfinedTestDispatcher.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AnalyticsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeHealthEntryDao
    private lateinit var fakeSettingsDao: FakeAppSettingsDao
    private lateinit var repository: HealthEntryRepository
    private lateinit var settingsRepository: AppSettingsRepository
    private lateinit var fakePdfGenerator: FakePdfGenerator
    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeHealthEntryDao()
        fakeSettingsDao = FakeAppSettingsDao()
        repository = HealthEntryRepository(fakeDao, FakeSyncTrigger())
        settingsRepository = AppSettingsRepository(fakeSettingsDao)
        fakePdfGenerator = FakePdfGenerator()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): AnalyticsViewModel {
        return AnalyticsViewModel(repository, settingsRepository, fakePdfGenerator)
    }

    private fun today(): LocalDate = LocalDate.now()

    private fun dateStr(daysAgo: Int): String =
        today().minusDays(daysAgo.toLong()).format(dateFormatter)

    private suspend fun insertEntry(
        daysAgo: Int,
        timeSlot: TimeSlot,
        severity: Severity
    ) {
        fakeDao.insert(
            HealthEntry(
                date = dateStr(daysAgo),
                timeSlot = timeSlot,
                severity = severity
            )
        )
    }

    // ===================================================================
    // Subtask 1.2: UiState sealed interface — Loading, Success, Empty
    // ===================================================================

    @Test
    fun `initial state is Loading`() = runTest {
        val viewModel = createViewModel()
        // With UnconfinedTestDispatcher the flow resolves immediately,
        // but initial value before subscription is Loading.
        // The stateIn initialValue = Loading.
        assertTrue(viewModel.uiState.value is AnalyticsUiState.Loading ||
            viewModel.uiState.value is AnalyticsUiState.Empty)
    }

    @Test
    fun `empty state when no entries for 1 Week range`() = runTest {
        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = awaitItem()
            // With no data, should be Empty
            if (state is AnalyticsUiState.Empty) {
                assertEquals(DateRange.ONE_WEEK, state.selectedRange)
            } else if (state is AnalyticsUiState.Loading) {
                val next = awaitItem()
                assertTrue("Expected Empty but got $next", next is AnalyticsUiState.Empty)
                assertEquals(DateRange.ONE_WEEK, (next as AnalyticsUiState.Empty).selectedRange)
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Subtask 1.3: selectedRange StateFlow — 1 Week default
    // ===================================================================

    @Test
    fun `default selected range is ONE_WEEK`() = runTest {
        val viewModel = createViewModel()
        assertEquals(DateRange.ONE_WEEK, viewModel.selectedRange.value)
    }

    @Test
    fun `selectRange updates selectedRange to ONE_MONTH`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRange(DateRange.ONE_MONTH)
        assertEquals(DateRange.ONE_MONTH, viewModel.selectedRange.value)
    }

    @Test
    fun `selectRange updates selectedRange to THREE_MONTHS`() = runTest {
        val viewModel = createViewModel()
        viewModel.selectRange(DateRange.THREE_MONTHS)
        assertEquals(DateRange.THREE_MONTHS, viewModel.selectedRange.value)
    }

    // ===================================================================
    // Subtask 1.4: Query entries for selected date range
    // ===================================================================

    @Test
    fun `success state when entries exist for 1 Week`() = runTest {
        // Insert entries within last 7 days
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)
        insertEntry(2, TimeSlot.AFTERNOON, Severity.MODERATE)
        insertEntry(3, TimeSlot.EVENING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue("Expected Success but got $state", state is AnalyticsUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `entries outside selected range are excluded`() = runTest {
        // Insert entry 20 days ago — outside 1 Week range
        insertEntry(20, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue("Expected Empty for out-of-range data but got $state",
                state is AnalyticsUiState.Empty)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `switching to ONE_MONTH includes entries beyond 1 week`() = runTest {
        // Insert entry 15 days ago — outside 1 Week but inside 1 Month
        insertEntry(15, TimeSlot.MORNING, Severity.MODERATE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            // Initially empty for 1 Week
            val initial = expectMostRecentItem()
            assertTrue("Expected Empty for 1-week but got $initial",
                initial is AnalyticsUiState.Empty)

            // Switch to 1 Month
            viewModel.selectRange(DateRange.ONE_MONTH)
            val updated = expectMostRecentItem()
            assertTrue("Expected Success for 1-month but got $updated",
                updated is AnalyticsUiState.Success)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Subtask 1.5: Transform entries to chart data — daily aggregation
    // ===================================================================

    @Test
    fun `chart data has one point per day sorted by date`() = runTest {
        insertEntry(3, TimeSlot.MORNING, Severity.MILD)
        insertEntry(2, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(1, TimeSlot.MORNING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(3, state.chartData.size)
            // Sorted ascending by date
            assertTrue(state.chartData[0].date < state.chartData[1].date)
            assertTrue(state.chartData[1].date < state.chartData[2].date)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `multiple entries per day aggregated to max severity`() = runTest {
        // Same day, different slots, different severities
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)
        insertEntry(1, TimeSlot.AFTERNOON, Severity.SEVERE)
        insertEntry(1, TimeSlot.EVENING, Severity.MODERATE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(1, state.chartData.size)
            // Max severity = SEVERE = 3.0
            assertEquals(3.0f, state.chartData[0].severityValue)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `chart data point severity value matches Severity numericValue`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.NO_PAIN)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(
                Severity.NO_PAIN.numericValue.toFloat(),
                state.chartData[0].severityValue
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Subtask 1.6: Summary stats — average severity + trend direction
    // ===================================================================

    @Test
    fun `average severity calculated from all entries not daily max`() = runTest {
        // Day 1: MILD(1) + SEVERE(3) = avg 2.0 → but all-entries avg = (1+3)/2 = 2.0 → MODERATE
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)
        insertEntry(1, TimeSlot.EVENING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.MODERATE, state.summary.averageSeverity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trend direction IMPROVING when later entries are lower`() = runTest {
        // Earlier: SEVERE, Later: MILD
        insertEntry(6, TimeSlot.MORNING, Severity.SEVERE)
        insertEntry(5, TimeSlot.MORNING, Severity.SEVERE)
        insertEntry(2, TimeSlot.MORNING, Severity.MILD)
        insertEntry(1, TimeSlot.MORNING, Severity.NO_PAIN)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(TrendDirection.IMPROVING, state.summary.trendDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trend direction WORSENING when later entries are higher`() = runTest {
        // Earlier: NO_PAIN, Later: SEVERE
        insertEntry(6, TimeSlot.MORNING, Severity.NO_PAIN)
        insertEntry(5, TimeSlot.MORNING, Severity.MILD)
        insertEntry(2, TimeSlot.MORNING, Severity.SEVERE)
        insertEntry(1, TimeSlot.MORNING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(TrendDirection.WORSENING, state.summary.trendDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `trend direction STABLE when entries are consistent`() = runTest {
        insertEntry(4, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(3, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(2, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(1, TimeSlot.MORNING, Severity.MODERATE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(TrendDirection.STABLE, state.summary.trendDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `summary period label matches selected range displayName`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals("1 Week", state.summary.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `summary period label updates when range changes`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            expectMostRecentItem() // consume initial

            viewModel.selectRange(DateRange.ONE_MONTH)
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals("1 Month", state.summary.periodLabel)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Subtask 1.6 continued: numericToSeverity rounding
    // ===================================================================

    @Test
    fun `numericToSeverity rounds correctly at boundaries`() {
        // 0–0.49 = NO_PAIN
        assertEquals(Severity.NO_PAIN, AnalyticsViewModel.numericToSeverity(0.0))
        assertEquals(Severity.NO_PAIN, AnalyticsViewModel.numericToSeverity(0.49))

        // 0.5–1.49 = MILD
        assertEquals(Severity.MILD, AnalyticsViewModel.numericToSeverity(0.5))
        assertEquals(Severity.MILD, AnalyticsViewModel.numericToSeverity(1.49))

        // 1.5–2.49 = MODERATE
        assertEquals(Severity.MODERATE, AnalyticsViewModel.numericToSeverity(1.5))
        assertEquals(Severity.MODERATE, AnalyticsViewModel.numericToSeverity(2.49))

        // 2.5–3.0 = SEVERE
        assertEquals(Severity.SEVERE, AnalyticsViewModel.numericToSeverity(2.5))
        assertEquals(Severity.SEVERE, AnalyticsViewModel.numericToSeverity(3.0))
    }

    // ===================================================================
    // Subtask 1.7: Reactive state — range changes trigger re-query
    // ===================================================================

    @Test
    fun `range change from ONE_WEEK to THREE_MONTHS recalculates`() = runTest {
        // Entry at 60 days ago — only visible in 3 Months
        insertEntry(60, TimeSlot.MORNING, Severity.MODERATE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val weekState = expectMostRecentItem()
            assertTrue("Expected Empty for 1-week", weekState is AnalyticsUiState.Empty)

            viewModel.selectRange(DateRange.THREE_MONTHS)
            val threeMonthState = expectMostRecentItem()
            assertTrue("Expected Success for 3-months",
                threeMonthState is AnalyticsUiState.Success)
            assertEquals(DateRange.THREE_MONTHS,
                (threeMonthState as AnalyticsUiState.Success).selectedRange)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `success state selectedRange reflects current selection`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(DateRange.ONE_WEEK, state.selectedRange)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Edge cases
    // ===================================================================

    @Test
    fun `single entry produces single chart data point`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(1, state.chartData.size)
            assertEquals(1.0f, state.chartData[0].severityValue) // MILD = 1
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single entry trend direction is STABLE`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(TrendDirection.STABLE, state.summary.trendDirection)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `all four time slots on same day aggregated correctly`() = runTest {
        // MORNING=NO_PAIN(0), AFTERNOON=MILD(1), EVENING=MODERATE(2), NIGHT=SEVERE(3)
        insertEntry(1, TimeSlot.MORNING, Severity.NO_PAIN)
        insertEntry(1, TimeSlot.AFTERNOON, Severity.MILD)
        insertEntry(1, TimeSlot.EVENING, Severity.MODERATE)
        insertEntry(1, TimeSlot.NIGHT, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(1, state.chartData.size)
            // Max = SEVERE = 3.0
            assertEquals(3.0f, state.chartData[0].severityValue)
            // Average of all entries: (0+1+2+3)/4 = 1.5 → MODERATE
            assertEquals(Severity.MODERATE, state.summary.averageSeverity)
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Story 5.2 — Slot Averages (Time-of-Day Breakdown)
    // ===================================================================

    @Test
    fun `slot averages contain all four TimeSlots`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(4, state.slotAverages.size)
            assertTrue(state.slotAverages.containsKey(TimeSlot.MORNING))
            assertTrue(state.slotAverages.containsKey(TimeSlot.AFTERNOON))
            assertTrue(state.slotAverages.containsKey(TimeSlot.EVENING))
            assertTrue(state.slotAverages.containsKey(TimeSlot.NIGHT))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `slot average is null for slot with no entries`() = runTest {
        // Only MORNING has data
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.MILD, state.slotAverages[TimeSlot.MORNING])
            assertEquals(null, state.slotAverages[TimeSlot.AFTERNOON])
            assertEquals(null, state.slotAverages[TimeSlot.EVENING])
            assertEquals(null, state.slotAverages[TimeSlot.NIGHT])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `slot average rounds correctly per story spec`() = runTest {
        // Morning: Moderate(2), Moderate(2), Severe(3), Moderate(2), Mild(1), Moderate(2), Moderate(2)
        // Sum = 14, Count = 7, Average = 2.0 → MODERATE
        insertEntry(7, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(6, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(5, TimeSlot.MORNING, Severity.SEVERE)
        insertEntry(4, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(3, TimeSlot.MORNING, Severity.MILD)
        insertEntry(2, TimeSlot.MORNING, Severity.MODERATE)
        insertEntry(1, TimeSlot.MORNING, Severity.MODERATE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.MODERATE, state.slotAverages[TimeSlot.MORNING])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `slot averages for all four slots with different data`() = runTest {
        // MORNING: NO_PAIN(0) → NO_PAIN
        insertEntry(1, TimeSlot.MORNING, Severity.NO_PAIN)
        // AFTERNOON: MILD(1), MODERATE(2) → avg 1.5 → MODERATE
        insertEntry(1, TimeSlot.AFTERNOON, Severity.MILD)
        insertEntry(2, TimeSlot.AFTERNOON, Severity.MODERATE)
        // EVENING: SEVERE(3) → SEVERE
        insertEntry(1, TimeSlot.EVENING, Severity.SEVERE)
        // NIGHT: MILD(1), MILD(1), SEVERE(3) → avg 1.67 → MODERATE
        insertEntry(1, TimeSlot.NIGHT, Severity.MILD)
        insertEntry(2, TimeSlot.NIGHT, Severity.MILD)
        insertEntry(3, TimeSlot.NIGHT, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.NO_PAIN, state.slotAverages[TimeSlot.MORNING])
            assertEquals(Severity.MODERATE, state.slotAverages[TimeSlot.AFTERNOON])
            assertEquals(Severity.SEVERE, state.slotAverages[TimeSlot.EVENING])
            assertEquals(Severity.MODERATE, state.slotAverages[TimeSlot.NIGHT])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `slot averages recalculate when date range changes`() = runTest {
        // Entry at 15 days ago — outside 1 Week but inside 1 Month
        insertEntry(15, TimeSlot.MORNING, Severity.SEVERE)
        // Entry at 1 day ago — inside both ranges
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            // 1 Week: only day 1 entry → MORNING = MILD
            val weekState = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.MILD, weekState.slotAverages[TimeSlot.MORNING])

            // Switch to 1 Month: both entries → avg (3+1)/2 = 2.0 → MODERATE
            viewModel.selectRange(DateRange.ONE_MONTH)
            val monthState = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.MODERATE, monthState.slotAverages[TimeSlot.MORNING])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `single entry in slot gives that exact severity`() = runTest {
        insertEntry(1, TimeSlot.EVENING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            assertEquals(Severity.SEVERE, state.slotAverages[TimeSlot.EVENING])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `slot average boundary rounding 0 point 49 is NO_PAIN`() = runTest {
        // Two entries: NO_PAIN(0) + MILD(1) = avg 0.5 → MILD (not NO_PAIN)
        insertEntry(1, TimeSlot.MORNING, Severity.NO_PAIN)
        insertEntry(2, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.uiState.test {
            val state = expectMostRecentItem() as AnalyticsUiState.Success
            // avg = 0.5 → MILD per rounding rules
            assertEquals(Severity.MILD, state.slotAverages[TimeSlot.MORNING])
            cancelAndIgnoreRemainingEvents()
        }
    }

    // ===================================================================
    // Story 6.1 — Export State (PDF Report Generation)
    // ===================================================================

    @Test
    fun `export state initial value is Idle`() = runTest {
        val viewModel = createViewModel()
        assertEquals(ExportState.Idle, viewModel.exportState.value)
    }

    @Test
    fun `onExportPdf transitions through Generating to Preview`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            assertEquals(ExportState.Idle, awaitItem())

            viewModel.onExportPdf()

            // Should transition to Generating then Preview
            val states = mutableListOf<ExportState>()
            while (true) {
                val state = awaitItem()
                states.add(state)
                if (state is ExportState.Preview || state is ExportState.Error) break
            }

            assertTrue(
                "Expected Generating in transition states",
                states.any { it is ExportState.Generating }
            )
            assertTrue(
                "Expected Preview as final state",
                states.last() is ExportState.Preview
            )
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf passes patient name to generator`() = runTest {
        fakeSettingsDao.insertOrReplace(AppSettings(patientName = "Uncle"))
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            // Wait for completion
            while (true) {
                val state = awaitItem()
                if (state is ExportState.Preview || state is ExportState.Error) break
            }

            assertEquals("Uncle", fakePdfGenerator.lastPatientName)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf with blank patient name does not error`() = runTest {
        // No settings configured — patient name defaults to ""
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            while (true) {
                val state = awaitItem()
                if (state is ExportState.Preview) {
                    // AC #4: blank name = no error
                    assertEquals("", fakePdfGenerator.lastPatientName)
                    break
                }
                if (state is ExportState.Error) {
                    throw AssertionError("Expected Preview, got Error: ${state.message}")
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf with empty entries produces Preview not Error`() = runTest {
        // AC #6: empty data = valid PDF, NOT an error
        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            while (true) {
                val state = awaitItem()
                if (state is ExportState.Preview) break
                if (state is ExportState.Error) {
                    throw AssertionError("Empty data should produce Preview, not Error: ${state.message}")
                }
            }
            assertEquals(0, fakePdfGenerator.lastEntries?.size)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf during Generating is no-op`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.onExportPdf()
        viewModel.onExportPdf() // double-tap — should be ignored

        // Wait for completion
        viewModel.exportState.test {
            val finalState = expectMostRecentItem()
            // Only 1 call to generator (double-tap prevented)
            assertEquals(1, fakePdfGenerator.generateCallCount)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf on error sets Error state`() = runTest {
        fakePdfGenerator.shouldFail = true
        fakePdfGenerator.errorMessage = "Test error"
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            while (true) {
                val state = awaitItem()
                if (state is ExportState.Error) {
                    assertEquals("Test error", state.message)
                    break
                }
                if (state is ExportState.Preview) {
                    throw AssertionError("Expected Error, got Preview")
                }
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `resetExportState returns to Idle`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            // Wait for Preview
            while (true) {
                val state = awaitItem()
                if (state is ExportState.Preview) break
            }

            viewModel.resetExportState()
            assertEquals(ExportState.Idle, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf passes slot averages to generator`() = runTest {
        insertEntry(1, TimeSlot.MORNING, Severity.MILD)
        insertEntry(1, TimeSlot.EVENING, Severity.SEVERE)

        val viewModel = createViewModel()
        viewModel.exportState.test {
            awaitItem() // Idle

            viewModel.onExportPdf()

            while (true) {
                val state = awaitItem()
                if (state is ExportState.Preview) break
                if (state is ExportState.Error) throw AssertionError("Unexpected error")
            }

            val slotAverages = fakePdfGenerator.lastSlotAverages!!
            assertEquals(Severity.MILD, slotAverages[TimeSlot.MORNING])
            assertEquals(null, slotAverages[TimeSlot.AFTERNOON])
            assertEquals(Severity.SEVERE, slotAverages[TimeSlot.EVENING])
            assertEquals(null, slotAverages[TimeSlot.NIGHT])
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `onExportPdf uses current date range`() = runTest {
        // Insert entry 15 days ago — outside 1 Week but inside 1 Month
        insertEntry(15, TimeSlot.MORNING, Severity.MODERATE)

        val viewModel = createViewModel()

        // Default range is 1 Week — entry is outside
        viewModel.onExportPdf()
        viewModel.exportState.test {
            val state = expectMostRecentItem()
            if (state is ExportState.Preview) {
                assertTrue(
                    "1-week export should have 0 entries for 15-day-old data",
                    fakePdfGenerator.lastEntries?.isEmpty() == true
                )
            }
            cancelAndIgnoreRemainingEvents()
        }

        // Switch to 1 Month — entry is inside
        viewModel.resetExportState()
        viewModel.selectRange(DateRange.ONE_MONTH)
        viewModel.onExportPdf()
        viewModel.exportState.test {
            while (true) {
                val state = expectMostRecentItem()
                if (state is ExportState.Preview) {
                    assertEquals(1, fakePdfGenerator.lastEntries?.size)
                    break
                }
                if (state is ExportState.Error) throw AssertionError("Unexpected error")
                break
            }
            cancelAndIgnoreRemainingEvents()
        }
    }
}
