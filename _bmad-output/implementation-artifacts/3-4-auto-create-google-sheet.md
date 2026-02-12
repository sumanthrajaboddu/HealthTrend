# Story 3.4: Auto-Create Google Sheet on Sign-In

Status: done

## Story

As Raja (the admin),
I want the app to automatically create a Google Sheet when I sign in with Google,
So that sync is ready immediately without manually creating and pasting a Sheet URL.

## Acceptance Criteria

1. **Given** Raja signs in with Google, **When** sign-in succeeds and no Sheet URL exists, **Then** a Google Sheet titled "HealthTrend" is auto-created in the signed-in account and its URL saved to AppSettings.
2. **Given** the Sheet is auto-created, **When** creation completes, **Then** the Sheet has a header row: Date, Morning, Afternoon, Evening, Night.
3. **Given** a Sheet URL already exists in settings, **When** Raja signs in (or re-signs in), **Then** no new Sheet is created — existing URL preserved.
4. **Given** Sheet creation fails (network/quota), **When** sign-in still succeeds, **Then** auth state is SignedIn, and sheet creation retries silently on next sync trigger or app launch.
5. **Given** Raja views Settings after sign-in, **When** the Sync section renders, **Then** the Sheet URL is shown as read-only text (not an editable field), with the Share button still functional.
6. **Given** no Sheet URL exists yet, **When** Settings renders, **Then** shows "Sheet will be created on sign-in" neutral message.
7. **Given** TalkBack enabled, **When** user focuses on Sheet status, **Then** announces the Sheet URL or status clearly.

## Tasks / Subtasks

