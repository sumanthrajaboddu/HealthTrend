# Requirements Inventory

## Functional Requirements

FR1: User can view today's Day Card showing four time slots (Morning, Afternoon, Evening, Night) with their current state
FR2: User can tap any time slot to open a severity selector with four options (No Pain, Mild, Moderate, Severe)
FR3: User can select a severity level to record an entry for that time slot
FR4: User can dismiss the severity selector without making a selection
FR5: User can change a previously recorded severity by tapping the filled slot and selecting a different level
FR6: User can log entries retroactively for any past time slot using the same flow as current logging
FR7: System auto-highlights the current time-of-day slot based on device time
FR8: System provides immediate visual confirmation when an entry is saved
FR9: System provides a brief visual acknowledgment (≤300ms animation) when all four slots for a day are completed
FR10: User can swipe horizontally on the Day Card to browse previous and future days
FR11: User can view a week strip showing the current week with day-level navigation
FR12: User can tap any day in the week strip to navigate directly to that day's card
FR13: System displays a data indicator on week strip days that have logged entries
FR14: User can navigate to previous/next weeks via the week strip
FR15: System always displays the current date context so the user knows which day they're viewing
FR16: User can return to today's Day Card via the bottom navigation "Today" tab
FR17: System persists all entries locally on the device immediately upon selection
FR18: System syncs entries to a configured Google Sheet in the background without user action
FR19: System queues entries when offline and syncs automatically when connectivity is restored
FR20: System writes entries to Google Sheets in the format: Date | Morning | Afternoon | Evening | Night
FR21: System operates identically whether online or offline with no user-visible difference
FR22: System silently handles sync failures with automatic retry without notifying the user
FR23: User can view a severity trend chart over a selectable date range (1 week, 1 month, 3 months)
FR24: User can view a time-of-day breakdown showing average severity per slot across the selected range
FR25: System displays analytics using the same severity color system as the Day Card
FR26: System defaults the analytics view to the most recent 1-week period
FR27: User can generate a PDF report from the analytics screen for a selected date range
FR28: System includes in the PDF: patient name, date range, trend chart, time-of-day summary, and daily log table
FR29: User can preview the generated PDF before sharing
FR30: User can share the PDF via the Android share sheet (WhatsApp, email, print, etc.)
FR31: System sends scheduled notification reminders at configurable times for each time slot
FR32: User can enable or disable reminders globally
FR33: User can enable or disable reminders independently per time slot
FR34: User can configure the reminder time for each time slot
FR35: System provides default reminder times: 8 AM, 1 PM, 6 PM, 10 PM
FR36: Tapping a notification opens the app to today's Day Card
FR37: System re-registers reminders after device restart
FR38: System sends one notification per slot with no follow-up, batching, or escalation
FR39: User can sign in with a Google account for Google Sheets access
FR40: User can configure the target Google Sheet URL or ID
FR41: User can enter a patient name (used in PDF report headers)
FR42: System auto-saves all settings immediately without requiring a save action
FR43: User can sign out of their Google account
FR44: User can share the Google Sheet link via the Android share sheet

## NonFunctional Requirements

NFR1: Entry logging completes in under 100ms perceived latency (local save is instant)
NFR2: App launches to Day Card in under 1 second from local data
NFR3: Day Card swipe navigation completes in under 250ms
NFR4: Analytics charts render from local data in under 500ms
NFR5: PDF generation completes in under 3 seconds
NFR6: All animations complete within 300ms maximum
NFR7: App maintains 60fps on mid-range Android devices (Samsung Galaxy A54 class)
NFR8: OAuth tokens stored securely using platform-appropriate secure storage mechanisms
NFR9: No health data transmitted to any service other than the user's configured Google Sheet
NFR10: No third-party analytics, tracking, or crash reporting SDKs
NFR11: App requests only minimum permissions required for functionality
NFR12: Google Sheet access scoped to minimum required API permissions
NFR13: All interactive elements meet WCAG 2.1 Level AA compliance
NFR14: All touch targets meet 48dp × 48dp minimum (Day Card slots: 64dp+)
NFR15: All text contrast ratios meet WCAG AA minimum (4.5:1 normal text, 3:1 large text)
NFR16: Every severity level distinguishable without color alone (icon + text label + color)
NFR17: Full TalkBack screen reader support with descriptive content labels conveying element purpose, state, and available actions
NFR18: All text uses sp units to support dynamic font scaling up to 1.5x
NFR19: App respects Android "Remove animations" accessibility setting
NFR20: All actions achievable via single tap (swipe has tap alternatives)
NFR21: Google Sheets API usage stays within quota limits under normal operation
NFR22: OAuth token refresh handled silently without user intervention
NFR23: Google Sheets sync tolerates up to 30 days offline with full queue recovery
NFR24: Sync operations are idempotent — duplicate syncs produce correct data
NFR25: Zero data loss — entries survive app crashes, force stops, and device restarts
NFR26: Data writes are atomic — no partial data corruption on app crash, force stop, or interruption
NFR27: App is fully functional without network connectivity for unlimited duration
NFR28: Reminder notifications persist across device restarts
NFR29: App handles Google API unavailability with no user-visible impact

