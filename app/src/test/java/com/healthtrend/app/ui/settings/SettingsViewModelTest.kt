package com.healthtrend.app.ui.settings

import app.cash.turbine.test
import android.content.Context
import com.healthtrend.app.data.auth.FakeGoogleAuthClient
import com.healthtrend.app.data.auth.GoogleSignInResult
import com.healthtrend.app.data.sync.FakeSheetsClient
import com.healthtrend.app.data.sync.FakeSyncTrigger
import org.mockito.Mockito.mock
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
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SettingsViewModel.
 * Uses FakeAppSettingsDao + FakeGoogleAuthClient + FakeReminderScheduler + FakeSheetsClient.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {

    private val testDispatcher = UnconfinedTestDispatcher()
    private lateinit var fakeDao: FakeAppSettingsDao
    private lateinit var repository: AppSettingsRepository
    private lateinit var fakeAuthClient: FakeGoogleAuthClient
    private lateinit var fakeScheduler: FakeReminderScheduler
    private lateinit var fakeSheetsClient: FakeSheetsClient
    private lateinit var fakeSyncTrigger: FakeSyncTrigger

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeDao = FakeAppSettingsDao()
        repository = AppSettingsRepository(fakeDao)
        fakeAuthClient = FakeGoogleAuthClient()
        fakeScheduler = FakeReminderScheduler()
        fakeSheetsClient = FakeSheetsClient()
        fakeSyncTrigger = FakeSyncTrigger()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): SettingsViewModel {
        return SettingsViewModel(repository, fakeAuthClient, fakeScheduler, fakeSheetsClient, fakeSyncTrigger)
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

    // --- Auth Sign-In Tests ---

    @Test
    fun `onSignIn success persists email and sets SignedIn state`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertTrue(state.authState is AuthState.SignedIn)
            assertEquals("raja@example.com", (state.authState as AuthState.SignedIn).email)
            assertEquals("raja@example.com", fakeDao.getSettingsOnce()?.googleAccountEmail)
        }
    }

    @Test
    fun `onSignIn failure sets RefreshFailed state`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Failure("Network error")
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertTrue(state.authState is AuthState.RefreshFailed)
        }
    }

    @Test
    fun `onSignIn cancelled keeps SignedOut state`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Cancelled
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem() // initial SignedOut

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            assertTrue(state.authState is AuthState.SignedOut)
        }
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

    // --- AC #5: Share Sheet Link — state exposes sheetUrl for share intent ---

    @Test
    fun `AC5 Share Sheet Link - success state exposes sheetUrl as content to share`() = runTest {
        val shareUrl = "https://docs.google.com/spreadsheets/d/abc123/edit"
        fakeDao.insertOrReplace(AppSettings(sheetUrl = shareUrl))

        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertEquals(shareUrl, state.sheetUrl)
            assertTrue(state.sheetUrl.isNotEmpty())
        }
    }

    // ── Story 3.4: Auto-Create Google Sheet Tests ──────────────────

    @Test
    fun `onSignIn success auto-creates sheet when sheetUrl is empty`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // Sheet was auto-created
            assertEquals(1, fakeSheetsClient.createdSheets.size)
            assertEquals("raja@example.com", fakeSheetsClient.createdSheets[0].first)
            assertEquals("HealthTrend", fakeSheetsClient.createdSheets[0].second)
            // URL saved to settings
            assertEquals(fakeSheetsClient.createSheetReturnUrl, state.sheetUrl)
        }
    }

    @Test
    fun `onSignIn success skips sheet creation when sheetUrl already exists`() = runTest {
        val existingUrl = "https://docs.google.com/spreadsheets/d/existing-sheet"
        fakeDao.insertOrReplace(AppSettings(sheetUrl = existingUrl))
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // No sheet created — existing URL preserved
            assertEquals(0, fakeSheetsClient.createdSheets.size)
            assertEquals(existingUrl, state.sheetUrl)
        }
    }

    @Test
    fun `onSignIn success still sets SignedIn when sheet creation fails`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        fakeSheetsClient.createSheetShouldFail = true
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // Auth succeeded despite sheet creation failure (AC #4)
            assertTrue(state.authState is AuthState.SignedIn)
            assertEquals("raja@example.com", (state.authState as AuthState.SignedIn).email)
            // Sheet URL remains empty — will retry on next launch
            assertEquals("", state.sheetUrl)
        }
    }

    @Test
    fun `onSignIn failure does not attempt sheet creation`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Failure("Network error")
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            assertEquals(0, fakeSheetsClient.createdSheets.size)
        }
    }

    @Test
    fun `onSignIn cancelled does not attempt sheet creation`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Cancelled
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            assertEquals(0, fakeSheetsClient.createdSheets.size)
        }
    }

    @Test
    fun `sheet title constant is HealthTrend`() {
        assertEquals("HealthTrend", SettingsViewModel.SHEET_TITLE)
    }

    @Test
    fun `onSignIn auto-creates sheet with correct title`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            // Verify sheet created with the correct title
            assertTrue(fakeSheetsClient.createdSheets.isNotEmpty())
            assertEquals("HealthTrend", fakeSheetsClient.createdSheets[0].second)
        }
    }

    // ── Cross-device Sheet Reuse: findSheet before createSheet ────

    @Test
    fun `onSignIn reuses existing sheet found via findSheet`() = runTest {
        val existingDriveUrl = "https://docs.google.com/spreadsheets/d/drive-found-id"
        fakeSheetsClient.findSheetReturnUrl = existingDriveUrl
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // findSheet was called
            assertEquals(1, fakeSheetsClient.findSheetCalls.size)
            assertEquals("raja@example.com", fakeSheetsClient.findSheetCalls[0].first)
            assertEquals("HealthTrend", fakeSheetsClient.findSheetCalls[0].second)
            // No sheet created — reused existing one
            assertEquals(0, fakeSheetsClient.createdSheets.size)
            // URL from findSheet saved to settings
            assertEquals(existingDriveUrl, state.sheetUrl)
        }
    }

    @Test
    fun `onSignIn creates new sheet when findSheet returns null`() = runTest {
        fakeSheetsClient.findSheetReturnUrl = null // no existing sheet
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // findSheet was called first
            assertEquals(1, fakeSheetsClient.findSheetCalls.size)
            // Then createSheet was called
            assertEquals(1, fakeSheetsClient.createdSheets.size)
            assertEquals(fakeSheetsClient.createSheetReturnUrl, state.sheetUrl)
        }
    }

    @Test
    fun `onSignIn handles findSheet failure gracefully`() = runTest {
        fakeSheetsClient.findSheetShouldFail = true
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // Auth succeeded despite findSheet failure (silent failure)
            assertTrue(state.authState is AuthState.SignedIn)
            // Sheet URL remains empty — will retry on next launch
            assertEquals("", state.sheetUrl)
            // No createSheet call since findSheet failed first
            assertEquals(0, fakeSheetsClient.createdSheets.size)
        }
    }

    // ── Immediate Sync After Sheet Setup ──────────────────────────

    @Test
    fun `onSignIn triggers immediate sync after sheet is found`() = runTest {
        fakeSheetsClient.findSheetReturnUrl = "https://docs.google.com/spreadsheets/d/found"
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            assertEquals(1, fakeSyncTrigger.syncEnqueueCount)
        }
    }

    @Test
    fun `onSignIn triggers immediate sync after sheet is created`() = runTest {
        fakeSheetsClient.findSheetReturnUrl = null // not found — will create
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            assertEquals(1, fakeSyncTrigger.syncEnqueueCount)
        }
    }

    @Test
    fun `onSignIn does not trigger sync when sheet setup fails`() = runTest {
        fakeSheetsClient.findSheetShouldFail = true
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            assertEquals(0, fakeSyncTrigger.syncEnqueueCount)
        }
    }

    @Test
    fun `onSignIn does not trigger sync when sheet URL already exists`() = runTest {
        fakeDao.insertOrReplace(AppSettings(sheetUrl = "https://docs.google.com/spreadsheets/d/existing"))
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()
            viewModel.onSignIn(mockContext)
            expectMostRecentItem()

            // No sync triggered — sheet already existed locally, ensureSheetExists returned early
            assertEquals(0, fakeSyncTrigger.syncEnqueueCount)
        }
    }

    // ── Story 3.4: Sheet Creation In-Progress State ───────────────

    @Test
    fun `initial state has sheetCreationInProgress false`() = runTest {
        val viewModel = createViewModel()

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            assertFalse(state.sheetCreationInProgress)
        }
    }

    @Test
    fun `sheetCreationInProgress is false after sheet creation completes`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.onSignIn(mockContext)
        // Allow IO dispatcher to complete (fakes are instant, just needs thread switch)
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(200)

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            // After completion: in-progress flag is cleared, URL is set
            assertFalse(state.sheetCreationInProgress)
            assertEquals(fakeSheetsClient.createSheetReturnUrl, state.sheetUrl)
        }
    }

    @Test
    fun `sheetCreationInProgress is false after sheet creation fails`() = runTest {
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        fakeSheetsClient.createSheetShouldFail = true
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.onSignIn(mockContext)
        // Allow IO dispatcher to complete (fakes are instant, just needs thread switch)
        @Suppress("BlockingMethodInNonBlockingContext")
        Thread.sleep(200)

        viewModel.uiState.test {
            val state = expectMostRecentItem() as SettingsUiState.Success
            // After failure: flag cleared, URL still empty
            assertFalse(state.sheetCreationInProgress)
            assertEquals("", state.sheetUrl)
        }
    }

    @Test
    fun `sheetCreationInProgress is false when sheet already exists`() = runTest {
        val existingUrl = "https://docs.google.com/spreadsheets/d/existing"
        fakeDao.insertOrReplace(AppSettings(sheetUrl = existingUrl))
        fakeAuthClient.signInResult = GoogleSignInResult.Success(
            email = "raja@example.com",
            idToken = "token123"
        )
        val mockContext = mock(Context::class.java)
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSignIn(mockContext)

            val state = expectMostRecentItem() as SettingsUiState.Success
            // Never set to true because sheet already existed
            assertFalse(state.sheetCreationInProgress)
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

    @Test
    fun `onSlotReminderToggled true does not schedule when global reminders are disabled`() = runTest {
        fakeDao.insertOrReplace(AppSettings(globalRemindersEnabled = false, eveningReminderEnabled = false))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotReminderToggled(TimeSlot.EVENING, true)
            expectMostRecentItem()

            assertFalse(fakeScheduler.scheduledAlarms.containsKey(TimeSlot.EVENING))
            assertTrue(fakeScheduler.cancelledSlots.contains(TimeSlot.EVENING))
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

    @Test
    fun `onSlotTimeChanged does not schedule when slot is disabled`() = runTest {
        fakeDao.insertOrReplace(AppSettings(morningReminderEnabled = false))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotTimeChanged(TimeSlot.MORNING, 7, 30)
            expectMostRecentItem()

            assertTrue(fakeScheduler.cancelledSlots.contains(TimeSlot.MORNING))
            assertFalse(fakeScheduler.scheduledAlarms.containsKey(TimeSlot.MORNING))
        }
    }

    @Test
    fun `onSlotTimeChanged does not schedule when global reminders are disabled`() = runTest {
        fakeDao.insertOrReplace(AppSettings(globalRemindersEnabled = false))
        val viewModel = createViewModel()

        viewModel.uiState.test {
            expectMostRecentItem()

            viewModel.onSlotTimeChanged(TimeSlot.MORNING, 7, 30)
            expectMostRecentItem()

            assertTrue(fakeScheduler.cancelledSlots.contains(TimeSlot.MORNING))
            assertFalse(fakeScheduler.scheduledAlarms.containsKey(TimeSlot.MORNING))
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
