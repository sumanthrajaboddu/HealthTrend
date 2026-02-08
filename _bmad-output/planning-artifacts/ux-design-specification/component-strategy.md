# Component Strategy

## Design System Components (Material 3 — Used As-Is)

| Component | Material 3 Name | Usage in HealthTrend |
|-----------|----------------|---------------------|
| Bottom Navigation | `NavigationBar` + `NavigationBarItem` | 3 tabs: Today, Analytics, Settings |
| Top App Bar | `TopAppBar` (Small) | Date header on Day Card, screen titles |
| Screen Layout | `Scaffold` | Screen-level structure with top bar + bottom nav |
| Date Range Chips | `FilterChip` | Analytics: "1 Week" / "1 Month" / "3 Months" |
| Settings Toggles | `Switch` | Reminder on/off per slot, global toggle |
| Time Picker | `TimePickerDialog` | Reminder time configuration |
| Text Input | `OutlinedTextField` | Google Sheet URL, patient name |
| Action Button | `FilledButton` | "Export PDF", "Share" |
| Google Sign-In | Google Sign-In SDK button | One-time OAuth authentication |
| Dividers | `HorizontalDivider` | Settings screen section separators |
| Loading Indicator | `CircularProgressIndicator` | PDF generation only |

## Custom Components

### 1. Week Strip (`WeekStripBar`)

**Purpose:** Provide week-level context and quick day navigation at the top of the Day Card screen.

**Anatomy:**
```
┌─────────────────────────────────────────────────────┐
│  ◄   Mon  Tue  Wed  [Thu]  Fri  Sat  Sun   ►      │
│        •    •    •                                   │
└─────────────────────────────────────────────────────┘
```
- 7 day cells in a horizontal row
- Optional left/right arrows for previous/next week
- Each cell shows: abbreviated day name + date number
- Green dot below days that have logged data

**States:**

| State | Appearance |
|-------|-----------|
| Normal day | Gray text, transparent background |
| Today | Purple background (`#6750A4`), white text |
| Selected day (not today) | Purple outline, purple text |
| Day with data | Small green dot (`#4CAF50`) below the date |
| Day with no data | No dot |
| Future day | Lighter gray text, not tappable |

**Interaction:** Tap a day cell → Day Card navigates to that date. Swipe week strip → navigate to previous/next week.

**Accessibility:** "Thursday, February 6, today, has data. Tap to view." Focus order: left arrow → day cells left to right → right arrow.

---

### 2. Time Slot Tile (`TimeSlotTile`)

**Purpose:** The core interactive component — displays one time slot's state and handles tap-to-log interaction.

**Anatomy:**
```
┌──────────────────────────────────────────────┐
│  [☀]   Morning                  [No Pain] 🟢 │
│   ↑        ↑                        ↑        │
│  icon    label                 severity pill  │
└──────────────────────────────────────────────┘
```
- Left: Static time-of-day icon (never changes)
- Center: Time slot label ("Morning", "Afternoon", "Evening", "Night")
- Right: Severity pill (when filled) or status text (when empty)

**States:**

| State | Background | Left Icon | Right Content | Border |
|-------|-----------|-----------|--------------|--------|
| Empty | `#EFEDF1` (Surface Container) | Time icon in `#79747E` | "—" in `#79747E` | None |
| Current (empty) | `#E8DEF8` (Tertiary Container) | Time icon in `#6750A4` | "Tap to log" in `#6750A4` | 1.5dp `#6750A4` |
| Filled — No Pain | `#E8F5E9` | Time icon in `#2E7D32` | "No Pain" green pill | None |
| Filled — Mild | `#FFF8E1` | Time icon in `#F57F17` | "Mild" amber pill | None |
| Filled — Moderate | `#FFF3E0` | Time icon in `#E65100` | "Moderate" orange pill | None |
| Filled — Severe | `#FFEBEE` | Time icon in `#C62828` | "Severe" red pill | None |
| Picker Expanded | `#F7F2F7` | Time icon unchanged | Inline severity picker | 1.5dp `#6750A4` |