## Additional Requirements

**From Architecture — Starter Template & Project Setup:**
- Android Studio Empty Compose Activity as starter template (package: com.healthtrend.app, Min SDK 26, Kotlin DSL)
- AGP 9.0.0 with built-in Kotlin support — do NOT apply org.jetbrains.kotlin.android plugin
- Compose BOM 2025.12.00 for all Compose library version management
- Room 2.8.4 with KSP (not kapt) for local database
- Hilt for dependency injection (@HiltAndroidApp, @HiltViewModel, hiltViewModel())
- Vico 2.x stable (vico-compose-m3 module) for charting
- WorkManager 2.11.1 for background sync (CoroutineWorker)
- Credential Manager 1.5.0 for Google Sign-In (replaces legacy Google Sign-In SDK)
- Google Sheets API v4 for data sync
- Version catalog in gradle/libs.versions.toml for all dependency versions
- R8 code shrinking enabled for release APK
- ProGuard rules for Google API client, Room, and Hilt
- Single build variant (release), no staging/production split
- google-services.json for Google API client configuration

**From Architecture — Data Architecture:**
- HealthEntry entity with composite unique constraint on (date, timeSlot)
- AppSettings single-row table for all app configuration
- Two-way timestamp-based sync (upgraded from PRD's one-way push) for multi-device safety
- Cell-level Google Sheet writes — never overwrite entire rows
- Timestamp comparison before every write — never overwrite newer data with older
- Push phase: only push entries where synced = false, compare timestamps before write
- Pull phase: read all Sheet rows, update local only if Sheet timestamp > local updatedAt
- Sync triggers: immediate (WorkManager one-time after save), periodic (hourly fallback), on app launch
- ExistingWorkPolicy.KEEP to prevent duplicate immediate syncs
- WorkManager BackoffPolicy.EXPONENTIAL with 30-second initial delay for retries
- Date format in Sheet Column A: YYYY-MM-DD (ISO 8601)
- Severity text in Sheet: exact display name ("No Pain", "Mild", "Moderate", "Severe")
- Timestamps in Sheet Columns F–I: epoch milliseconds

**From Architecture — Code Architecture:**
- Single-module MVVM + Repository project structure
- One ViewModel per screen, no shared ViewModels
- UiState as sealed interface, never data class or open class
- StateFlow + collectAsStateWithLifecycle(), never LiveData or collectAsState()
- Repository functions always suspend — never launch their own coroutines
- ViewModels never access DAOs directly — always through Repository
- Events flow up as lambdas from Composables to ViewModels
- Severity and TimeSlot enums are the single source of truth for colors, labels, icons, numeric values
- All animation durations in AnimationSpec.kt — no inline magic numbers
- NavHost with 3 flat routes (daycard, analytics, settings)
- HorizontalPager for day navigation with date-offset page index

**From Architecture — Infrastructure:**
- Simple signing keystore (created once, stored locally)
- APK sideload distribution — no Play Store compliance
- Logcat only during development, no production logging
- Single build variant, no environment split

**From UX — Interaction Requirements:**
- Portrait-only orientation lock
- Inline expansion picker (not modal, not bottom sheet) — picker expands within the slot
- Picker collapse is instant (0ms) after selection; expand is 200ms ease-out
- Color fill bloom animation: 150ms
- All-complete day bloom: 300ms
- Day swipe animation: 250ms
- Haptic pulse on severity save (if device supports)
- Picker dismiss control (back arrow or × icon) for accidental taps
- Rest of Day Card dimmed when picker is open
- Current severity highlighted when editing existing entry
- Pointer cancellation: selection registers on tap-up, not tap-down

**From UX — Anti-Pattern Constraints:**
- NO toast messages, NO snackbars, NO "Saved!" confirmations — color fill IS the confirmation
- NO sync indicators, NO connectivity banners, NO "last synced" timestamps
- NO loading spinners (ONLY exception: PDF generation spinner)
- NO confirmation dialogs — tap = done, tap again to change
- NO onboarding, NO tutorials, NO tooltips, NO "what's new" prompts
- NO gamification — no streaks, no badges, no "Great job!" messages
- Empty states are calm and neutral — no illustrations, no motivational text, just "—"

**From UX — Accessibility Requirements:**
- Triple encoding for severity: color + text label + unique icon (never color alone)
- 48dp minimum touch targets; Day Card tiles 64dp+ height
- 12dp gap between tiles and picker options
- TalkBack content descriptions for all interactive elements (specific announcements defined in UX spec)
- Focus order: Top App Bar → Week Strip → Time Slot Tiles → Bottom Navigation
- Font scaling support up to 1.5x using sp units
- Respect Settings.Global.ANIMATOR_DURATION_SCALE — skip animations if disabled
- Week strip day cells: 44dp × 56dp minimum
- Keyboard/D-pad reachability for switch access users

**From UX — Layout Requirements:**
- Single-column fluid layout, no breakpoints
- 16dp horizontal screen margins on all sizes
- Time slot tiles stretch horizontally (full width minus margins)
- Week strip: 7 day cells distributed evenly across available width
- Severity picker options distributed evenly within tile width with minimum 8dp gaps
- No horizontal scrolling anywhere in the app
- Day Card area scrollable at maximum font scale (1.5x)

## FR Coverage Map

| FR | Epic | Description |
|----|------|-------------|
| FR1 | Epic 1 | View today's Day Card with four time slots |
| FR2 | Epic 1 | Tap time slot to open severity selector |
| FR3 | Epic 1 | Select severity to record entry |
| FR4 | Epic 1 | Dismiss severity selector without selecting |
| FR5 | Epic 1 | Change previously recorded severity |
| FR6 | Epic 1 | Log entries retroactively for past slots |
| FR7 | Epic 1 | Auto-highlight current time-of-day slot |
| FR8 | Epic 1 | Immediate visual confirmation on save |
| FR9 | Epic 1 | Completion animation when all four slots filled |
| FR10 | Epic 2 | Swipe horizontally to browse days |
| FR11 | Epic 2 | Week strip showing current week |
| FR12 | Epic 2 | Tap day in week strip for direct navigation |
| FR13 | Epic 2 | Data indicator on week strip for logged days |
| FR14 | Epic 2 | Navigate previous/next weeks |
| FR15 | Epic 2 | Current date context always displayed |
| FR16 | Epic 2 | Return to today via bottom nav "Today" tab |
| FR17 | Epic 1 | Persist entries locally immediately |
| FR18 | Epic 3 | Sync entries to Google Sheet in background |
| FR19 | Epic 3 | Queue offline entries, sync on reconnect |
| FR20 | Epic 3 | Sheet format: Date, Morning, Afternoon, Evening, Night |
| FR21 | Epic 3 | Identical online/offline behavior |
| FR22 | Epic 3 | Silent sync failure handling with retry |
| FR23 | Epic 5 | Severity trend chart with date range selection |
| FR24 | Epic 5 | Time-of-day breakdown with average severity |
| FR25 | Epic 5 | Analytics uses same severity color system |
| FR26 | Epic 5 | Default analytics view: most recent 1-week |
| FR27 | Epic 6 | Generate PDF from analytics for date range |
| FR28 | Epic 6 | PDF includes name, range, chart, summary, log table |
| FR29 | Epic 6 | Preview PDF before sharing |
| FR30 | Epic 6 | Share PDF via Android share sheet |
| FR31 | Epic 4 | Scheduled notification reminders per slot |
| FR32 | Epic 4 | Enable/disable reminders globally |
| FR33 | Epic 4 | Enable/disable reminders per slot |
| FR34 | Epic 4 | Configure reminder time per slot |
| FR35 | Epic 4 | Default times: 8 AM, 1 PM, 6 PM, 10 PM |
| FR36 | Epic 4 | Notification tap opens today's Day Card |
| FR37 | Epic 4 | Re-register reminders after device restart |
| FR38 | Epic 4 | One notification per slot, no follow-up |
| FR39 | Epic 3 | Google account sign-in |
| FR40 | Epic 3 | Configure target Google Sheet URL/ID |
| FR41 | Epic 3 | Enter patient name |
| FR42 | Epic 3 | Auto-save all settings |
| FR43 | Epic 3 | Sign out of Google account |
| FR44 | Epic 3 | Share Google Sheet link |
