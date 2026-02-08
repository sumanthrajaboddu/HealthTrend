# Implementation Patterns & Consistency Rules

## Pattern Categories Defined

**18 conflict points identified** where AI agents could make different choices. Patterns below ensure consistent, compatible code across all implementation.

## Naming Patterns

**Kotlin Code Naming:**

| Element | Convention | Example |
|---------|-----------|---------|
| Classes | PascalCase | `HealthEntry`, `DayCardViewModel`, `SyncWorker` |
| Functions | camelCase | `getEntriesForDate()`, `saveSeverity()` |
| Variables/Properties | camelCase | `currentDate`, `isSynced`, `patientName` |
| Constants | SCREAMING_SNAKE_CASE | `MAX_RETRY_COUNT`, `SYNC_INTERVAL_HOURS` |
| Enums | PascalCase class, SCREAMING_SNAKE values | `TimeSlot.MORNING`, `Severity.NO_PAIN` |
| Composable functions | PascalCase (like classes) | `TimeSlotTile()`, `SeverityPicker()`, `WeekStripBar()` |
| Packages | lowercase, no underscores | `com.healthtrend.app.ui.daycard` |

**Room Database Naming:**

| Element | Convention | Example |
|---------|-----------|---------|
| Table names | snake_case, plural | `health_entries`, `app_settings` |
| Column names | snake_case | `time_slot`, `updated_at`, `is_synced` |
| DAO classes | PascalCase + "Dao" suffix | `HealthEntryDao`, `AppSettingsDao` |
| Database class | PascalCase + "Database" suffix | `HealthTrendDatabase` |

**File Naming:**

| Element | Convention | Example |
|---------|-----------|---------|
| Kotlin files | PascalCase matching primary class | `HealthEntry.kt`, `DayCardScreen.kt` |
| Composable screens | PascalCase + "Screen" suffix | `DayCardScreen.kt`, `AnalyticsScreen.kt` |
| ViewModels | PascalCase + "ViewModel" suffix | `DayCardViewModel.kt` |
| Repositories | PascalCase + "Repository" suffix | `HealthEntryRepository.kt` |
| DI modules | PascalCase + "Module" suffix | `DatabaseModule.kt`, `SyncModule.kt` |
| Workers | PascalCase + "Worker" suffix | `SyncWorker.kt` |

**Resource Naming (Android):**

| Element | Convention | Example |
|---------|-----------|---------|
| String resources | snake_case with prefix | `label_morning`, `action_export_pdf`, `error_sign_in_failed` |
| Color resources | snake_case with semantic name | `severity_no_pain`, `severity_mild`, `surface_background` |
| Dimension resources | snake_case with element | `slot_tile_height`, `picker_circle_size` |
| Content descriptions | snake_case with cd_ prefix | `cd_morning_slot`, `cd_severity_picker` |

## Structure Patterns

**Package Organization:**

```
com.healthtrend.app/
├── data/
│   ├── local/          # Room: Database, DAOs, Entities
│   ├── remote/         # Google Sheets API service class
│   ├── repository/     # Repository implementations (the bridge)
│   └── sync/           # SyncWorker, sync logic
├── di/                 # Hilt @Module classes only
├── domain/
│   └── model/          # Severity, TimeSlot, domain enums/classes
├── ui/
│   ├── theme/          # Theme.kt, Color.kt, Type.kt, Shape.kt, AnimationSpec.kt
│   ├── components/     # Shared composables used by multiple screens
│   ├── daycard/        # DayCardScreen.kt, DayCardViewModel.kt
│   ├── analytics/      # AnalyticsScreen.kt, AnalyticsViewModel.kt
│   └── settings/       # SettingsScreen.kt, SettingsViewModel.kt
├── notification/       # NotificationScheduler.kt, BootReceiver.kt
└── HealthTrendApp.kt   # @HiltAndroidApp Application class
```

**Structural Rules:**

1. One ViewModel per screen. No shared ViewModels. If two screens need the same data, each gets its own ViewModel calling the same Repository.
2. Composables in `components/` only if shared across multiple screens. Screen-specific composables live in their screen's package.
3. No business logic in composables. Composables render state. ViewModels hold logic. Repositories access data.
4. Repository is the only data access path. ViewModels never touch DAOs or API services directly.

## State Management Patterns

**ViewModel State Pattern (mandatory for all screens):**

```kotlin
// 1. UiState is always a sealed interface
sealed interface DayCardUiState {
    data object Loading : DayCardUiState
    data class Ready(
        val date: LocalDate,
        val entries: Map<TimeSlot, HealthEntry?>,
        val expandedSlot: TimeSlot?,
        val isToday: Boolean
    ) : DayCardUiState
}

// 2. ViewModel exposes StateFlow, never MutableStateFlow publicly
class DayCardViewModel @Inject constructor(...) : ViewModel() {
    private val _uiState = MutableStateFlow<DayCardUiState>(DayCardUiState.Loading)
    val uiState: StateFlow<DayCardUiState> = _uiState.asStateFlow()
}

// 3. Composable collects with lifecycle awareness
@Composable
fun DayCardScreen(viewModel: DayCardViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
}
```

**State Management Rules:**

1. Always `StateFlow`, never `LiveData`, never `mutableStateOf` in ViewModel
2. Always `sealed interface` for UiState, never open classes or data-only classes
3. Always `collectAsStateWithLifecycle()`, never `collectAsState()`
4. Events flow up as lambdas: `onSeveritySelected: (TimeSlot, Severity) -> Unit`
5. One-shot events use `SharedFlow` with `replay = 0` (not currently needed in HealthTrend)

## Coroutine Patterns

