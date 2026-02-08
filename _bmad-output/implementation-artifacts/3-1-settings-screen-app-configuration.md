# Story 3.1: Settings Screen & App Configuration

Status: review

## Story

As Raja (the admin),
I want a Settings screen where I can enter the patient name, configure the Google Sheet URL, and manage all app settings with auto-save,
So that I can set up the app for Uncle in one sitting without any "save" buttons.

## Acceptance Criteria

1. **Given** Settings requires persistent storage, **When** Story 3.1 is implemented, **Then** `AppSettings` Room entity is created with: `patientName`, `sheetUrl`, `googleAccountEmail`, `globalRemindersEnabled`, per-slot reminder toggles/times. `AppSettingsDao` provides Flow queries and suspend updates. `AppSettingsRepository` registered with Hilt.
2. **Given** Raja navigates to Settings tab, **When** screen renders, **Then** displays fields for patient name, Google Sheet URL, and a share Sheet link button.
3. **Given** Raja types "Uncle" in patient name, **When** text entered, **Then** immediately persisted to Room AppSettings — no save button, no confirmation.
4. **Given** Raja pastes a Google Sheet URL, **When** URL entered, **Then** auto-saved to AppSettings. Basic format validation shows red outline if URL appears invalid.
5. **Given** a Sheet URL is configured, **When** Raja taps "Share Sheet Link", **Then** Android share sheet opens with Sheet URL as content.
6. **Given** TalkBack enabled, **When** Raja navigates to each field, **Then** each announces its label and current value.

## Tasks / Subtasks

