# Story 5.2: Time-of-Day Breakdown Cards

Status: review

## Story

As Uncle (the patient),
I want to see average severity broken down by time of day across the selected date range,
So that I can tell my doctor which part of the day is consistently worse.

## Acceptance Criteria

1. **Given** Analytics screen with 1 Week selected, **When** time-of-day breakdown renders, **Then** four cards below trend chart — Morning, Afternoon, Evening, Night — each showing average severity for that slot over past 7 days.
2. **Given** Morning entries past week: Moderate, Moderate, Severe, Moderate, Mild, Moderate, Moderate, **When** Morning card renders, **Then** shows "Moderate" (rounded average), tinted with Moderate soft color, with Moderate icon.
3. **Given** Uncle switches to 3 Months, **When** breakdown cards update, **Then** averages recalculate for full 3-month range.
4. **Given** no Evening entries in selected range, **When** Evening card renders, **Then** neutral empty state (dash or "No data") — no alarm, no encouragement.
5. **Given** TalkBack enabled, **When** user navigates to Afternoon card, **Then** announces "Afternoon average: Mild over 7 days."

## Tasks / Subtasks

- [x] Task 1: Calculate time-of-day averages in ViewModel (AC: #1, #2, #3)
  - [x] 1.1 Add to `AnalyticsViewModel`: compute average severity per TimeSlot for selected date range
  - [x] 1.2 Average calculation: sum `Severity.numericValue` for slot across days, divide by count, round to nearest Severity
  - [x] 1.3 Rounding: 0–0.49 = No Pain, 0.5–1.49 = Mild, 1.5–2.49 = Moderate, 2.5–3.0 = Severe
  - [x] 1.4 Update `AnalyticsUiState` to include `slotAverages: Map<TimeSlot, Severity?>`
  - [x] 1.5 Null for slots with no data in the range
  - [x] 1.6 Reactively recalculate when date range changes

- [x] Task 2: Build TimeOfDayBreakdownCard composable (AC: #1, #2, #4)
  - [x] 2.1 Create `TimeOfDayBreakdownCard.kt` in `ui/analytics/`
  - [x] 2.2 Card for each TimeSlot showing: slot name, average severity label, severity icon
  - [x] 2.3 Background tinted with `Severity.softColor` of the average
  - [x] 2.4 Slot name from `TimeSlot.displayName` — NEVER hardcode
  - [x] 2.5 Average label from `Severity.displayName` — NEVER hardcode
  - [x] 2.6 Empty state: "—" or "No data" with neutral styling — no alarm, no encouragement

- [x] Task 3: Layout 4 breakdown cards (AC: #1)
  - [x] 3.1 Place below trend chart on Analytics screen
  - [x] 3.2 Layout: 2x2 grid or 4 horizontal cards — fits within 16dp margins
  - [x] 3.3 Cards sized consistently, responsive to screen width
  - [x] 3.4 Triple encoding on each card: severity color + text label + icon

- [x] Task 4: TalkBack accessibility (AC: #5)
  - [x] 4.1 Each card: "[Slot] average: [Severity] over [period]."
  - [x] 4.2 Empty card: "[Slot]: No data for this period."
  - [x] 4.3 48dp minimum touch targets (cards should be large enough)
  - [x] 4.4 Semantics describe the data, not the visual appearance

## Dev Notes

### Architecture Compliance

- **AnalyticsViewModel** manages all analytics state — no separate ViewModel for breakdown
- **Data flows from Room → Repository → ViewModel → Composable**
- **StateFlow + collectAsStateWithLifecycle()**
- **Breakdown composables live in `ui/analytics/`** — screen-specific

### Severity Averaging Logic

```
For each TimeSlot in selected range:
  1. Collect all entries for that slot across all days
  2. Sum severity.numericValue
  3. Divide by count
  4. Round to nearest Severity:
     - 0.00–0.49 → NO_PAIN
     - 0.50–1.49 → MILD
     - 1.50–2.49 → MODERATE
     - 2.50–3.00 → SEVERE
  5. If no entries, return null (empty state)
```

### Display Rules — CRITICAL

- All colors: `Severity.softColor` for card background — NEVER hardcode hex
- All labels: `Severity.displayName` — NEVER hardcode "Moderate" etc.
- All slot names: `TimeSlot.displayName` — NEVER hardcode "Morning" etc.
- Triple encoding: color + text + icon on every card
- Empty state: calm neutral "—" — NO alarm, NO encouragement, NO motivational text

### UX Constraints

- NO loading spinners
- Empty states are neutral — no illustrations
- Same color system as Day Card
- Responsive layout within 16dp margins

### Project Structure Notes

```
ui/analytics/
├── AnalyticsScreen.kt              # Updated: adds breakdown section
├── AnalyticsViewModel.kt           # Updated: slot average calculations
├── AnalyticsUiState.kt             # Updated: slotAverages field
├── TrendChart.kt                   # From Story 5.1
├── DateRangeSelector.kt            # From Story 5.1
└── TimeOfDayBreakdownCard.kt       # NEW: individual breakdown card
```

### Dependencies on Stories 1.1, 5.1

- Requires: Severity/TimeSlot enums (1.1), AnalyticsScreen + ViewModel + date range (5.1)
- This story extends the Analytics screen with breakdown cards

### References

- [Source: project-context.md#Severity & TimeSlot Model]
- [Source: project-context.md#UX Constraints]
- [Source: requirements-inventory.md#FR24, FR25]
- [Source: requirements-inventory.md#NFR4, NFR13, NFR14, NFR16, NFR17]
- [Source: epic-5-analytics-trends.md#Story 5.2]

## Dev Agent Record

### Agent Model Used
claude-4.6-opus (Cursor IDE)

### Debug Log References
- No gradle wrapper available in dev environment; tests written but need verification from Android Studio

### Completion Notes List
- **Task 1:** Added `calculateSlotAverages()` to `AnalyticsViewModel`. Groups entries by `TimeSlot`, computes mean of `severity.numericValue`, rounds via existing `numericToSeverity()` (same rounding as Story 5.1). Returns `Map<TimeSlot, Severity?>` — null for slots with zero entries. Added `slotAverages` field to `AnalyticsUiState.Success` (default = emptyMap for backward compat). Reactively recalculates via `flatMapLatest` on `selectedRange`. 7 new tests in `AnalyticsViewModelTest` + 2 new tests in `AnalyticsUiStateTest`.
- **Task 2:** Created `TimeOfDayBreakdownCard.kt` — Material 3 Card with `Severity.softColor` background, `TimeSlot.icon` + `TimeSlot.displayName` label + `Severity.displayName` text + severity-tinted icon (triple encoding). Empty state shows "—" with neutral `onSurfaceVariant` styling. No hardcoded colors/labels.
- **Task 3:** Created `TimeOfDayBreakdownSection` — 2x2 grid layout (two `Row`s with `weight(1f)` per card). 8dp spacing, 16dp horizontal padding. Placed below trend chart in `AnalyticsContent` with 24dp spacer.
- **Task 4:** Card-level `semantics(mergeDescendants = true)` with `contentDescription`: "Morning average: Mild over 1 Week." for data cards, "Morning: No data for this period." for empty cards. Inner content uses `clearAndSetSemantics` to avoid TalkBack reading duplicate labels. Cards are large enough (12dp padding + content) for 48dp touch targets.

### File List
- `app/src/main/java/com/healthtrend/app/ui/analytics/AnalyticsUiState.kt` — MODIFIED (added `slotAverages` field to Success, added TimeSlot import)
- `app/src/main/java/com/healthtrend/app/ui/analytics/AnalyticsViewModel.kt` — MODIFIED (added `calculateSlotAverages()`, TimeSlot import, included slotAverages in Success)
- `app/src/main/java/com/healthtrend/app/ui/analytics/AnalyticsScreen.kt` — MODIFIED (added TimeOfDayBreakdownSection below chart in AnalyticsContent)
- `app/src/main/java/com/healthtrend/app/ui/analytics/TimeOfDayBreakdownCard.kt` — NEW (card composable + 2x2 section layout)
- `app/src/test/java/com/healthtrend/app/ui/analytics/AnalyticsViewModelTest.kt` — MODIFIED (7 new slot average tests)
- `app/src/test/java/com/healthtrend/app/ui/analytics/AnalyticsUiStateTest.kt` — MODIFIED (2 new slotAverages tests)
