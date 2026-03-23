# Project Structure & Boundaries

## Complete Project Directory Structure

```
HealthTrend/
├── .gitignore
├── build.gradle.kts                    # Root build file (AGP 9.0, Kotlin plugin)
├── settings.gradle.kts                 # Project settings, repository configuration
├── gradle.properties                   # JVM args, Android/Kotlin config flags
├── gradle/
│   ├── wrapper/
│   │   ├── gradle-wrapper.jar
│   │   └── gradle-wrapper.properties   # Gradle 9.1.0+
│   └── libs.versions.toml              # Version catalog (all dependency versions)
│
├── app/
│   ├── build.gradle.kts                # App module: dependencies, Hilt, KSP, R8
│   ├── proguard-rules.pro              # R8 keep rules for Google API, Room, Hilt
│   ├── google-services.json            # Google API client configuration
│   │
│   └── src/
│       └── main/
│           ├── AndroidManifest.xml     # Permissions, Activity, Receivers, Services
│           │
│           ├── java/com/healthtrend/app/
│           │   │
│           │   ├── HealthTrendApp.kt              # @HiltAndroidApp Application class
│           │   ├── MainActivity.kt                 # Single Activity (Compose host)
│           │   │
│           │   ├── data/
│           │   │   ├── local/
│           │   │   │   ├── HealthTrendDatabase.kt  # @Database: entities, version, DAOs
│           │   │   │   ├── HealthEntryDao.kt       # CRUD + queries by date/range/synced
│           │   │   │   ├── AppSettingsDao.kt       # Read/write single-row settings
│           │   │   │   ├── HealthEntryEntity.kt    # @Entity: health_entries table
│           │   │   │   └── AppSettingsEntity.kt    # @Entity: app_settings table
│           │   │   │
│           │   │   ├── remote/
│           │   │   │   └── GoogleSheetsService.kt  # Sheets API v4: read/write/find row
│           │   │   │
│           │   │   ├── repository/
│           │   │   │   ├── HealthEntryRepository.kt    # Entry CRUD, date queries, analytics
│           │   │   │   └── AppSettingsRepository.kt    # Settings read/write, reminder config
│           │   │   │
│           │   │   └── sync/
│           │   │       ├── SyncWorker.kt               # CoroutineWorker: push then pull
│           │   │       └── SyncManager.kt              # Enqueue immediate + periodic work
│           │   │
│           │   ├── di/
│           │   │   ├── DatabaseModule.kt           # @Module: Room DB, DAOs
│           │   │   ├── NetworkModule.kt            # @Module: Google Sheets API client
│           │   │   ├── RepositoryModule.kt         # @Module: Repository bindings
│           │   │   └── SyncModule.kt               # @Module: WorkManager, SyncManager
│           │   │
│           │   ├── domain/
│           │   │   └── model/
│           │   │       ├── Severity.kt             # Enum: NO_PAIN, MILD, MODERATE, SEVERE
│           │   │       ├── TimeSlot.kt             # Enum: MORNING, AFTERNOON, EVENING, NIGHT
│           │   │       └── HealthEntry.kt          # Domain model (mapped from entity)
│           │   │
│           │   ├── ui/
│           │   │   ├── theme/
│           │   │   │   ├── Theme.kt                # MaterialTheme with fixed severity colors
│           │   │   │   ├── Color.kt                # All color definitions (severity + neutral)
│           │   │   │   ├── Type.kt                 # Typography scale (Material 3, Roboto)
│           │   │   │   ├── Shape.kt                # Shape definitions (pill badges, cards)
│           │   │   │   └── AnimationSpec.kt        # All animation duration constants
│           │   │   │
│           │   │   ├── navigation/
│           │   │   │   └── HealthTrendNavHost.kt   # NavHost with 3 routes + bottom bar
│           │   │   │
│           │   │   ├── components/
│           │   │   │   ├── SeverityPill.kt         # Severity pill badge (shared)
│           │   │   │   └── TimeSlotIcon.kt         # Static time-of-day icon (shared)
│           │   │   │
│           │   │   ├── daycard/
│           │   │   │   ├── DayCardScreen.kt        # Screen + HorizontalPager
│           │   │   │   ├── DayCardViewModel.kt     # State: entries, expanded slot, date
│           │   │   │   ├── WeekStripBar.kt         # Week navigation strip
│           │   │   │   ├── TimeSlotTile.kt         # Individual time slot tile
│           │   │   │   └── SeverityPicker.kt       # Inline severity selector
│           │   │   │
│           │   │   ├── analytics/
│           │   │   │   ├── AnalyticsScreen.kt      # Screen + date range chips
│           │   │   │   ├── AnalyticsViewModel.kt   # State: chart data, range, averages
│           │   │   │   ├── TrendChart.kt           # Vico line chart composable
│           │   │   │   ├── SlotAverageCards.kt     # Time-of-day breakdown cards
│           │   │   │   └── PdfExportManager.kt     # PDF generation, preview, share
│           │   │   │
│           │   │   └── settings/
│           │   │       ├── SettingsScreen.kt       # Screen + auto-save fields
│           │   │       └── SettingsViewModel.kt    # State: auth status, settings
│           │   │
│           │   └── notification/
│           │       ├── NotificationScheduler.kt    # AlarmManager: schedule/cancel alarms
│           │       ├── NotificationReceiver.kt     # BroadcastReceiver: fires notification
│           │       └── BootReceiver.kt             # BOOT_COMPLETED: re-register alarms
│           │
│           └── res/
│               ├── values/
│               │   ├── strings.xml                 # All user-facing strings
│               │   ├── colors.xml                  # XML color definitions
│               │   └── themes.xml                  # Base theme (Material 3)
│               ├── drawable/
│               │   └── ic_notification.xml         # Notification icon
│               └── mipmap-*/
│                   └── ic_launcher.png             # App icon (all densities)
```

