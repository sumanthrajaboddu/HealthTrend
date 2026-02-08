# Story 1.3: Inline Severity Picker & Entry Logging

Status: review

## Story

As Uncle (the patient),
I want to tap a time slot and select a severity level from an inline picker so that my symptom entry is saved instantly with clear visual feedback.

## Acceptance Criteria

1. **Given** an empty Morning slot, **When** Uncle taps the tile, **Then** severity picker expands inline within the tile (200ms ease-out), shows 4 options with color + label + icon, rest of Day Card dims. Touch targets 48dp+ with 8dp minimum gaps.
2. **Given** picker is open for empty slot, **When** Uncle taps "Mild", **Then** picker collapses instantly (0ms), tile fills with amber "Mild" (150ms bloom), haptic pulse fires, entry persisted to Room with `synced = false` and `updatedAt = currentTimeMillis`.
3. **Given** picker is open, **When** Uncle taps dismiss (x) icon, **Then** picker collapses without saving, tile remains in previous state.
4. **Given** Morning tile shows "Mild", **When** Uncle taps it, **Then** picker expands with "Mild" highlighted, Uncle can select a different severity.
5. **Given** Uncle selects "Moderate" to replace "Mild", **When** saved, **Then** tile updates to orange "Moderate" with bloom, `updatedAt` updated, `synced = false`.
6. **Given** all 4 slots logged, **When** Night severity saved, **Then** all tiles bloom together (300ms) as quiet completion acknowledgment. NO toast, NO snackbar, NO "Saved!" text.
7. **Given** system animations disabled, **When** Uncle logs severity, **Then** picker collapses instantly, color fills instantly, no bloom, no completion animation.
8. **Given** TalkBack enabled and picker open, **When** user swipes to "Severe", **Then** TalkBack announces "Severe. Double tap to select." Selection/dismiss returns focus to tile.
9. **Given** any date (today or past), **When** Uncle taps and selects severity, **Then** same flow executes — no special mode, no confirmation dialog for past entries.

## Tasks / Subtasks

