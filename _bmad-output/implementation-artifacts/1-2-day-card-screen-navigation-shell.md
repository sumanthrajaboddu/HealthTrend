# Story 1.2: Day Card Screen & Navigation Shell

Status: review

## Story

As Uncle (the patient),
I want to open the app and see today's Day Card with four time slots showing their current state,
So that I know at a glance what I've logged and what's still empty.

## Acceptance Criteria

1. **Given** the app launches for the first time, **When** the Day Card screen renders, **Then** today's date is in the top app bar, four time slot tiles (Morning, Afternoon, Evening, Night) shown vertically with empty dash state. Tiles: 64dp+ height, 12dp gaps, 16dp horizontal margins, full-width single-column.
2. **Given** it is currently 2:30 PM, **When** the Day Card renders, **Then** the Afternoon tile has a subtle highlight distinguishing it from other slots.
3. **Given** the user previously logged "Mild" for Morning, **When** Day Card loads today's entries from Room, **Then** Morning tile displays amber "Mild" color, "Mild" text label, and the mild icon.
4. **Given** bottom navigation bar is visible, **When** user taps "Today" tab, **Then** Day Card for today is displayed. Three nav tabs visible: Today, Analytics, Settings — Analytics and Settings show placeholder screens.
5. **Given** TalkBack is enabled, **When** user navigates to empty Morning tile, **Then** TalkBack announces "Morning, not logged. Double tap to log severity."
6. **Given** system font is 1.5x, **When** Day Card renders, **Then** all text scales with sp units, tiles grow in height without clipping.

## Tasks / Subtasks

