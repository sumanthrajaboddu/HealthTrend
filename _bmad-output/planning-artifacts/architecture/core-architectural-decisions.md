# Core Architectural Decisions

## Decision Priority Analysis

**Critical Decisions (Block Implementation):**

1. Two-way timestamp-based sync with Google Sheets (shapes entire data layer)
2. Room entity design with per-slot rows and timestamps (foundation for all queries)
3. Credential Manager for persistent one-time Google Sign-In (blocks sync)
4. Cell-level Sheet writes with timestamp conflict resolution (blocks multi-device safety)

**Important Decisions (Shape Architecture):**

5. ViewModel + StateFlow state management pattern (shapes all UI code)
6. HorizontalPager for day navigation (shapes Day Card screen)
7. Hilt dependency injection (shapes all class construction)
8. Vico charting for analytics (shapes Analytics screen)

**Deferred Decisions (Post-MVP):**

- Dark theme color variants
- Play Store signing and distribution
- Multi-symptom type data model expansion
- Widget architecture

## Data Architecture

**Room Entity: `HealthEntry`**

| Field | Type | Purpose |
|-------|------|---------|
| id | Long (auto-generated) | Primary key |
| date | LocalDate | Which day this entry belongs to |
| timeSlot | Enum (MORNING, AFTERNOON, EVENING, NIGHT) | Which time slot |
| severity | Enum (NO_PAIN, MILD, MODERATE, SEVERE) | Severity level |
| updatedAt | Long (epoch millis) | Last modified timestamp — used for sync conflict resolution |
| synced | Boolean | Whether this entry has been pushed to Sheet since last modification |

**Unique constraint:** Composite key on `(date, timeSlot)` — only one entry per slot per day.

**Room Entity: `AppSettings`**

Single-row table for app configuration:

| Field | Type | Purpose |
|-------|------|---------|
| patientName | String? | Used in PDF report headers |
| sheetUrl | String? | Google Sheet URL or ID |
| remindersEnabled | Boolean | Global reminder toggle |
| morningReminderEnabled | Boolean | Per-slot toggle |
| morningReminderTime | String | Per-slot time (e.g., "08:00") |
| afternoonReminderEnabled | Boolean | Per-slot toggle |
| afternoonReminderTime | String | Per-slot time (e.g., "13:00") |
| eveningReminderEnabled | Boolean | Per-slot toggle |
| eveningReminderTime | String | Per-slot time (e.g., "18:00") |
| nightReminderEnabled | Boolean | Per-slot toggle |
| nightReminderTime | String | Per-slot time (e.g., "22:00") |

**Data Validation:** Validated at the domain model level via Kotlin sealed classes/enums. Severity and TimeSlot are enums — invalid values are impossible by construction. Dates validated at entry point (UI layer).

**Migration Strategy:** Room auto-migration for schema changes. Version-tracked in `@Database(version = N)`. For a single-user app, destructive migration with re-sync from Sheet is an acceptable fallback.

## Authentication & Security

**One-Time Persistent Google Sign-In:**

- Technology: Credential Manager 1.5.0 (replaces legacy Google Sign-In SDK)
- Flow: Raja signs in once during initial setup → credential persisted in Android secure storage
- Token refresh: Automatic and silent via Credential Manager — user never sees a re-auth prompt
- Persistence: Survives app restarts, device reboots, force stops
- Sign-out: Available in Settings but expected to never be used in normal operation
- Scope: `https://www.googleapis.com/auth/spreadsheets` (read + write to Sheets)

**Architectural Rule:** Zero re-authentication after initial setup. If token refresh fails (e.g., Google account password changed), surface a single "Please sign in again" prompt in Settings — not a blocking error.

**Token Storage:** Managed entirely by Credential Manager. No custom keystore implementation.

**Data Security:** Local data in Room (standard Android app sandbox). No encryption at rest beyond Android platform defaults. OAuth tokens in Credential Manager secure storage. No health data sent anywhere except user's own configured Google Sheet.

## API & Communication Patterns

**Two-Way Sync Architecture:**

Direction: Push local entries to Sheet, then pull all Sheet data to local.

**Google Sheet Format:**

```
Column A: Date (YYYY-MM-DD)
Column B: Morning (severity text: "No Pain" / "Mild" / "Moderate" / "Severe")
Column C: Afternoon
Column D: Evening
Column E: Night
Column F: Morning_ts (epoch millis timestamp)
Column G: Afternoon_ts
Column H: Evening_ts
Column I: Night_ts
```

Columns A–E are human-readable. Columns F–I are sync metadata (can be hidden by user in Google Sheets).

**Sync Trigger Strategy:**

- Immediate: Enqueue unique one-time WorkManager request after each local entry save
- Periodic fallback: Hourly periodic work as safety net
- On app launch: Trigger sync to pull latest data from Sheet
- Work policy: `ExistingWorkPolicy.KEEP` prevents duplicate immediate syncs