**Size:** Full width, 64dp minimum height, 24dp padding left/right, 12dp gap between tiles.

**Interaction:** Tap → expands inline severity picker (or re-opens it with current selection if already filled).

**Accessibility:** "Morning, not logged. Tap to log severity." or "Afternoon, currently Mild. Tap to change severity."

---

### 3. Inline Severity Picker (`SeverityPicker`)

**Purpose:** The four-option severity selector that expands inline within a Time Slot Tile.

**Anatomy:**
```
┌──────────────────────────────────────────────────┐
│  ☼  Afternoon — Select severity                   │
│  ┌──────────────────────────────────────────┐     │
│  │  🟢      🟡       🟠        🔴     [✕]  │     │
│  │ No Pain  Mild  Moderate  Severe          │     │
│  └──────────────────────────────────────────┘     │
└──────────────────────────────────────────────────┘
```
- Four color circles in a horizontal row, each with a text label below
- Dismiss button (✕) at the right edge
- When editing, the current selection has a visible highlight ring

**States per option:**

| State | Appearance |
|-------|-----------|
| Unselected | Color circle + label, standard size |
| Selected (editing) | Color circle with dark outline ring (2.5dp `#1C1B1F`), bold label |
| Tapped | Brief scale-up animation (105%, 100ms) then collapses |

**Interaction:**
- Tap a color → picker collapses instantly, slot fills, haptic pulse
- Tap dismiss (✕) → picker collapses, no change
- Tap outside picker → same as dismiss

**Animation:** Expands with `AnimatedVisibility` (200ms ease-out). Collapses instantly on selection (no delay). Color fill bloom on the slot: 150ms.

**Accessibility:** "Severity picker open for Afternoon. No Pain, Mild, Moderate currently selected, Severe. Dismiss picker." Each option is individually focusable.

---

### 4. Severity Pill Badge (`SeverityPill`)

**Purpose:** Compact label showing the severity level within a filled Time Slot Tile.

**Anatomy:** `[ No Pain ]` — rounded pill shape with severity color background and white or dark text.

**Variants:**

| Severity | Background | Text Color | Text |
|----------|-----------|------------|------|
| No Pain | `#4CAF50` | `#FFFFFF` | "No Pain" |
| Mild | `#FFC107` | `#FFFFFF` | "Mild" |
| Moderate | `#FF9800` | `#FFFFFF` | "Moderate" |
| Severe | `#F44336` | `#FFFFFF` | "Severe" |

**Size:** Auto-width based on text, 28dp height, 12dp horizontal padding, 14dp rounded corners. Label Large typography (14sp, Medium weight).

---

### 5. Trend Line Chart (`TrendChart`)

**Purpose:** Visualize severity trends over a selectable date range in the Analytics screen.

**Anatomy:**
- X-axis: dates (formatted based on range: daily for 1 week, weekly for 1-3 months)
- Y-axis: severity scale (0 = No Pain, 1 = Mild, 2 = Moderate, 3 = Severe)
- Data points: colored by severity value at that point
- Lines connect data points across time
- Up to 4 data points per day (one per time slot)

**Visual Treatment:**
- Chart background: transparent on Surface
- Grid lines: `#CAC4D0` at 0.5dp
- Data points: 8dp diameter circles in severity color
- Connecting line: 2dp, color transitions between severity colors
- Y-axis labels: severity names in On Surface Variant color
- X-axis labels: date abbreviations in On Surface Variant color

**Interaction:** Non-interactive in MVP. Tap-to-inspect individual data points can be added later.

**Accessibility:** "Trend chart showing severity over the last 7 days. Average severity: Mild. Trend: improving." Provide summary description, not individual data points.

---

### 6. Time-of-Day Breakdown (`SlotAverageCards`)

