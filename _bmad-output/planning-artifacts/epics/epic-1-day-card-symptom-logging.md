# Epic 1: Day Card Symptom Logging

Uncle can open the app, see today's Day Card with four time slots, tap a slot, pick a severity, and it's saved locally. The core loop — the entire reason this app exists.

## Story 1.1: Project Setup, Core Data Layer & Theme

**As a** developer,
**I want** the HealthTrend Android project initialized with the correct framework, database, dependency injection, domain models, and visual theme,
**So that** I have a working, launchable app foundation ready for UI development.

**Acceptance Criteria:**

**Given** the project is opened in Android Studio
**When** the developer builds and runs the app
**Then** it compiles successfully, launches on a device/emulator, and displays an empty Compose screen
**And** the project uses AGP 9.0, Compose BOM 2025.12.00, Kotlin DSL, Min SDK 26, Target SDK 35, package com.healthtrend.app

**Given** the Room database is configured
**When** a HealthEntry is inserted via DAO
**Then** the entry is persisted with correct composite unique constraint on (date, timeSlot)
**And** HealthEntryDao provides Flow queries for observable data and suspend functions for one-shot operations

**Given** the Severity and TimeSlot enums are defined
**When** accessing any enum value
**Then** it provides displayName, color, softColor, numericValue (Severity) or displayName, icon, defaultReminderTime (TimeSlot)
**And** no hardcoded severity colors, labels, or time slot names exist anywhere else in the codebase

**Given** the theme is configured
**When** the app launches
**Then** Material 3 theme applies with dynamic color disabled, fixed severity color palette, Roboto system font, and all animation constants defined in AnimationSpec.kt

**Given** Hilt dependency injection is configured
**When** the app builds
**Then** DatabaseModule provides Room DB and DAOs, RepositoryModule provides HealthEntryRepository, and @HiltAndroidApp is on the Application class

## Story 1.2: Day Card Screen & Navigation Shell

**As** Uncle (the patient),
**I want** to open the app and see today's Day Card with four time slots showing their current state,
**So that** I know at a glance what I've logged and what's still empty.

**Acceptance Criteria:**

**Given** the app is launched for the first time
**When** the Day Card screen renders
**Then** today's date is displayed in the top app bar, and four time slot tiles (Morning, Afternoon, Evening, Night) are shown vertically with empty dash state
**And** tiles are 64dp+ height, 12dp gaps, 16dp horizontal margins, full-width single-column layout

**Given** it is currently 2:30 PM
**When** the Day Card renders
**Then** the Afternoon tile has a subtle highlight distinguishing it from the other slots

**Given** the user has previously logged "Mild" for Morning
**When** the Day Card loads today's entries from Room
**Then** the Morning tile displays the amber "Mild" color, "Mild" text label, and the mild icon

**Given** the bottom navigation bar is visible
**When** the user taps the "Today" tab
**Then** the Day Card for today is displayed (or remains displayed if already showing today)
**And** three navigation tabs are visible (Today, Analytics, Settings) with Analytics and Settings showing placeholder screens

**Given** TalkBack is enabled
**When** the user navigates to an empty Morning tile
**Then** TalkBack announces "Morning, not logged. Double tap to log severity."

**Given** the system font is set to 1.5x
**When** the Day Card renders
**Then** all text scales correctly using sp units and tiles grow in height to accommodate larger text without clipping

## Story 1.3: Inline Severity Picker & Entry Logging

**As** Uncle (the patient),
**I want** to tap a time slot and select a severity level from an inline picker so that my symptom entry is saved instantly with clear visual feedback.

**Acceptance Criteria:**

**Given** the Day Card is showing today with an empty Morning slot
**When** Uncle taps the Morning tile
**Then** the severity picker expands inline within the tile (200ms ease-out), showing 4 severity options with color + label + icon, and the rest of the Day Card dims
**And** touch targets are 48dp+ for picker options with 8dp minimum gaps

**Given** the severity picker is open for an empty slot
**When** Uncle taps "Mild"
**Then** the picker collapses instantly (0ms), the tile fills with amber "Mild" color (150ms bloom), a haptic pulse fires, and the entry is persisted to Room with synced = false and updatedAt = current timestamp

**Given** the severity picker is open
**When** Uncle taps the dismiss (×) icon
**Then** the picker collapses without saving, the tile remains in its previous state

**Given** the Morning tile already shows "Mild"
**When** Uncle taps the Morning tile
**Then** the picker expands with "Mild" visually highlighted, and Uncle can select a different severity

**Given** Uncle selects "Moderate" to replace "Mild"
**When** the entry is saved
**Then** the tile updates to orange "Moderate" with bloom animation, updatedAt is set to the current timestamp, and synced is set to false

**Given** Morning, Afternoon, and Evening are logged, Night is empty
**When** Uncle logs Night's severity
**Then** all four tiles briefly bloom together (300ms) as a quiet completion acknowledgment
**And** no toast, no snackbar, no "Saved!" text appears — the color fill is the confirmation

**Given** system animations are disabled via accessibility settings
**When** Uncle logs a severity
**Then** the picker collapses instantly, the color fills instantly (no bloom), and no completion animation plays

**Given** TalkBack is enabled and the picker is open
**When** the user swipes to the "Severe" option
**Then** TalkBack announces "Severe. Double tap to select."
**And** selecting an option or dismissing returns focus to the tile

**Given** Uncle is viewing any date's Day Card (today or past)
**When** Uncle taps a slot and selects a severity
**Then** the exact same picker flow executes regardless of the date — no special mode, no confirmation dialog for past entries

---