**Sync Worker — Push Phase:**

1. Query local entries where `synced = false`
2. For each entry, read the corresponding `_ts` column from Sheet
3. If local `updatedAt` > Sheet timestamp (or no Sheet timestamp): write severity + timestamp to Sheet
4. If local `updatedAt` <= Sheet timestamp: skip (Sheet has newer data)
5. Mark local entry `synced = true`

**Sync Worker — Pull Phase:**

1. Read all rows from Sheet (or relevant date range for performance)
2. For each cell with data:
   - If Sheet timestamp > local `updatedAt` (or entry doesn't exist locally): create/update local entry
   - If Sheet timestamp <= local `updatedAt`: keep local (already newer)
3. Local DB now reflects newest data from all devices

**Conflict Resolution:** Timestamp-based — newest `updatedAt` wins on both push and pull. Fully automatic, no user intervention.

**Multi-Device Safety Rules:**

1. Only write cells that have actual severity data — never write empty/null
2. Cell-level writes — never overwrite an entire row
3. Timestamp comparison before every write — never overwrite newer data with older
4. Each device pushes only its own new/modified entries
5. Every device pulls the complete picture from Sheet

**Idempotency:** Date is the unique row key. Timestamps ensure running sync multiple times produces the same correct result.

**Error Handling:** WorkManager `BackoffPolicy.EXPONENTIAL` with 30-second initial delay. All failures silent to user. Token refresh handled by Credential Manager. Zero user-facing error states for sync.

## Frontend Architecture

**State Management: ViewModel + StateFlow**

Each screen has one ViewModel exposing a `StateFlow<UiState>` where `UiState` is a sealed class:

```kotlin
// Example: Day Card
sealed class DayCardUiState {
    object Loading : DayCardUiState()
    data class Ready(
        val date: LocalDate,
        val entries: Map<TimeSlot, HealthEntry?>,
        val expandedSlot: TimeSlot?,
        val isToday: Boolean
    ) : DayCardUiState()
}
```

Composables collect state with `collectAsStateWithLifecycle()`. Events flow up via lambda callbacks. Unidirectional data flow: UI → ViewModel (events) → Repository → Room → ViewModel (state) → UI.

**Day Pager: HorizontalPager**

- `HorizontalPager` from `foundation.pager` with `PagerState`
- Page index maps to date offset from an anchor date (e.g., page 0 = 2020-01-01, today = page ~2,228)
- Initial page set to today's index
- Forward navigation capped at today (future pages show empty, non-interactive cards)
- Pager state synced bidirectionally with WeekStripBar selection
- Smooth swipe animation at 250ms (per UX spec)

**Navigation: NavHost with 3 Flat Routes**

```kotlin
NavHost(navController, startDestination = "daycard") {
    composable("daycard") { DayCardScreen() }
    composable("analytics") { AnalyticsScreen() }
    composable("settings") { SettingsScreen() }
}
```

Bottom `NavigationBar` always visible. 3 `NavigationBarItem`s. No nested navigation. "Today" tab always resets to today's date. Standard Material 3 crossfade transitions (300ms).

## Infrastructure & Deployment

**Build Configuration:**

- Release APK with R8 code shrinking enabled
- Simple signing keystore (created once, stored locally by Raja)
- No Play Store compliance, no app bundle — direct APK sideload
- ProGuard rules for Google API client, Room, and Hilt (standard keep rules)

**Logging & Observability:**

- Logcat only during development. No production logging layer.
- No crash reporting SDKs (NFR10). If issues arise, Raja connects phone to Android Studio.

**Environment Configuration:**

- Single build variant (release). No staging/production split needed.
- Google API client ID configured in `google-services.json` or Credential Manager setup.
- Sheet URL stored in Room `AppSettings` — user-configurable, not build-time.

## Decision Impact Analysis

**Implementation Sequence:**

1. Project initialization (Android Studio wizard + dependencies)
2. Room database with HealthEntry and AppSettings entities
3. Hilt DI setup
4. Day Card UI (TimeSlotTile, SeverityPicker, WeekStripBar, HorizontalPager)
5. Credential Manager Google Sign-In (Settings screen)
6. Two-way sync worker (push + pull with timestamps)
7. Notification scheduler (AlarmManager + BootReceiver)
8. Analytics screen (Vico charts + SlotAverageCards)
9. PDF export (report generation + preview + share)

**Cross-Component Dependencies:**

| Component | Depends On |
|-----------|-----------|
| Day Card UI | Room HealthEntry, Severity enum, TimeSlot enum |
| Sync Worker | Room HealthEntry, Credential Manager (auth token), Google Sheets API |
| Analytics | Room HealthEntry (date range queries), Vico library |
| PDF Export | Room HealthEntry (date range), Analytics chart rendering |
| Notifications | AppSettings (reminder config), AlarmManager |
| Settings | Credential Manager, Room AppSettings, AlarmManager (reminder scheduling) |