**Purpose:** Show average severity per time slot across the selected date range. Answers "which part of the day is worst?"

**Anatomy:**
```
┌─────────────────────────────┐
│  ☀ Morning      Avg: Mild  │  ← soft amber background
├─────────────────────────────┤
│  ☼ Afternoon    Avg: Mild  │  ← soft amber background
├─────────────────────────────┤
│  🌙 Evening   Avg: Moderate │  ← soft orange background
├─────────────────────────────┤
│  ★ Night       Avg: No Pain │  ← soft green background
└─────────────────────────────┘
```
- Four rows, one per time slot, using the same static time icons
- Each shows the average severity with color tinting
- Sorted by time of day (not by severity)

---

### 7. PDF Report Layout (`HealthTrendReport`)

**Purpose:** Generate a printable/shareable PDF summarizing severity data for a doctor visit.

**Layout (Top to Bottom):**
1. **Header block:** Patient name (from Settings), "HealthTrend Report", date range
2. **Trend chart:** Same visualization as the in-app trend chart, optimized for print (larger labels, higher contrast)
3. **Time-of-day summary:** Four-row table showing average severity per slot
4. **Daily log table:** Date rows × 4 columns (Morning, Afternoon, Evening, Night), cells color-coded by severity
5. **Footer:** "Generated by HealthTrend on [date]"

**Print considerations:** Severity colors adjusted for CMYK print clarity. White background. No transparency. Font size minimum 10pt for readability.

## Settings Screen Component Layout

**Architecture Decision: Google Sheets authentication uses OAuth (Option B)**

Uncle (or Raja during setup) signs into a Google account once. The app uses OAuth tokens to access Google Sheets directly. No service account required.

**Settings Screen Layout:**

1. **Google Account Section**
   - "Google Account" header
   - If not signed in: `[Sign in with Google]` button (standard Google Sign-In SDK)
   - If signed in: Display account email + `[Sign Out]` text button
   - Google Sheet URL: `OutlinedTextField` — enter Sheet URL or ID

2. **Patient Info Section**
   - "Patient" header
   - Patient name: `OutlinedTextField` — used in PDF report header

3. **Reminders Section**
   - "Reminders" header
   - Global toggle: `Switch` — enable/disable all reminders
   - Per-slot rows (only visible when global is on):
     - Morning: `Switch` + time display (tap to edit via `TimePickerDialog`)
     - Afternoon: `Switch` + time display
     - Evening: `Switch` + time display
     - Night: `Switch` + time display
   - Default times pre-filled: 8:00 AM, 1:00 PM, 6:00 PM, 10:00 PM

4. **About Section**
   - App version
   - "Share Google Sheet" — opens Android share sheet with the Sheet URL

## Component Implementation Strategy

**Build with Material 3 tokens:** All custom components use Material 3 color tokens, typography scale, and spacing tokens. No hardcoded values — everything references the design system for consistency.

**Composable architecture:** Each custom component is a self-contained Jetpack Compose composable with:
- Clear parameter interface (data in, events out)
- Preview support for all states
- Accessibility annotations built in from day one

**State management:** Time Slot Tile state (empty → picker open → filled) managed via a single sealed class with all possible states. Animation transitions driven by state changes.

## Implementation Roadmap

**Phase 1 — Core (MVP Critical):**
1. `TimeSlotTile` — The entire app depends on this component
2. `SeverityPicker` — The core interaction
3. `SeverityPill` — Visual feedback for filled slots
4. `WeekStripBar` — Primary navigation aid
5. Settings screen with Google Sign-In + Sheet URL + Patient name

**Phase 2 — Analytics:**
6. `TrendChart` — Analytics screen main visualization
7. `SlotAverageCards` — Time-of-day breakdown

**Phase 3 — Export:**
8. `HealthTrendReport` — PDF generation and layout

This roadmap means the app is fully functional for daily logging (Phase 1) before analytics and export are built. Uncle can start using it immediately.