- [x] Task 1: Add createSheet to SheetsClient + GoogleSheetsService (AC: #1, #2)
  - [x] 1.1 Add `createSheet(accountEmail: String, title: String): String` to `SheetsClient` interface
  - [x] 1.2 Implement in `GoogleSheetsService` using Sheets API v4 `spreadsheets.create`
  - [x] 1.3 Set header row: Date, Morning, Afternoon, Evening, Night
  - [x] 1.4 Return full Sheet URL (`https://docs.google.com/spreadsheets/d/{id}`)

- [x] Task 2: Wire auto-creation into SettingsViewModel.onSignIn() (AC: #1, #3)
  - [x] 2.1 Inject `SheetsClient` into `SettingsViewModel`
  - [x] 2.2 After `GoogleSignInResult.Success`, check if `sheetUrl` is empty
  - [x] 2.3 If empty, call `sheetsClient.createSheet()` on `Dispatchers.IO`
  - [x] 2.4 Save URL via `appSettingsRepository.updateSheetUrl()`
  - [x] 2.5 If Sheet URL already exists, skip creation (AC #3)

- [x] Task 3: Add retry logic for sheet creation on app launch (AC: #4)
  - [x] 3.1 In `HealthTrendApplication.onCreate()`, check if signed in but no Sheet URL
  - [x] 3.2 If so, attempt sheet creation silently in background
  - [x] 3.3 Failure is silent — no error shown, next launch retries

- [x] Task 4: Update Settings UI (AC: #5, #6)
  - [x] 4.1 Replace editable Sheet URL `OutlinedTextField` with read-only `Text`
  - [x] 4.2 Show truncated Sheet URL when present, "Sheet will be created on sign-in" when absent
  - [x] 4.3 Keep Share Sheet Link button (enabled when URL exists)
  - [x] 4.4 Remove `onSheetUrlChanged` callback chain
  - [x] 4.5 Remove `isSheetUrlValid` from `SettingsUiState`
  - [x] 4.6 Remove `onSheetUrlChanged()` and `isValidSheetUrl()` from ViewModel

- [x] Task 5: Update TalkBack accessibility (AC: #7)
  - [x] 5.1 Sheet status: "Google Sheet URL, [url]. Read only." or "Google Sheet, not created yet."
  - [x] 5.2 Share button: updated disabled state message

- [x] Task 6: Update tests
  - [x] 6.1 Add `createSheet()` to `FakeSheetsClient`
  - [x] 6.2 Add ViewModel tests: auto-creation on sign-in, skip when URL exists, creation failure
  - [x] 6.3 Remove obsolete URL validation tests
  - [x] 6.4 Update existing tests for removed `isSheetUrlValid` field

## Dev Notes

### Architecture Compliance

- OAuth scope `SheetsScopes.SPREADSHEETS` already includes create permission — no scope change
- `SheetsClient` interface already has a fake — adding `createSheet()` maintains testability
- Sheet title is always "HealthTrend" — not parameterized
- Existing sync protocol (push/pull in `SyncLogic`) is unaffected — reads `sheetUrl` from settings
- `onSheetUrlChanged()` and `isValidSheetUrl()` become dead code and are removed

### Sheet Creation Flow

```
Sign-in success → check sheetUrl empty → createSheet("HealthTrend") → save URL → sync ready
```

### Retry Flow

```
App launch → check signed in + no sheetUrl → createSheet() → save URL
              ↓ (failure)
              silent skip → retry on next launch
```

### UX Constraints

- NO manual Sheet URL entry — auto-created
- Sheet URL displayed read-only — not editable
- Share Sheet Link button preserved
- NO error messages for sheet creation failures — silent retry

### References

- [Source: project-context.md#Google Sign-In (Credential Manager)]
- [Source: epic-3-app-configuration-google-sheets-sync.md]

## Dev Agent Record

### Agent Model Used
claude-4.6-opus-high-thinking (Cursor IDE)

### Debug Log References
- `./gradlew :app:testDebugUnitTest` — BUILD SUCCESSFUL (all unit tests pass)
- `./gradlew :app:assembleDebug` — BUILD SUCCESSFUL

### Completion Notes List
- **Task 1:** Added `createSheet(accountEmail, title): String` to `SheetsClient` interface. Implemented in `GoogleSheetsService` using Sheets API v4 `Spreadsheet` + `SpreadsheetProperties`. Header row (Date, Morning, Afternoon, Evening, Night) written via `values().update()` to `Sheet1!A1:E1`. Returns full URL `https://docs.google.com/spreadsheets/d/{id}`.
- **Task 2:** Injected `SheetsClient` into `SettingsViewModel`. `onSignIn()` now calls `ensureSheetExists(email)` after `GoogleSignInResult.Success`. Method checks `sheetUrl.isEmpty()` before creating; skips if URL exists (AC #3). Runs on `Dispatchers.IO`. Failures caught silently (AC #4).
- **Task 3:** Added `retrySheetCreationIfNeeded()` to `HealthTrendApplication.onCreate()` via `applicationScope.launch`. Checks if signed in (`googleAccountEmail` non-empty) AND `sheetUrl` is empty. If so, calls `sheetsClient.createSheet()` and saves URL. Failures are silent.
- **Task 4:** Replaced editable `OutlinedTextField` for Sheet URL with read-only `SheetStatusSection` composable. Shows URL when present (maxLines=2), "Sheet will be created on sign-in" when absent. Share button preserved (enabled when URL exists). Removed `onSheetUrlChanged` callback, `isSheetUrlValid` from `SettingsUiState`, and `isValidSheetUrl()`/`SHEETS_URL_PATTERN` from ViewModel.
- **Task 5:** TalkBack: Sheet URL announces "Google Sheet URL. [url]. Read only." Empty state announces "Google Sheet, not created yet." Share button disabled state: "Sign in to create a sheet first."
- **Task 6:** Added `createSheet()` to `FakeSheetsClient` with tracking (`createdSheets`, `createSheetShouldFail`, `createSheetReturnUrl`). Added 7 new ViewModel tests: auto-creation on sign-in, skip when URL exists, creation failure preserves SignedIn, failure/cancelled don't attempt creation, correct title constant, title via onSignIn flow. Removed 8 obsolete tests (URL validation, `onSheetUrlChanged`, `isSheetUrlValid`). Fixed pre-existing broken `GoogleAuthManagerTest`.

### File List
- `app/src/main/java/com/healthtrend/app/data/sync/GoogleSheetsService.kt` — MODIFIED (added `createSheet()` to interface + implementation, new imports)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsViewModel.kt` — MODIFIED (injected `SheetsClient`, added `ensureSheetExists()`, removed `onSheetUrlChanged`/`isValidSheetUrl`/`SHEETS_URL_PATTERN`)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsScreen.kt` — MODIFIED (replaced Sheet URL field with `SheetStatusSection`, removed `onSheetUrlChanged` callback)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsUiState.kt` — MODIFIED (removed `isSheetUrlValid` from `Success`)
- `app/src/main/java/com/healthtrend/app/HealthTrendApplication.kt` — MODIFIED (added `retrySheetCreationIfNeeded()`, injected `SheetsClient`)
- `app/src/main/java/com/healthtrend/app/data/auth/GoogleAuthManager.kt` — MODIFIED (added debug logging)
- `app/src/test/java/com/healthtrend/app/data/sync/FakeSheetsClient.kt` — MODIFIED (added `createSheet()` fake)
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsViewModelTest.kt` — MODIFIED (added 7 auto-creation tests, removed 8 obsolete, updated constructor)
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsUiStateTest.kt` — MODIFIED (removed `isSheetUrlValid` tests)
- `app/src/test/java/com/healthtrend/app/data/auth/GoogleAuthManagerTest.kt` — MODIFIED (fixed pre-existing broken test)

## Senior Developer Review (AI)

### Reviewer

Amelia (Dev Agent) — 2026-02-11

### Findings Addressed

- **HIGH:** Replaced hardcoded header row `listOf("Date", "Morning", ...)` in `GoogleSheetsService.createSheet()` with `listOf("Date") + TimeSlot.entries.map { it.displayName }` to comply with project-context rule: "NEVER hardcode time slot labels."
- **HIGH:** Eliminated duplicated `SHEET_TITLE` constant between `SettingsViewModel` and `HealthTrendApplication`. Added `SheetsClient.DEFAULT_SHEET_TITLE` as canonical source; both consumers now reference it.
- **MEDIUM:** Fixed blanket `catch (_: Exception)` in `SettingsViewModel.ensureSheetExists()` and `HealthTrendApplication.retrySheetCreationIfNeeded()` — now rethrows `CancellationException` to preserve structured concurrency.
- **MEDIUM:** Extracted `SheetCreationRetrier` from `HealthTrendApplication` (same pattern as `BootReminderRegistrar` in 4.2). Added 8 unit tests covering: create on retry, skip when no settings, skip when no email, skip when null email, skip when URL exists, URL persistence, creation failure propagation, title constant verification.
- **MEDIUM:** Added `sprint-status.yaml` to File List (was modified by git but undocumented).
- **LOW:** Added `sheetCreationInProgress` state to `SettingsUiState.Success` + `SettingsViewModel` + `SheetStatusSection` UI. Now shows "Creating sheet..." with TalkBack "Google Sheet, creating now." during active sheet creation, instead of the misleading "Sheet will be created on sign-in." Flag set via `_sheetCreationInProgress` MutableStateFlow in `ensureSheetExists()` with `finally` block for guaranteed cleanup. Added 4 ViewModel tests + 1 UiState test for the new field.
- **LOW:** `GoogleAuthManager.kt` debug logging changes noted as tangential to Story 3.4 scope — no code change needed, retained for auth debugging utility.

### Validation Run

- `./gradlew :app:testDebugUnitTest --tests com.healthtrend.app.ui.settings.SettingsViewModelTest --tests com.healthtrend.app.data.sync.SheetCreationRetrierTest --tests com.healthtrend.app.ui.settings.SettingsUiStateTest`
- Result: **BUILD SUCCESSFUL** (all HIGH, MEDIUM, and LOW fixes)

### Review Fix File List (2026-02-11)

- `app/src/main/java/com/healthtrend/app/data/sync/GoogleSheetsService.kt` — MODIFIED (added `SheetsClient.DEFAULT_SHEET_TITLE` companion constant; header row uses `TimeSlot.entries.map`)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsViewModel.kt` — MODIFIED (`SHEET_TITLE` references `SheetsClient.DEFAULT_SHEET_TITLE`; `ensureSheetExists()` rethrows `CancellationException`; added `_sheetCreationInProgress` flow)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsUiState.kt` — MODIFIED (added `sheetCreationInProgress: Boolean` to `Success`)
- `app/src/main/java/com/healthtrend/app/ui/settings/SettingsScreen.kt` — MODIFIED (`SheetStatusSection` shows "Creating sheet..." state with TalkBack)
- `app/src/main/java/com/healthtrend/app/HealthTrendApplication.kt` — MODIFIED (delegates to `SheetCreationRetrier`; removed duplicated `SHEET_TITLE`; rethrows `CancellationException`)
- `app/src/main/java/com/healthtrend/app/data/sync/SheetCreationRetrier.kt` — NEW (testable retry logic extracted from Application)
- `app/src/test/java/com/healthtrend/app/data/sync/SheetCreationRetrierTest.kt` — NEW (8 unit tests for app-launch retry path)
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsViewModelTest.kt` — MODIFIED (4 new `sheetCreationInProgress` tests)
- `app/src/test/java/com/healthtrend/app/ui/settings/SettingsUiStateTest.kt` — MODIFIED (added `sheetCreationInProgress` default assertion)
- `_bmad-output/implementation-artifacts/sprint-status.yaml` — MODIFIED (documented; was missing from original File List)

## Change Log

- 2026-02-11: Senior review fixes applied for Story 3.4 all findings (2 high, 3 medium, 2 low). Story status remains `done` after targeted unit tests passed.
- 2026-02-11: Story 3.4 implemented and all tests passing. Story status set to `done`.
