package com.healthtrend.app.ui.daycard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.healthtrend.app.data.model.Severity
import com.healthtrend.app.data.model.TimeSlot
import com.healthtrend.app.data.repository.HealthEntryRepository
import com.healthtrend.app.util.TimeProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * ViewModel for the Day Card screen.
 * One ViewModel per screen. Injects HealthEntryRepository (never DAO directly).
 * State exposed as StateFlow — never LiveData.
 *
 * Supports multi-date navigation: [selectedDate] drives which day's entries are loaded.
 * Entries query reacts to [selectedDate] changes via [flatMapLatest].
 * Week strip data indicators query reacts to week changes via separate [flatMapLatest].
 */
@HiltViewModel
class DayCardViewModel @Inject constructor(
    private val repository: HealthEntryRepository,
    private val timeProvider: TimeProvider
) : ViewModel() {

    /** Today's date — fixed at ViewModel creation time. */
    val today: LocalDate = timeProvider.currentDate()

    /** Currently selected date for the Day Card pager. */
    private val _selectedDate = MutableStateFlow(today)
    val selectedDate: StateFlow<LocalDate> = _selectedDate.asStateFlow()

    private val _uiState = MutableStateFlow<DayCardUiState>(DayCardUiState.Loading)
    val uiState: StateFlow<DayCardUiState> = _uiState.asStateFlow()

    /** Which dates in the currently visible week have any logged entries. */
    private val _weekDatesWithData = MutableStateFlow<Set<LocalDate>>(emptySet())
    val weekDatesWithData: StateFlow<Set<LocalDate>> = _weekDatesWithData.asStateFlow()

    /** Which time slot's picker is currently open (null = none). */
    private val _pickerOpenForSlot = MutableStateFlow<TimeSlot?>(null)

    /**
     * Tracks whether we should check for all-complete bloom on the next state update.
     * Set to true after a severity save; cleared after the check.
     */
    private var pendingAllCompleteCheck = false

    /** Tracks whether all slots were logged BEFORE the current save. */
    private var previousAllLogged = false

    init {
        loadEntries()
        observeWeekDataIndicators()
    }

    /**
     * Observes [_selectedDate] and loads entries for the selected date via [flatMapLatest].
     * When selectedDate changes, the previous query is cancelled and a new one starts.
     * Combines with [_pickerOpenForSlot] to include picker state in the UI state.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun loadEntries() {
        viewModelScope.launch {
            _selectedDate
                .flatMapLatest { date ->
                    val dateString = date.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    repository.getEntriesByDate(dateString)
                        .combine(_pickerOpenForSlot) { entries, pickerSlot ->
                            Triple(date, entries, pickerSlot)
                        }
                }
                .catch { e ->
                    _uiState.value = DayCardUiState.Error(
                        e.message ?: "Failed to load entries"
                    )
                }
                .collect { (date, entries, pickerSlot) ->
                    val entryMap = TimeSlot.entries.associateWith { slot ->
                        entries.find { it.timeSlot == slot }
                    }

                    val isToday = date == today

                    val allLogged = entryMap.values.all { it != null }
                    val shouldBloom = pendingAllCompleteCheck && allLogged && !previousAllLogged
                    if (pendingAllCompleteCheck) {
                        pendingAllCompleteCheck = false
                    }
                    previousAllLogged = allLogged

                    _uiState.value = DayCardUiState.Success(
                        date = date,
                        entries = entryMap,
                        currentTimeSlot = if (isToday) determineCurrentTimeSlot() else null,
                        pickerOpenForSlot = pickerSlot,
                        allCompleteBloom = shouldBloom,
                        isToday = isToday
                    )
                }
        }
    }

    /**
     * Observes [_selectedDate], derives the week range (Monday–Sunday), and queries
     * which dates in that range have entries. Only re-queries when the week changes
     * (not on every date change within the same week).
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeWeekDataIndicators() {
        viewModelScope.launch {
            _selectedDate
                .map { date ->
                    val start = DatePagerUtils.weekStartDate(date)
                    val end = DatePagerUtils.weekEndDate(date)
                    Pair(start, end)
                }
                .distinctUntilChanged() // Only re-query when week boundaries change
                .flatMapLatest { (start, end) ->
                    val startStr = start.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    val endStr = end.format(DateTimeFormatter.ISO_LOCAL_DATE)
                    repository.getDatesWithEntries(startStr, endStr)
                }
                .catch { /* Silent — week indicators are non-critical */ }
                .collect { dateStrings ->
                    _weekDatesWithData.value = dateStrings
                        .map { LocalDate.parse(it) }
                        .toSet()
                }
        }
    }

    /**
     * Updates the selected date when the pager settles on a new page.
     * Closes any open picker and resets bloom tracking for the new date.
     * No-op if the date hasn't changed.
     */
    fun onDateSelected(date: LocalDate) {
        if (date != _selectedDate.value) {
            _pickerOpenForSlot.value = null
            previousAllLogged = false
            pendingAllCompleteCheck = false
            _selectedDate.value = date
        }
    }

    /**
     * Navigates the selected date by one week forward or backward.
     * Forward navigation is capped at [today] — no future dates.
     * Used by week strip left/right arrow buttons.
     *
     * @param forward true = next week, false = previous week.
     */
    fun onNavigateWeek(forward: Boolean) {
        val offset = if (forward) 7L else -7L
        val newDate = _selectedDate.value.plusDays(offset)
        val capped = if (newDate.isAfter(today)) today else newDate
        onDateSelected(capped)
    }

    /**
     * Whether forward week navigation would change the selected date.
     * False when selectedDate + 7 days would exceed today (already at latest navigable week).
     */
    fun canNavigateWeekForward(): Boolean {
        val projected = _selectedDate.value.plusDays(7)
        val capped = if (projected.isAfter(today)) today else projected
        return capped != _selectedDate.value
    }

    /**
     * Opens the severity picker for the given time slot.
     * If the same slot's picker is already open, closes it (toggle).
     */
    fun onTileClick(timeSlot: TimeSlot) {
        val current = _pickerOpenForSlot.value
        _pickerOpenForSlot.value = if (current == timeSlot) null else timeSlot
    }

    /**
     * Closes the severity picker without saving.
     */
    fun onDismissPicker() {
        _pickerOpenForSlot.value = null
    }

    /**
     * Saves a severity selection for the given time slot on the currently selected date.
     * Closes picker immediately (0ms). Persists via repository.
     * Sets synced = false and updatedAt = currentTimeMillis.
     * Same logic for today and past dates — no conditional behavior.
     */
    fun onSeveritySelected(timeSlot: TimeSlot, severity: Severity) {
        _pickerOpenForSlot.value = null // Close picker immediately (0ms)
        pendingAllCompleteCheck = true

        viewModelScope.launch {
            val dateString = _selectedDate.value.format(DateTimeFormatter.ISO_LOCAL_DATE)
            repository.upsertEntry(dateString, timeSlot, severity)
            // Room Flow will automatically re-emit, updating UiState via collect
        }
    }

    /**
     * Maps the current hour to a TimeSlot for the subtle highlight.
     * Ranges: Morning 6-11, Afternoon 12-16, Evening 17-20, Night 21-5.
     */
    private fun determineCurrentTimeSlot(): TimeSlot {
        val hour = timeProvider.currentHour()
        return when (hour) {
            in 6..11 -> TimeSlot.MORNING
            in 12..16 -> TimeSlot.AFTERNOON
            in 17..20 -> TimeSlot.EVENING
            else -> TimeSlot.NIGHT // 21-23 and 0-5
        }
    }
}