| Context | Scope | Dispatcher |
|---------|-------|-----------|
| ViewModel operations | `viewModelScope` | `Dispatchers.Main` (default) |
| Room queries | `viewModelScope` | `Dispatchers.IO` (via Room's built-in) |
| WorkManager sync | `CoroutineWorker.doWork()` | `Dispatchers.Default` |
| Repository methods | Caller's scope | Suspend functions (let caller decide) |

**Coroutine Rules:**

1. Repository functions are always `suspend` — they never launch their own coroutines
2. ViewModels launch coroutines via `viewModelScope.launch { }`
3. Never use `GlobalScope` — all work is scoped to lifecycle
4. Room DAOs return `Flow<T>` for observable queries, `suspend` for one-shot operations

## Severity Data Model Pattern (Cross-Cutting)

Single source of truth for the severity model used across all layers:

```kotlin
enum class Severity(
    val displayName: String,
    val numericValue: Int,
    val color: Color,
    val softColor: Color,
    val iconDescription: String
) {
    NO_PAIN("No Pain", 0, Color(0xFF4CAF50), Color(0xFFE8F5E9), "check"),
    MILD("Mild", 1, Color(0xFFFFC107), Color(0xFFFFF8E1), "dash"),
    MODERATE("Moderate", 2, Color(0xFFFF9800), Color(0xFFFFF3E0), "exclamation"),
    SEVERE("Severe", 3, Color(0xFFF44336), Color(0xFFFFEBEE), "double_exclamation")
}

enum class TimeSlot(
    val displayName: String,
    val icon: String,
    val defaultReminderTime: String
) {
    MORNING("Morning", "☀", "08:00"),
    AFTERNOON("Afternoon", "☼", "13:00"),
    EVENING("Evening", "🌙", "18:00"),
    NIGHT("Night", "★", "22:00")
}
```

**Severity Model Rules:**

1. Never hardcode severity colors anywhere — always reference `Severity.color` or `Severity.softColor`
2. Never hardcode time slot labels — always reference `TimeSlot.displayName`
3. Numeric value mapping (0–3) used for: Room storage, analytics Y-axis, Google Sheets comparison
4. String display name used for: UI labels, severity pills, Google Sheets cell values, PDF text

## Error Handling Patterns

| Layer | Pattern | Example |
|-------|---------|---------|
| Repository | Return `Result<T>` or handle errors internally | `suspend fun saveSeverity(): Result<Unit>` |
| ViewModel | Catch errors, update UiState | `try { repo.save() } catch { _uiState.value = Error(...) }` |
| Sync Worker | Return `Result.retry()` or `Result.failure()` | WorkManager handles backoff |
| UI | Display based on UiState only | `when (state) { is Error -> ... }` |

**Error Handling Rules:**

1. Never throw exceptions from Repository to ViewModel — use `Result<T>` wrapper or catch internally
2. Sync errors are always silent — no user-facing error states for sync failures
3. Only user-visible errors: Google Sign-In failure (Credential Manager UI) and Sheet URL validation (red TextField outline)
4. Logcat tagging convention: Tag = class name. `Log.e("SyncWorker", "Sheets API failed", exception)`

## Animation Patterns

| Animation | Duration | Constant Name |
|-----------|----------|--------------|
| Severity picker expand | 200ms | `PICKER_EXPAND_DURATION` |
| Severity picker collapse | 0ms (instant) | N/A |
| Color fill bloom | 150ms | `COLOR_FILL_DURATION` |
| All-complete day bloom | 300ms | `COMPLETION_BLOOM_DURATION` |
| Day Card swipe | 250ms | `DAY_SWIPE_DURATION` |
| Screen tab switch | 300ms | `TAB_SWITCH_DURATION` |
| Picker dismiss | 150ms | `PICKER_DISMISS_DURATION` |

**Animation Rules:**

1. All durations defined as constants in a single `AnimationSpec.kt` file in `ui/theme/`
2. Always check `Settings.Global.ANIMATOR_DURATION_SCALE` — if system animations are disabled, skip all custom animations
3. No animation exceeds 300ms
4. Use `AnimatedVisibility` for show/hide. Use `animateColorAsState` for color transitions.

## Google Sheets Interaction Patterns

| Operation | Pattern |
|-----------|---------|
| Find row by date | Search Column A for date string (`YYYY-MM-DD`) |
| Write severity | Write to specific cell (e.g., `B5` for row 5, Morning column) |
| Write timestamp | Write epoch millis to corresponding `_ts` column |
| Read all data | Read range `A:I` for all rows |
| Create new row | Append to first empty row |

**Google Sheets Rules:**

1. Date format in Sheet Column A: `YYYY-MM-DD` (ISO 8601, e.g., `2026-02-07`)
2. Severity text in Sheet: Exact display name — `No Pain`, `Mild`, `Moderate`, `Severe`
3. Timestamp in Sheet: Epoch milliseconds as a number (e.g., `1738900500000`)
4. Never write empty string or null to a cell that might have data from another device
5. Always read before write during push phase to compare timestamps

## Enforcement Guidelines

**All AI Agents MUST:**

1. Follow naming conventions exactly — no deviations for "readability" or "preference"
2. Use the Severity and TimeSlot enums as the single source of truth — never create parallel models
3. Route all data access through Repositories — never access DAOs from ViewModels
4. Use StateFlow + sealed interface for all screen state — no LiveData, no mutableStateOf in ViewModels
5. Define all animation durations in AnimationSpec.kt — no inline magic numbers
6. Follow the sync protocol exactly: push (compare timestamps, write if newer) then pull (compare timestamps, accept if newer)
7. Never surface sync errors to the user — silence is trust