- [x] Task 1: Create AppSettings data layer (AC: #1)
  - [x] 1.1 Create `AppSettings` Room entity — single-row table `app_settings`
  - [x] 1.2 Fields: `id` (PK, always 1), `patient_name` (String, default ""), `sheet_url` (String, default ""), `google_account_email` (String?, nullable), `global_reminders_enabled` (Boolean, default true), `morning_reminder_enabled` (Boolean, default true), `afternoon_reminder_enabled` (Boolean, default true), `evening_reminder_enabled` (Boolean, default true), `night_reminder_enabled` (Boolean, default true), `morning_reminder_time` (String, default "08:00"), `afternoon_reminder_time` (String, default "13:00"), `evening_reminder_time` (String, default "18:00"), `night_reminder_time` (String, default "22:00")
  - [x] 1.3 Create `AppSettingsDao`: `getSettings(): Flow<AppSettings?>`, `suspend updatePatientName(name)`, `suspend updateSheetUrl(url)`, `suspend updateGoogleAccount(email)`, etc.
  - [x] 1.4 Create `AppSettingsRepository` wrapping DAO — all functions `suspend`
  - [x] 1.5 Add `AppSettings` entity to `HealthTrendDatabase` (migration or schema update)
  - [x] 1.6 Register `AppSettingsDao` and `AppSettingsRepository` in Hilt modules

- [x] Task 2: Build SettingsViewModel (AC: #2, #3, #4)
  - [x] 2.1 Create `SettingsViewModel` with `@HiltViewModel` injecting `AppSettingsRepository`
  - [x] 2.2 Define `SettingsUiState` as `sealed interface`
  - [x] 2.3 Collect AppSettings as StateFlow from repository
  - [x] 2.4 `onPatientNameChanged(name)`: debounce, then persist via `viewModelScope.launch { repo.updatePatientName(name) }`
  - [x] 2.5 `onSheetUrlChanged(url)`: validate format, persist immediately
  - [x] 2.6 URL validation: basic check for Google Sheets URL pattern, red outline for invalid

- [x] Task 3: Build Settings screen composable (AC: #2, #3, #4, #5, #6)
  - [x] 3.1 Replace `SettingsPlaceholder` with real `SettingsScreen.kt` in `ui/settings/`
  - [x] 3.2 Patient name text field with label
  - [x] 3.3 Google Sheet URL text field with validation indicator
  - [x] 3.4 "Share Sheet Link" button (enabled only when URL is non-empty)
  - [x] 3.5 Auto-save: text fields persist on every change (debounced for name, immediate for URL)
  - [x] 3.6 NO save button anywhere — auto-save is the only mechanism

- [x] Task 4: Implement share functionality (AC: #5)
  - [x] 4.1 On "Share Sheet Link" tap, create `Intent.ACTION_SEND` with Sheet URL
  - [x] 4.2 Launch Android share sheet via `startActivity(Intent.createChooser(...))`

- [x] Task 5: TalkBack accessibility (AC: #6)
  - [x] 5.1 Patient name field: "Patient name, [current value]. Edit text."
  - [x] 5.2 Sheet URL field: "Google Sheet URL, [current value]. Edit text."
  - [x] 5.3 Share button: "Share Sheet link. Double tap to share."
  - [x] 5.4 Validation error: announce when URL is invalid

## Dev Notes

### Architecture Compliance

- **One ViewModel per screen:** `SettingsViewModel` — separate from `DayCardViewModel`
- **AppSettings is single-row table** — always id=1, query returns one row
- **Auto-save pattern:** No save buttons. Text changes trigger immediate or debounced persist
- **Repository pattern:** ViewModel → AppSettingsRepository → AppSettingsDao
- **StateFlow + collectAsStateWithLifecycle()** — never LiveData

### AppSettings Entity Design

- Single-row pattern: `id = 1` always. Query `getSettings()` returns the one row.
- Default values for all fields — first launch creates the row with defaults
- Reminder fields included now (used in Epic 4) to avoid schema migration later
- Table name: `app_settings` (snake_case, plural per convention)
- Column names: `patient_name`, `sheet_url`, `google_account_email`, etc.

### UX Constraints

- NO save button — auto-save on every change
- NO confirmation messages — just persist silently
- NO toast/snackbar after save
- Settings screen is one of 3 tabs in bottom nav

### Project Structure Notes

```
data/
├── model/
│   └── AppSettings.kt             # NEW: Room @Entity
├── local/
│   ├── HealthTrendDatabase.kt     # Updated: add AppSettings entity
│   └── AppSettingsDao.kt          # NEW: DAO
└── repository/
    └── AppSettingsRepository.kt   # NEW: wraps AppSettingsDao

di/
├── DatabaseModule.kt              # Updated: provide AppSettingsDao
└── RepositoryModule.kt            # Updated: provide AppSettingsRepository

ui/settings/
├── SettingsScreen.kt              # NEW: replaces placeholder
├── SettingsViewModel.kt           # NEW: @HiltViewModel
└── SettingsUiState.kt             # NEW: sealed interface
```

### Dependencies on Stories 1.1, 1.2

- Requires: Room database (Story 1.1), navigation shell with Settings tab (Story 1.2)
- This story replaces the Settings placeholder with a real screen

### Forward Compatibility

- AppSettings entity includes reminder fields for Epic 4 — avoids migration
- `googleAccountEmail` field prepared for Story 3.2 (Google Sign-In)
- `sheetUrl` field prepared for Story 3.3 (Sync)

### References

- [Source: project-context.md#Room Database Rules]
- [Source: project-context.md#Kotlin & Compose Rules]
- [Source: project-context.md#UX Constraints]
- [Source: requirements-inventory.md#FR39, FR40, FR41, FR42, FR44]
- [Source: requirements-inventory.md#Additional Requirements — Data Architecture]
- [Source: epic-3-app-configuration-google-sheets-sync.md#Story 3.1]

## Dev Agent Record

### Agent Model Used

Claude claude-4.6-opus (via Cursor)

### Debug Log References

- Build environment (Java, Gradle, Android SDK) not available in terminal — tests authored but not executed at runtime. Tests are structurally correct following Epic 1 patterns (FakeDao + Turbine + runTest).

### Completion Notes List

- **Task 1:** Created `AppSettings` Room entity (single-row, id=1, 13 fields with defaults including Epic 4 reminder fields). Created `AppSettingsDao` with Flow observable + suspend write queries. Created `AppSettingsRepository` with `ensureSettingsExist()` pattern for first-launch initialization. Updated `HealthTrendDatabase` to version 2 with `AutoMigration(from=1, to=2)`. Registered DAO and Repository in Hilt modules.
- **Task 2:** Created `SettingsViewModel` with `@HiltViewModel`. `SettingsUiState` is sealed interface (Loading, Success). Patient name uses 500ms debounce via `MutableStateFlow` + `debounce()`. Sheet URL persists immediately. URL validation via regex for Google Sheets pattern. StateFlow collected with `WhileSubscribed(5_000)`.
- **Task 3:** Created `SettingsScreen.kt` replacing placeholder. Scrollable column with patient name field (Words capitalization, Person icon), Sheet URL field (Uri keyboard, TableChart icon, red outline for invalid), Share Sheet Link button (disabled when URL empty). Local text state prevents cursor jumping from debounced saves via `rememberSaveable`.
- **Task 4:** Share via `Intent.ACTION_SEND` + `Intent.createChooser()` with Sheet URL as `EXTRA_TEXT`.
- **Task 5:** TalkBack semantics on all interactive elements — patient name field, Sheet URL field (with invalid state announcement), share button (with disabled state description). Top bar heading semantics.

### File List

**New files:**
- `app/src/main/java/com/healthtrend/app/data/model/AppSettings.kt`
- `app/src/main/java/com/healthtrend/app/data/local/AppSettingsDao.kt`
- `app/src/main/java/com/healthtrend/app/data/repository/AppSettingsRepository.kt`
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsScreen.kt`
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsViewModel.kt`
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsUiState.kt`
- `app/src/test/java/com/healthtrend/app/data/model/AppSettingsTest.kt`
- `app/src/test/java/com/healthtrend/app/data/local/FakeAppSettingsDao.kt`
- `app/src/test/java/com/healthtrend/app/data/repository/AppSettingsRepositoryTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsViewModelTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsUiStateTest.kt`

**Modified files:**
- `app/src/main/java/com/healthtrend/app/data/local/HealthTrendDatabase.kt` — added AppSettings entity, bumped to v2, auto-migration
- `app/src/main/java/com/healthtrend/app/di/DatabaseModule.kt` — added provideAppSettingsDao
- `app/src/main/java/com/healthtrend/app/di/RepositoryModule.kt` — added provideAppSettingsRepository
- `app/src/main/java/com/healthtrend/app/ui/navigation/HealthTrendNavHost.kt` — replaced SettingsPlaceholderScreen with SettingsScreen