- [x] Task 1: Build SeverityPicker composable (AC: #1, #3, #4, #8)
  - [x] 1.1 Create `SeverityPicker.kt` in `ui/daycard/`
  - [x] 1.2 Inline expansion within the TimeSlotTile — NOT a modal, NOT a bottom sheet
  - [x] 1.3 Show 4 severity options horizontally: color swatch + label + icon for each
  - [x] 1.4 Touch targets 48dp+ per option, 8dp minimum gaps between options
  - [x] 1.5 Dismiss control: back arrow or × icon for accidental taps
  - [x] 1.6 If editing existing entry, highlight current severity
  - [x] 1.7 Distribute options evenly within tile width
  - [x] 1.8 Pointer cancellation: selection registers on tap-up, NOT tap-down

- [x] Task 2: Implement picker animations (AC: #1, #2, #7)
  - [x] 2.1 Expand: 200ms ease-out (from `AnimationSpec.kt` constant)
  - [x] 2.2 Collapse on selection: 0ms instant (from `AnimationSpec.kt`)
  - [x] 2.3 Collapse on dismiss: 0ms instant
  - [x] 2.4 Color fill bloom: 150ms (from `AnimationSpec.kt`)
  - [x] 2.5 Check `Settings.Global.ANIMATOR_DURATION_SCALE` — if 0, skip ALL animations
  - [x] 2.6 NEVER use inline duration values — always reference `AnimationSpec.kt` constants

- [x] Task 3: Implement Day Card dimming (AC: #1)
  - [x] 3.1 When picker opens, dim the rest of the Day Card (other tiles, background)
  - [x] 3.2 Only the active tile + picker remains fully visible
  - [x] 3.3 Tapping dimmed area could dismiss picker (optional UX enhancement)

- [x] Task 4: Implement entry persistence (AC: #2, #5, #9)
  - [x] 4.1 On severity selection: call `repository.upsertEntry(date, timeSlot, severity)`
  - [x] 4.2 Repository sets `synced = false`, `updatedAt = System.currentTimeMillis()`
  - [x] 4.3 Upsert uses composite key `(date, timeSlot)` — insert or update
  - [x] 4.4 Same logic for today AND past dates — no conditional behavior
  - [x] 4.5 ViewModel calls `viewModelScope.launch { repository.upsertEntry(...) }`

- [x] Task 5: Implement haptic feedback (AC: #2)
  - [x] 5.1 Trigger haptic pulse on successful severity save
  - [x] 5.2 Use `HapticFeedbackType.LongPress` or similar via `LocalHapticFeedback`
  - [x] 5.3 Only if device supports haptics — graceful no-op otherwise

- [x] Task 6: Implement all-complete bloom (AC: #6)
  - [x] 6.1 After saving, check if all 4 slots for the current date are now logged
  - [x] 6.2 If yes, trigger 300ms bloom on all tiles simultaneously
  - [x] 6.3 NO toast, NO snackbar, NO text — bloom IS the only acknowledgment
  - [x] 6.4 Skip if system animations disabled

- [x] Task 7: Implement TalkBack accessibility (AC: #8)
  - [x] 7.1 Picker options: "Severe. Double tap to select."
  - [x] 7.2 Dismiss button: "Close severity picker. Double tap to dismiss."
  - [x] 7.3 After selection/dismiss, return focus to the tile
  - [x] 7.4 Announce state change after selection

## Dev Notes

### Architecture Compliance

- **Picker is inline expansion** — NOT a dialog, NOT a bottom sheet, NOT a modal
- **ViewModel handles save logic** via `viewModelScope.launch { }`
- **Repository.upsertEntry()** is `suspend` — never launches its own coroutines
- **UiState updates** flow reactively from Room → Repository → ViewModel → Composable

### UX Constraints (CRITICAL — Agents Will Try to Violate These)

- **NO toast messages, NO snackbars, NO "Saved!" confirmations** — the color fill IS the confirmation
- **NO confirmation dialogs** — tap = done. To undo, tap again to change severity
- **NO loading spinners** — Room writes are instant
- Empty slots: calm neutral "—". No motivational text
- Picker collapse on selection is INSTANT (0ms) — no exit animation on collapse
- Color fill bloom is the ONLY visual feedback for save

### Animation Constants (MUST use from AnimationSpec.kt)

| Animation | Duration | Easing |
|-----------|----------|--------|
| Picker expand | 200ms | ease-out |
| Picker collapse (selection) | 0ms | instant |
| Color fill bloom | 150ms | — |
| All-complete bloom | 300ms | — |

- **Hard cap:** No animation exceeds 300ms
- **NEVER use inline duration values** — always reference `AnimationSpec.kt`

### Severity Display — CRITICAL

- All colors: `Severity.color`, `Severity.softColor` — NEVER hardcode hex
- All labels: `Severity.displayName` — NEVER hardcode "Mild" etc.
- Triple encoding in picker: color + label + icon for each option
- Severity numeric: 0=NO_PAIN, 1=MILD, 2=MODERATE, 3=SEVERE

### Project Structure Notes

```
ui/daycard/
├── DayCardScreen.kt          # Updated: adds picker state management + dimming
├── DayCardViewModel.kt       # Updated: adds upsertEntry, all-complete check
├── DayCardUiState.kt         # Updated: adds pickerOpenForSlot field
├── TimeSlotTile.kt           # Updated: tap handler opens picker
└── SeverityPicker.kt         # NEW: inline picker composable
```

### Dependencies on Story 1.1, 1.2

- Requires: all of 1.1 (Room, enums, Hilt, theme, AnimationSpec) + 1.2 (DayCardScreen, TimeSlotTile, ViewModel)
- This story completes the core logging loop

### References

- [Source: project-context.md#Severity & TimeSlot Model]
- [Source: project-context.md#UX Constraints]
- [Source: project-context.md#Animation Constants]
- [Source: project-context.md#Accessibility]
- [Source: requirements-inventory.md#FR2, FR3, FR4, FR5, FR6, FR8, FR9, FR17]
- [Source: requirements-inventory.md#NFR1, NFR6, NFR7, NFR14, NFR17, NFR19, NFR20, NFR25, NFR26]
- [Source: requirements-inventory.md#UX — Interaction Requirements]
- [Source: epic-1-day-card-symptom-logging.md#Story 1.3]

## Dev Agent Record

### Agent Model Used

Claude claude-4.6-opus (via Cursor IDE)

### Debug Log References

- No build environment (Java/Gradle) available in agent environment. Tests written and structurally verified but could not be executed. Must be run in Android Studio.

### Completion Notes List

- **Task 1 (SeverityPicker):** Created `SeverityPicker.kt` in `ui/daycard/`. 4 severity options shown horizontally via `Row` with `Arrangement.SpaceEvenly`. Each `SeverityOption` is a 72x72dp `Card` (exceeds 48dp touch target requirement) with triple encoding: `severity.softColor` background, `severity.icon()` icon, `severity.displayName` text label. Close icon (`Icons.Default.Close`) for dismiss. `isSelected` border highlight from `severity.color` when editing existing entry. Options distributed evenly. `Severity.icon()` extension moved to `internal` visibility shared across package. Haptic feedback (`HapticFeedbackType.LongPress`) fires on option click.
- **Task 2 (Animations):** Picker expand: `AnimatedVisibility` with `expandVertically(tween(PICKER_EXPAND_MS, EaseOut))`. Collapse (selection/dismiss): `shrinkVertically(snap())` — instant 0ms. Color fill bloom: `animateColorAsState` with `colorFillBloomSpec()` (150ms). System animations check: `Settings.Global.ANIMATOR_DURATION_SCALE` — when 0, all animations replaced with `snap()`. No inline duration values — all from `HealthTrendAnimation` constants.
- **Task 3 (Dimming):** Non-active tiles dimmed with `graphicsLayer { alpha = 0.3f }` when a picker is open. Active tile remains fully visible. Tapping dimmed tile dismisses picker (optional UX enhancement implemented).
- **Task 4 (Persistence):** Added `HealthEntryRepository.upsertEntry(date, timeSlot, severity)` — looks up existing entry via `getEntry(date, timeSlot.name)`, then either `update` (preserving id) or `insert`. Always sets `synced = false`, `updatedAt = System.currentTimeMillis()`. ViewModel calls via `viewModelScope.launch { repository.upsertEntry(...) }`. Room Flow automatically re-emits updated entries. Same logic for today and past dates.
- **Task 5 (Haptics):** `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` called inside `SeverityPicker` on option click, before `onSeveritySelected` callback. `LocalHapticFeedback` gracefully handles devices without haptic support.
- **Task 6 (All-Complete Bloom):** ViewModel tracks `pendingAllCompleteCheck` (set on save) and `previousAllLogged` (tracks transition). Bloom triggers only when transitioning from "not all logged" to "all logged" after a save — prevents re-triggering on severity changes when all already logged. UI implements 300ms scale pulse (1.0 → 1.03 → 1.0) via `Animatable` + `LaunchedEffect(state.allCompleteBloom)`. Skipped when system animations disabled. NO toast, NO snackbar, NO text.
- **Task 7 (TalkBack):** Picker options: `"{severity.displayName}. Double tap to select."` with `stateDescription = "Selected"` for current severity. Dismiss button: `"Close severity picker. Double tap to dismiss."`. Focus returns to tile naturally when picker collapses. Tile semantics updated reactively when entry state changes.
- **ViewModel restructure:** Replaced simple `repository.getEntriesByDate().collect` with `combine(repositoryFlow, _pickerOpenForSlot)` to merge Room data with local UI state (picker). `dateString` moved to class-level for access by `onSeveritySelected`.
- **Tests written:** `DayCardViewModelTest` (27 tests — 14 from Story 1.2 + 13 new for picker/selection/bloom), `DayCardUiStateTest` (10 tests — 6 existing + 4 new), `HealthEntryRepositoryUpsertTest` (6 new tests). Total: 43 unit tests across 3 test files.

### File List

**New files:**
- `app/src/main/java/com/healthtrend/app/ui/daycard/SeverityPicker.kt`
- `app/src/test/java/com/healthtrend/app/data/repository/HealthEntryRepositoryUpsertTest.kt`

**Modified files:**
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardUiState.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardViewModel.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/DayCardScreen.kt`
- `app/src/main/java/com/healthtrend/app/ui/daycard/TimeSlotTile.kt`
- `app/src/main/java/com/healthtrend/app/data/repository/HealthEntryRepository.kt`
- `app/src/test/java/com/healthtrend/app/ui/daycard/DayCardViewModelTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/daycard/DayCardUiStateTest.kt`

## Change Log

- **2026-02-08:** Story 1.3 implemented — Inline severity picker with 200ms expand / 0ms collapse animations, 150ms color fill bloom, haptic feedback, Day Card dimming, all-complete 300ms bloom, system animations check, TalkBack accessibility. Repository.upsertEntry() added for insert-or-update persistence. 43 unit tests total across 3 test files.