- [x] Task 1: Set up navigation shell with bottom navigation (AC: #4)
  - [x] 1.1 Create `NavHost` with 3 flat routes: `daycard`, `analytics`, `settings`
  - [x] 1.2 Create bottom navigation bar with Today, Analytics, Settings tabs
  - [x] 1.3 Create placeholder composables for Analytics and Settings screens
  - [x] 1.4 Wire `MainActivity` with `@AndroidEntryPoint`, `setContent { HealthTrendTheme { ... } }`

- [x] Task 2: Create DayCardViewModel (AC: #1, #2, #3)
  - [x] 2.1 Create `DayCardViewModel` with `@HiltViewModel` injecting `HealthEntryRepository`
  - [x] 2.2 Define `DayCardUiState` as `sealed interface` with Loading, Success, Error states
  - [x] 2.3 Success state holds: `date: LocalDate`, `entries: Map<TimeSlot, HealthEntry?>`, `currentTimeSlot: TimeSlot?`
  - [x] 2.4 Load today's entries via `repository.getEntriesForDate(today)` as `Flow` collected to `StateFlow`
  - [x] 2.5 Determine current time slot from device time for subtle highlight

- [x] Task 3: Build Day Card screen composable (AC: #1, #2, #6)
  - [x] 3.1 Create `DayCardScreen.kt` in `ui/daycard/` package
  - [x] 3.2 Top app bar showing formatted date (e.g., "Saturday, February 7")
  - [x] 3.3 Vertical column of 4 `TimeSlotTile` composables with 12dp gaps and 16dp horizontal padding
  - [x] 3.4 Use `hiltViewModel()` to get `DayCardViewModel`
  - [x] 3.5 Collect UiState with `collectAsStateWithLifecycle()`

- [x] Task 4: Build TimeSlotTile composable (AC: #1, #2, #3, #5, #6)
  - [x] 4.1 Create `TimeSlotTile.kt` in `ui/daycard/`
  - [x] 4.2 Empty state: dash "—", neutral background, 64dp+ height
  - [x] 4.3 Logged state: severity color background, severity text label, severity icon — triple encoding
  - [x] 4.4 Current time slot: subtle highlight (e.g., slightly elevated or bordered)
  - [x] 4.5 Display `TimeSlot.displayName` — NEVER hardcode "Morning" etc.
  - [x] 4.6 Display severity using `Severity.color`, `Severity.displayName` — NEVER hardcode hex colors
  - [x] 4.7 All text in `sp`, tile height in `dp`, minimum 64dp
  - [x] 4.8 Full-width layout (fills available width minus 16dp margins)

- [x] Task 5: Implement TalkBack accessibility (AC: #5)
  - [x] 5.1 Add `Modifier.semantics { }` to `TimeSlotTile` with descriptive content labels
  - [x] 5.2 Empty tile: "Morning, not logged. Double tap to log severity."
  - [x] 5.3 Logged tile: "Morning, currently Mild. Tap to change severity."
  - [x] 5.4 Current slot indicator accessible via semantics
  - [x] 5.5 Focus order: Top App Bar → Time Slot Tiles → Bottom Navigation

## Dev Notes

### Architecture Compliance

- **One ViewModel per screen:** `DayCardViewModel` for the Day Card screen only
- **UiState:** `sealed interface DayCardUiState` — NEVER data class
- **State collection:** `collectAsStateWithLifecycle()` — NEVER `collectAsState()`
- **No DAO access from ViewModel:** Always through `HealthEntryRepository`
- **Screen composables:** In `ui/daycard/` package. Shared composables (used by 2+ screens) in `ui/components/`
- **No screen imports composables from another screen's package**

### UX Constraints (CRITICAL)

- NO toast, snackbar, "Saved!" — color fill IS the confirmation
- NO loading spinners on Day Card — data loads from local Room, should be instant
- NO onboarding, tutorials, tooltips — Day Card is first and only screen on launch
- Empty states: calm, neutral, "—" only. No illustrations, no motivational text
- Portrait-only orientation

### Severity Display Rules

- **Triple encoding required:** color + text label + icon. NEVER color alone
- All colors from `Severity.color` / `Severity.softColor` — no hardcoded hex
- All labels from `Severity.displayName` — no hardcoded strings
- All slot names from `TimeSlot.displayName` — no hardcoded "Morning" etc.

### Accessibility Requirements

- Touch targets: 48dp minimum, Day Card tiles 64dp+ height
- 12dp gaps between tiles
- All text in `sp` for font scaling support up to 1.5x
- Respect `Settings.Global.ANIMATOR_DURATION_SCALE`
- TalkBack: descriptive content labels conveying purpose, state, action
- Focus order: Top App Bar → Week Strip (Story 2.2) → Time Slot Tiles → Bottom Nav

### Project Structure Notes

```
ui/
├── daycard/
│   ├── DayCardScreen.kt          # Screen composable
│   ├── DayCardViewModel.kt       # @HiltViewModel
│   ├── DayCardUiState.kt         # sealed interface
│   └── TimeSlotTile.kt           # Tile composable
├── analytics/
│   └── AnalyticsPlaceholder.kt   # Placeholder for now
├── settings/
│   └── SettingsPlaceholder.kt    # Placeholder for now
├── navigation/
│   └── HealthTrendNavHost.kt     # NavHost + bottom nav
└── theme/                         # From Story 1.1
```

### Dependencies on Story 1.1

- Requires: `HealthEntry`, `Severity`, `TimeSlot` enums, `HealthEntryRepository`, Room DB, Hilt modules, Theme
- This story adds the first screen — everything from 1.1 must be in place

### References

- [Source: project-context.md#Kotlin & Compose Rules]
- [Source: project-context.md#UX Constraints]
- [Source: project-context.md#Accessibility]
- [Source: project-context.md#Project Structure Rules]
- [Source: requirements-inventory.md#FR1, FR7, FR8, FR15, FR16, FR17]
- [Source: requirements-inventory.md#NFR1, NFR2, NFR13-NFR20]
- [Source: epic-1-day-card-symptom-logging.md#Story 1.2]

## Dev Agent Record

### Agent Model Used

Claude claude-4.6-opus (via Cursor IDE)

### Debug Log References

- No build environment (Java/Gradle) available in agent environment. Tests written and structurally verified but could not be executed. Must be run in Android Studio.

### Completion Notes List

- **Task 1 (Navigation Shell):** Created `HealthTrendNavHost.kt` with `NavHost` hosting 3 flat routes (`daycard`, `analytics`, `settings`). `BottomNavDestination` enum defines routes, labels, and icons. Bottom navigation bar with Today/Analytics/Settings tabs using Material 3 `NavigationBar` + `NavigationBarItem`. Placeholder screens created for Analytics and Settings. `MainActivity` updated to wire `HealthTrendNavHost` inside `HealthTrendTheme`.
- **Task 2 (DayCardViewModel):** Created `DayCardViewModel` with `@HiltViewModel` injecting `HealthEntryRepository` and `TimeProvider`. `DayCardUiState` sealed interface with Loading/Success/Error. Success holds `date: LocalDate`, `entries: Map<TimeSlot, HealthEntry?>`, `currentTimeSlot: TimeSlot`. Loads today's entries via `repository.getEntriesByDate()` Flow collected to StateFlow. Current time slot determined by hour ranges (Morning 6-11, Afternoon 12-16, Evening 17-20, Night 21-5). `TimeProvider` interface + `SystemTimeProvider` created for testability; bound via `AppModule` Hilt module.
- **Task 3 (DayCardScreen):** Created `DayCardScreen.kt` in `ui/daycard/`. `CenterAlignedTopAppBar` displays formatted date ("EEEE, MMMM d" pattern). Vertical `LazyColumn` of 4 `TimeSlotTile` composables with 12dp spacing and 16dp horizontal padding. Uses `hiltViewModel()` and `collectAsStateWithLifecycle()`. No loading spinner per UX rules.
- **Task 4 (TimeSlotTile):** Created `TimeSlotTile.kt` in `ui/daycard/`. Empty state: em-dash, `surfaceContainerLow` background, 64dp min height. Logged state: `severity.softColor` background, triple encoding (severity icon + text + color). Current slot: primary color 2dp border. All text from `TimeSlot.displayName` and `Severity.displayName` — zero hardcoded strings/colors. Severity icons mapped via UI-layer extension function (`SentimentVerySatisfied`, `SentimentSatisfied`, `SentimentNeutral`, `SentimentVeryDissatisfied`).
- **Task 5 (TalkBack):** `Modifier.semantics(mergeDescendants = true)` on tile with `contentDescription`. Empty: "{Slot}, not logged. Double tap to log severity." Logged: "{Slot}, currently {Severity}. Tap to change severity." Current slot: `stateDescription = "Current time slot"`. Focus order follows composition order: TopAppBar → Tiles → BottomNav. Top app bar title has `heading()` semantics.
- **Tests written:** `BottomNavDestinationTest` (8 tests), `DayCardUiStateTest` (6 tests), `DayCardViewModelTest` (14 tests). Test fakes: `FakeHealthEntryDao`, `FakeTimeProvider`.
- **Design decisions:** Used `TimeProvider` interface instead of direct `LocalDate.now()` to enable deterministic ViewModel testing. Severity icons defined as UI-layer extension (`Severity.icon()`) rather than modifying the Story 1.1 enum. `LazyColumn` used for tile list (extensible for future scrolling needs).

### File List

**New files:**
- `app/src/main/java/com/healthtrend/app/ui/navigation/HealthTrendNavHost.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardScreen.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardViewModel.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardUiState.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/TimeSlotTile.kt`
- `app/src/main/java/com/healthtrend/app/ui/analytics/AnalyticsPlaceholderScreen.kt`
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsPlaceholderScreen.kt`
- `app/src/main/java/com/healthtrend/app/util/TimeProvider.kt`
- `app/src/main/java/com/healthtrend/app/di/AppModule.kt`
- `app/src/test/java/com/healthtrend/app/ui/navigation/BottomNavDestinationTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/daycard/DayCardViewModelTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/daycard/DayCardUiStateTest.kt`
- `app/src/test/java/com/healthtrend/app/data/local/FakeHealthEntryDao.kt`
- `app/src/test/java/com/healthtrend/app/util/FakeTimeProvider.kt`

**Modified files:**
- `app/src/main/java/com/healthtrend/app/MainActivity.kt`

## Change Log

- **2026-02-08:** Story 1.2 implemented — Day Card screen with navigation shell, DayCardViewModel, TimeSlotTile with triple-encoded severity display, TalkBack accessibility, bottom navigation with Today/Analytics/Settings tabs. 28 unit tests added across 3 test files.
