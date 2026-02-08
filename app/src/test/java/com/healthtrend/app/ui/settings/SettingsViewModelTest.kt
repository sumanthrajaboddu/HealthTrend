package com.healthtrend.app.ui.settings

import app.cash.turbine.test
import com.healthtrend.app.data.auth.FakeGoogleAuthClient
import com.healthtrend.app.data.auth.GoogleSignInResult
import com.healthtrend.app.data.local.FakeAppSettingsDao
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.notification.FakeReminderScheduler
import com.healthtrend.app.data.repository.AppSettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel.
 * Uses FakeAppSettingsDao + FakeGoogleAuthClient + FakeReminderScheduler for deterministic testing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeAppSettingsDao
    private lateinit var repository: AppSettingsRepository
    private lateinit var fakeAuthClient: FakeGoogleAuthClient
    private lateinit var fakeScheduler: FakeReminderScheduler

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAppSettingsDao()
        repository = AppSettingsRepository(fakeDao)
        fakeAuthClient = FakeGoogleAuthClient()
        fakeScheduler = FakeReminderScheduler()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(repository, fakeAuthClient, fakeScheduler)
    }

    // --- Initial State Tests ---

    @Test
    fun `initial state is Success with empty defaults after settings ensured`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem()
            assertTrue("Expected Success, got $state", state is SettingsUiState.Success)
            val success = state as SettingsUiState.Success
            assertEquals("", success.patientName)
            assertEquals("", success.sheetUrl)
            assertTrue(success.isSheetUrlValid)
            assertTrue(success.authState is AuthState.SignedOut)
        }
    }

    @Test
    fun `initial state loads existing settings from repository`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(
                patientName = "Uncle",
                sheetUrl = "https://docs.google.com/spreadsheets/d/abc123"
            )
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("Uncle", state.patientName)
            assertEquals("https://docs.google.com/spreadsheets/d/abc123", state.sheetUrl)
            assertTrue(state.isSheetUrlValid)
        }
    }

    @Test
    fun `initial state shows SignedIn when email stored in settings`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(googleAccountEmail = "raja@example.com")
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            val signedIn = state.authState as AuthState.SignedIn
            assertEquals("raja@example.com", signedIn.email)
        }
    }

    // --- Patient Name Auto-Save Tests ---

    @Test
    fun `onPatientNameChanged triggers debounced save`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onPatientNameChanged("Uncle")

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("Uncle", state.patientName)
        }
    }

    @Test
    fun `rapid patient name changes only persist final value`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onPatientNameChanged("U")
            viewModel.onPatientNameChanged("Un")
            viewModel.onPatientNameChanged("Unc")
            viewModel.onPatientNameChanged("Uncle")

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("Uncle", state.patientName)
        }
    }

    // --- Sheet URL Auto-Save Tests ---

    @Test
    fun `onSheetUrlChanged persists immediately`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            val url = "https://docs.google.com/spreadsheets/d/abc123"
            viewModel.onSheetUrlChanged(url)

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals(url, state.sheetUrl)
            assertTrue(state.isSheetUrlValid)
        }
    }

    @Test
    fun `invalid sheet url shows validation error`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onSheetUrlChanged("not-a-valid-url")

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("not-a-valid-url", state.sheetUrl)
            assertFalse(state.isSheetUrlValid)
        }
    }

    @Test
    fun `empty sheet url is considered valid`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("", state.sheetUrl)
            assertTrue(state.isSheetUrlValid)
        }
    }

    // --- URL Validation Tests ---

    @Test
    fun `isValidSheetUrl accepts valid Google Sheets URL`() {
        assertTrue(
            SettingsViewModel.isValidSheetUrl(
                "https://docs.google.com/spreadsheets/d/1BxiMVs0XRA5nFMdKvBdBZjgmUUqptlbs74OgVE2upms/edit"
            )
        )
    }

    @Test
    fun `isValidSheetUrl accepts minimal valid URL`() {
        assertTrue(
            SettingsViewModel.isValidSheetUrl(
                "https://docs.google.com/spreadsheets/d/abc123"
            )
        )
    }

    @Test
    fun `isValidSheetUrl rejects non-Google URL`() {
        assertFalse(
            SettingsViewModel.isValidSheetUrl("https://example.com/sheets")
        )
    }

    @Test
    fun `isValidSheetUrl rejects random string`() {
        assertFalse(
            SettingsViewModel.isValidSheetUrl("not-a-url")
        )
    }

    @Test
    fun `isValidSheetUrl rejects http without s`() {
        assertFalse(
            SettingsViewModel.isValidSheetUrl(
                "http://docs.google.com/spreadsheets/d/abc123"
            )
        )
    }

    @Test
    fun `isValidSheetUrl rejects google docs non-spreadsheet URL`() {
        assertFalse(
            SettingsViewModel.isValidSheetUrl(
                "https://docs.google.com/document/d/abc123"
            )
        )
    }

    // --- Auth Sign-Out Tests ---

    @Test
    fun `onSignOut clears credential and stored email`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(googleAccountEmail = "raja@example.com")
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial — SignedIn

            viewModel.onSignOut()

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertTrue(state.authState is AuthState.SignedOut)
            assertTrue(fakeAuthClient.signOutCalled)
        }
    }

    // --- Multiple Field Updates ---

    @Test
    fun `updating patient name does not affect sheet url`() = runTest {
        fakeDao.insertOrReplace(
            AppSettings(
                patientName = "Uncle",
                sheetUrl = "https://docs.google.com/spreadsheets/d/abc123"
            )
        )

        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onPatientNameChanged("Aunty")

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("Aunty", state.patientName)
            assertEquals("https://docs.google.com/spreadsheets/d/abc123", state.sheetUrl)
        }
    }

    @Test
    fun `updating sheet url does not affect patient name`() = runTest {
        fakeDao.insertOrReplace(AppSettings(patientName = "Uncle"))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onSheetUrlChanged("https://docs.google.com/spreadsheets/d/xyz789")

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals("Uncle", state.patientName)
            assertEquals("https://docs.google.com/spreadsheets/d/xyz789", state.sheetUrl)
        }
    }

    // ── Reminder Configuration Tests (Story 4.2) ──────────────────

    @Test
    fun `initial state includes reminder defaults - global enabled and 4 slot states`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertTrue(state.globalRemindersEnabled)
            assertEquals(4, state.slotReminders.size)
            // All slots enabled by default
            state.slotReminders.forEach { assertTrue(it.enabled) }
        }
    }

    @Test
    fun `slot reminder states map TimeSlot displayNames correctly`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            val slotNames = state.slotReminders.map { it.timeSlot.displayName }
            assertEquals(listOf("Morning", "Afternoon", "Evening", "Night"), slotNames)
        }
    }

    @Test
    fun `slot reminder states show correct default times`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals(8, state.slotReminders[0].hour)    // Morning
            assertEquals(0, state.slotReminders[0].minute)
            assertEquals(13, state.slotReminders[1].hour)   // Afternoon
            assertEquals(18, state.slotReminders[2].hour)   // Evening
            assertEquals(22, state.slotReminders[3].hour)   // Night
        }
    }

    // --- AC #2: Global toggle cancels all alarms ---

    @Test
    fun `onGlobalRemindersToggled false cancels all alarms`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onGlobalRemindersToggled(false)

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertFalse(state.globalRemindersEnabled)
            assertEquals(1, fakeScheduler.cancelAllCount)
        }
    }

    @Test
    fun `onGlobalRemindersToggled false persists to Room`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onGlobalRemindersToggled(false)
            expectMostRecentItem()
        }

        val saved = fakeDao.getSettingsOnce()!!
        assertFalse(saved.globalRemindersEnabled)
    }

    @Test
    fun `onGlobalRemindersToggled true re-schedules all active alarms`() = runTest {
        // Start with global disabled
        fakeDao.insertOrReplace(AppSettings(globalRemindersEnabled = false))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onGlobalRemindersToggled(true)
            expectMostRecentItem()

            // All 4 default-enabled slots should be scheduled
            assertEquals(4, fakeScheduler.scheduledAlarms.size)
        }
    }

    // --- AC #3: Per-slot toggle ---

    @Test
    fun `onSlotReminderToggled false cancels only that slot alarm`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotReminderToggled(TimeSlot.EVENING, false)

            val state = expectMostRecentItem() as SettingsUiState.Success
            val eveningSlot = state.slotReminders.first { it.timeSlot == TimeSlot.EVENING }
            assertFalse(eveningSlot.enabled)
            assertTrue(fakeScheduler.cancelledSlots.contains(TimeSlot.EVENING))
        }
    }

    @Test
    fun `onSlotReminderToggled false persists to Room`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSlotReminderToggled(TimeSlot.EVENING, false)
            expectMostRecentItem()
        }

        val saved = fakeDao.getSettingsOnce()!!
        assertFalse(saved.eveningReminderEnabled)
        // Other slots unchanged
        assertTrue(saved.morningReminderEnabled)
        assertTrue(saved.afternoonReminderEnabled)
        assertTrue(saved.nightReminderEnabled)
    }

    @Test
    fun `onSlotReminderToggled true schedules that slot alarm`() = runTest {
        fakeDao.insertOrReplace(AppSettings(eveningReminderEnabled = false))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotReminderToggled(TimeSlot.EVENING, true)
            expectMostRecentItem()

            assertTrue(fakeScheduler.scheduledAlarms.containsKey(TimeSlot.EVENING))
            assertEquals(18 to 0, fakeScheduler.scheduledAlarms[TimeSlot.EVENING])
        }
    }

    // --- AC #4: Time change ---

    @Test
    fun `onSlotTimeChanged updates time and reschedules alarm`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotTimeChanged(TimeSlot.MORNING, 7, 30)

            val state = expectMostRecentItem() as SettingsUiState.Success
            val morningSlot = state.slotReminders.first { it.timeSlot == TimeSlot.MORNING }
            assertEquals(7, morningSlot.hour)
            assertEquals(30, morningSlot.minute)
        }
    }

    @Test
    fun `onSlotTimeChanged persists new time to Room`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSlotTimeChanged(TimeSlot.MORNING, 7, 30)
            expectMostRecentItem()
        }

        val saved = fakeDao.getSettingsOnce()!!
        assertEquals("07:30", saved.morningReminderTime)
    }

    @Test
    fun `onSlotTimeChanged cancels old alarm and schedules new`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotTimeChanged(TimeSlot.MORNING, 7, 30)
            expectMostRecentItem()

            // Old alarm cancelled
            assertTrue(fakeScheduler.cancelledSlots.contains(TimeSlot.MORNING))
            // New alarm scheduled at 7:30
            assertEquals(7 to 30, fakeScheduler.scheduledAlarms[TimeSlot.MORNING])
        }
    }

    // --- buildSlotReminderStates ---

    @Test
    fun `buildSlotReminderStates maps all 4 slots from settings`() {
        val settings = AppSettings()
        val states = SettingsViewModel.buildSlotReminderStates(settings)
        assertEquals(4, states.size)
        assertEquals(TimeSlot.MORNING, states[0].timeSlot)
        assertEquals(TimeSlot.AFTERNOON, states[1].timeSlot)
        assertEquals(TimeSlot.EVENING, states[2].timeSlot)
        assertEquals(TimeSlot.NIGHT, states[3].timeSlot)
    }

    @Test
    fun `buildSlotReminderStates reflects disabled slot`() {
        val settings = AppSettings(afternoonReminderEnabled = false)
        val states = SettingsViewModel.buildSlotReminderStates(settings)
        assertTrue(states[0].enabled)  // Morning
        assertFalse(states[1].enabled) // Afternoon disabled
        assertTrue(states[2].enabled)  // Evening
        assertTrue(states[3].enabled)  // Night
    }

    @Test
    fun `buildSlotReminderStates reflects custom time`() {
        val settings = AppSettings(morningReminderTime = "07:30")
        val states = SettingsViewModel.buildSlotReminderStates(settings)
        assertEquals(7, states[0].hour)
        assertEquals(30, states[0].minute)
    }
}