## Architectural Boundaries

**Data Layer Boundary:**

```
UI (Composables) → ViewModels → Repositories → DAOs / GoogleSheetsService
                                                      │
                                                Room Database
                                                Google Sheets API
```

- ViewModels never access DAOs or GoogleSheetsService directly — always through Repository.
- Repositories never access UI state — they return data or `Result<T>`.
- SyncWorker accesses Repository (not DAOs directly) for consistency.

**UI Layer Boundary:**

- Each screen package (`daycard/`, `analytics/`, `settings/`) is self-contained: Screen + ViewModel + screen-specific composables.
- `ui/components/` holds ONLY composables shared across 2+ screens.
- `ui/theme/` is the single source for colors, typography, shapes, and animation specs.
- No screen imports composables from another screen's package.

**Sync Boundary:**

```
Local Entry Save → SyncManager.enqueueSync()
                        │
                   WorkManager
                        │
                   SyncWorker.doWork()
                        ├── Push Phase: Repository → GoogleSheetsService (write cells)
                        └── Pull Phase: GoogleSheetsService (read rows) → Repository
```

- Sync is triggered by SyncManager — ViewModels don't know about WorkManager.
- SyncWorker is the only class that orchestrates push + pull.
- GoogleSheetsService handles raw Sheets API calls. Sync logic lives in SyncWorker.

**Notification Boundary:**

```
Settings (reminder config) → NotificationScheduler → AlarmManager
                                                          │
BootReceiver → NotificationScheduler (re-register)   NotificationReceiver (fires)
```

- NotificationScheduler is the single interface for scheduling/cancelling alarms.
- SettingsViewModel calls NotificationScheduler when reminder settings change.
- BootReceiver calls NotificationScheduler.rescheduleAll() on device restart.

## Requirements to Structure Mapping

| FR Category | Primary Files |
|------------|--------------|
| Symptom Logging (FR1–FR9) | `TimeSlotTile.kt`, `SeverityPicker.kt`, `DayCardScreen.kt`, `DayCardViewModel.kt`, `HealthEntryRepository.kt` |
| Day Navigation (FR10–FR16) | `DayCardScreen.kt` (HorizontalPager), `WeekStripBar.kt`, `DayCardViewModel.kt` |
| Data Persistence (FR17–FR22) | `HealthEntryEntity.kt`, `HealthEntryDao.kt`, `HealthEntryRepository.kt`, `SyncWorker.kt`, `SyncManager.kt`, `GoogleSheetsService.kt` |
| Analytics (FR23–FR26) | `AnalyticsScreen.kt`, `AnalyticsViewModel.kt`, `TrendChart.kt`, `SlotAverageCards.kt` |
| Report Generation (FR27–FR30) | `PdfExportManager.kt`, `AnalyticsScreen.kt` |
| Notifications (FR31–FR38) | `NotificationScheduler.kt`, `NotificationReceiver.kt`, `BootReceiver.kt` |
| App Configuration (FR39–FR44) | `SettingsScreen.kt`, `SettingsViewModel.kt`, `AppSettingsEntity.kt`, `AppSettingsDao.kt`, `AppSettingsRepository.kt` |

## Cross-Cutting Concerns Mapping

| Concern | Files |
|---------|-------|
| Severity model | `domain/model/Severity.kt` — single source, used everywhere |
| TimeSlot model | `domain/model/TimeSlot.kt` — single source, used everywhere |
| Color system | `ui/theme/Color.kt` — references Severity enum colors |
| Animation durations | `ui/theme/AnimationSpec.kt` — all constants |
| Auth / Google Sign-In | `SettingsScreen.kt` (UI), `GoogleSheetsService.kt` (token usage) |

## Data Flow Diagrams

**Entry Save Flow:**

```
User taps severity
  → SeverityPicker (UI event)
  → DayCardViewModel.saveSeverity(date, slot, severity)
  → HealthEntryRepository.saveEntry(entry) [updatedAt = now, synced = false]
  → HealthEntryDao.upsert(entry) [Room write — instant]
  → SyncManager.enqueueImmediateSync() [WorkManager one-time request]
  → UI updates via StateFlow (Room Flow query auto-emits)
```

**Sync Flow:**

```
WorkManager triggers SyncWorker.doWork()
  → Push Phase:
    → HealthEntryRepository.getUnsyncedEntries()
    → For each: GoogleSheetsService.readTimestamp(date, slot)
    → If local newer: GoogleSheetsService.writeCell(date, slot, severity, timestamp)
    → HealthEntryRepository.markSynced(entry)
  → Pull Phase:
    → GoogleSheetsService.readAllRows()
    → For each cell with data: compare Sheet timestamp vs local updatedAt
    → If Sheet newer: HealthEntryRepository.upsertFromSheet(entry)
  → Return Result.success() or Result.retry()
```

**Analytics Flow:**

```
User navigates to Analytics tab
  → AnalyticsViewModel collects entries for date range from HealthEntryRepository
  → Repository queries HealthEntryDao with date range
  → Room Flow emits results
  → ViewModel transforms to chart data + slot averages
  → TrendChart + SlotAverageCards render from UiState
```
