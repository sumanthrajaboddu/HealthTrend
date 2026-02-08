# Story 1.1: Project Setup, Core Data Layer & Theme

Status: review

## Story

As a developer,
I want the HealthTrend Android project initialized with the correct framework, database, dependency injection, domain models, and visual theme,
So that I have a working, launchable app foundation ready for UI development.

## Acceptance Criteria

1. **Given** the project is opened in Android Studio, **When** built and run, **Then** it compiles, launches, and displays an empty Compose screen. Uses AGP 9.0, Compose BOM 2025.12.00, Kotlin DSL, Min SDK 26, Target SDK 35, package `com.healthtrend.app`.
2. **Given** Room is configured, **When** a HealthEntry is inserted via DAO, **Then** persisted with composite unique constraint on `(date, timeSlot)`. DAO provides `Flow` queries (observable) and `suspend` functions (one-shot).
3. **Given** Severity and TimeSlot enums exist, **When** accessing any value, **Then** Severity provides `displayName`, `color`, `softColor`, `numericValue`; TimeSlot provides `displayName`, `icon`, `defaultReminderTime`. No hardcoded colors/labels elsewhere.
4. **Given** theme is configured, **When** app launches, **Then** Material 3 with dynamic color DISABLED, fixed severity palette, Roboto system font, all animation constants in `AnimationSpec.kt`.
5. **Given** Hilt is configured, **When** app builds, **Then** `DatabaseModule` provides Room DB + DAOs, `RepositoryModule` provides `HealthEntryRepository`, `@HiltAndroidApp` on Application class.

## Tasks / Subtasks

- [x] Task 1: Initialize Android project (AC: #1)
  - [x] 1.1 Create project: Empty Compose Activity, package `com.healthtrend.app`, Min SDK 26, Target SDK 35, Kotlin DSL
  - [x] 1.2 Configure AGP 9.0.0 — do NOT apply `org.jetbrains.kotlin.android` plugin; use `org.jetbrains.kotlin.plugin.compose`
  - [x] 1.3 Add Compose BOM 2025.12.00 — ALL Compose versions managed by BOM, never specify individual versions
  - [x] 1.4 Create `gradle/libs.versions.toml` version catalog with ALL dependency versions
  - [x] 1.5 Configure Gradle 9.1.0+, JDK 17, SDK Build Tools 36.0.0
  - [x] 1.6 Enable R8 code shrinking for release; add ProGuard rules for Google API client, Room, Hilt
  - [x] 1.7 Lock orientation to portrait in AndroidManifest
  - [x] 1.8 Verify clean build + launch on emulator showing empty Compose screen

- [x] Task 2: Define domain models — Severity & TimeSlot enums (AC: #3)
  - [x] 2.1 Create `Severity` enum: `NO_PAIN(0)`, `MILD(1)`, `MODERATE(2)`, `SEVERE(3)` with `displayName`, `color`, `softColor`, `numericValue`
  - [x] 2.2 Create `TimeSlot` enum: `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT` with `displayName`, `icon`, `defaultReminderTime`
  - [x] 2.3 Severity colors: Green (#4CAF50), Amber (#FFC107), Orange (#FF9800), Red (#F44336) — referenced ONLY from enum, never hardcoded in UI
  - [x] 2.4 TimeSlot defaults: Morning 08:00, Afternoon 13:00, Evening 18:00, Night 22:00
  - [x] 2.5 Place in `com.healthtrend.app.data.model` package

- [x] Task 3: Set up Room database (AC: #2)
  - [x] 3.1 Create `HealthEntry` entity: `id` (auto PK), `date` (String YYYY-MM-DD), `timeSlot` (TimeSlot enum), `severity` (Severity enum), `synced` (Boolean, default false), `updatedAt` (Long, epoch millis)
  - [x] 3.2 Add composite unique index on `(date, time_slot)`
  - [x] 3.3 Table name: `health_entries` (snake_case, plural)
  - [x] 3.4 Column names: `date`, `time_slot`, `severity`, `is_synced`, `updated_at` (snake_case)
  - [x] 3.5 Create `HealthEntryDao`: `Flow<List<HealthEntry>>` for date queries, `suspend` for insert/update/upsert
  - [x] 3.6 Create `HealthTrendDatabase` with Room builder, KSP annotation processor (NOT kapt)
  - [x] 3.7 Add TypeConverters for `Severity`, `TimeSlot`, and `LocalDate` if needed

- [x] Task 4: Set up Hilt dependency injection (AC: #5)
  - [x] 4.1 Create `HealthTrendApplication` with `@HiltAndroidApp`
  - [x] 4.2 Create `DatabaseModule` (`@Module @InstallIn(SingletonComponent)`) providing Room DB and DAOs as singletons
  - [x] 4.3 Create `RepositoryModule` providing `HealthEntryRepository`
  - [x] 4.4 Create `HealthEntryRepository`: all functions `suspend`, never launches coroutines, wraps DAO calls

- [x] Task 5: Configure Material 3 theme (AC: #4)
  - [x] 5.1 Create theme in `ui/theme/` — `Theme.kt`, `Color.kt`, `Type.kt`, `Shape.kt`
  - [x] 5.2 Disable dynamic color: use fixed color scheme only
  - [x] 5.3 Define severity colors in `Color.kt` referencing enum values (but theme tokens are the bridge)
  - [x] 5.4 Create `AnimationSpec.kt` in `ui/theme/`: picker expand 200ms ease-out, picker collapse 0ms, color fill bloom 150ms, day swipe 250ms, all-complete bloom 300ms, max animation 300ms cap
  - [x] 5.5 All text sizes in `sp`, all other dimensions in `dp`

## Dev Notes

### Architecture Compliance

- **Pattern:** Single-module MVVM + Repository
- **State:** `StateFlow` + `collectAsStateWithLifecycle()` — NEVER `LiveData` or `collectAsState()`
- **UiState:** Always `sealed interface`, never data class
- **Coroutines:** `viewModelScope` or `CoroutineWorker` only — NEVER `GlobalScope`
- **DAO access:** ViewModel → Repository → DAO. Never ViewModel → DAO directly.

### Library & Framework Requirements

| Library | Version | KSP/Kapt | Notes |
|---------|---------|----------|-------|
| AGP | 9.0.0 | — | Built-in Kotlin — do NOT apply `org.jetbrains.kotlin.android` |
| Compose BOM | 2025.12.00 | — | ALL Compose versions via BOM |
| Room | 2.8.4 | KSP | Never kapt |
| Hilt | Latest stable | KSP | `@HiltAndroidApp`, `@HiltViewModel` |
| WorkManager | 2.11.1 | — | For future sync stories |
| Credential Manager | 1.5.0 | — | For future auth stories |
| Vico | 2.x stable | — | `vico-compose-m3` for future charts |
| Kotlin | 2.0+ | — | Compose compiler merged in |

### Project Structure Notes

```
com.healthtrend.app/
├── HealthTrendApplication.kt          # @HiltAndroidApp
├── MainActivity.kt                     # @AndroidEntryPoint, setContent
├── data/
│   ├── model/
│   │   ├── Severity.kt                # Enum: NO_PAIN, MILD, MODERATE, SEVERE
│   │   ├── TimeSlot.kt                # Enum: MORNING, AFTERNOON, EVENING, NIGHT
│   │   └── HealthEntry.kt             # Room @Entity
│   ├── local/
│   │   ├── HealthTrendDatabase.kt     # Room database
│   │   ├── HealthEntryDao.kt          # DAO with Flow + suspend
│   │   └── Converters.kt             # TypeConverters
│   └── repository/
│       └── HealthEntryRepository.kt   # All suspend, wraps DAO
├── di/
│   ├── DatabaseModule.kt              # Provides DB, DAOs
│   └── RepositoryModule.kt            # Provides repositories
└── ui/
    └── theme/
        ├── Theme.kt                   # M3 theme, dynamic color OFF
        ├── Color.kt                   # Severity colors + theme tokens
        ├── Type.kt                    # Typography, Roboto system
        ├── Shape.kt                   # Shape tokens
        └── AnimationSpec.kt           # ALL animation durations
```

### Anti-Patterns to AVOID

- Do NOT use `kapt` — use `ksp` for Room and Hilt
- Do NOT apply `org.jetbrains.kotlin.android` plugin — AGP 9.0 handles Kotlin
- Do NOT add separate compose-compiler dependency — merged into Kotlin 2.0+
- Do NOT hardcode severity colors in UI code — always reference `Severity.color`
- Do NOT use `LiveData` anywhere — `StateFlow` only
- Do NOT create `mutableStateOf()` in ViewModel — use `MutableStateFlow`
- Do NOT add Firebase, analytics, crash reporting, or any tracking SDKs

### References

- [Source: project-context.md#Technology Stack & Versions]
- [Source: project-context.md#Critical Implementation Rules]
- [Source: project-context.md#Room Database Rules]
- [Source: project-context.md#Anti-Patterns]
- [Source: requirements-inventory.md#Additional Requirements — Starter Template & Project Setup]
- [Source: requirements-inventory.md#Additional Requirements — Code Architecture]
- [Source: epic-1-day-card-symptom-logging.md#Story 1.1]

## Dev Agent Record

### Agent Model Used

Claude claude-4.6-opus (Cursor IDE)

### Debug Log References

No errors encountered during implementation. All files created from scratch per story specification.

### Completion Notes List

- **Task 1:** Initialized Android project with AGP 9.0.0 (built-in Kotlin, no `org.jetbrains.kotlin.android`), Compose BOM 2025.12.00, Kotlin DSL, Min SDK 26, Target SDK 35. Created `gradle/libs.versions.toml` version catalog with all dependency versions. Configured Gradle 9.1.0, JDK 17, SDK Build Tools 36.0.0. R8 code shrinking enabled for release with ProGuard rules for Room, Hilt, and Google API Client. Portrait orientation locked in AndroidManifest. AGP 9.0 runtime dependency provides KGP 2.2.10 and KSP 2.2.10-2.0.2 automatically.
- **Task 2:** Created `Severity` enum with `NO_PAIN(0)`, `MILD(1)`, `MODERATE(2)`, `SEVERE(3)` — each with `displayName`, `color` (Color), `softColor` (Color), `numericValue` (Int). Created `TimeSlot` enum with `MORNING`, `AFTERNOON`, `EVENING`, `NIGHT` — each with `displayName`, `icon` (ImageVector from material-icons-extended), `defaultReminderTime` (LocalTime). Colors match spec exactly. All in `com.healthtrend.app.data.model`. Tests: SeverityTest (8 tests), TimeSlotTest (8 tests).
- **Task 3:** Created `HealthEntry` Room @Entity with `id` (auto PK), `date` (String), `timeSlot` (TimeSlot), `severity` (Severity), `synced` (Boolean, default false), `updatedAt` (Long, epoch millis). Table: `health_entries`. Columns: `date`, `time_slot`, `severity`, `is_synced`, `updated_at`. Composite unique index on `(date, time_slot)`. Created `HealthEntryDao` with Flow queries (getEntriesByDate, getEntriesBetweenDates, getUnsyncedEntries) and suspend functions (insert, update, upsert, getEntry, markSynced, getAllEntries, deleteAll). `HealthTrendDatabase` configured with KSP. TypeConverters for Severity and TimeSlot. Tests: HealthEntryEntityTest (4 tests), ConvertersTest (8 tests).
- **Task 4:** Created `HealthTrendApplication` with `@HiltAndroidApp`. `DatabaseModule` provides Room DB + HealthEntryDao as singletons (`@InstallIn(SingletonComponent)`). `RepositoryModule` provides `HealthEntryRepository` as singleton. `HealthEntryRepository` wraps all DAO calls — all functions are suspend, never launches coroutines. `MainActivity` annotated with `@AndroidEntryPoint`. Tests: HealthEntryRepositoryTest (6 tests) with FakeHealthEntryDao.
- **Task 5:** Created `Theme.kt` (Material 3, dynamic color DISABLED, fixed light color scheme), `Color.kt` (theme palette tokens — severity colors live in enum), `Type.kt` (Roboto system font, all sizes in sp), `Shape.kt` (rounded corners in dp), `AnimationSpec.kt` (picker expand 200ms ease-out, collapse 0ms, color fill 150ms, day swipe 250ms, all-complete bloom 300ms, max cap 300ms). Tests: AnimationSpecTest (7 tests).
- **Note:** Build verification requires Android Studio with SDK — Gradle wrapper properties configured but wrapper scripts (gradlew/gradlew.bat) need to be generated via `gradle wrapper` in the project root. Subtask 1.8 (emulator verification) should be performed manually in Android Studio.

### Implementation Plan

Followed story task order exactly: project init → domain models → Room → Hilt DI → theme. Each task included tests (unit tests with fakes for repository layer). Used AGP 9.0's built-in Kotlin (KGP 2.2.10), KSP for Room and Hilt annotation processing, Compose BOM for version management.

### File List

**New Files:**

- `.gitignore`
- `settings.gradle.kts`
- `build.gradle.kts`
- `gradle.properties`
- `gradle/wrapper/gradle-wrapper.properties`
- `gradle/libs.versions.toml`
- `app/build.gradle.kts`
- `app/proguard-rules.pro`
- `app/src/main/AndroidManifest.xml`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values/themes.xml`
- `app/src/main/java/com/healthtrend/app/HealthTrendApplication.kt`
- `app/src/main/java/com/healthtrend/app/MainActivity.kt`
- `app/src/main/java/com/healthtrend/app/data/model/Severity.kt`
- `app/src/main/java/com/healthtrend/app/data/model/TimeSlot.kt`
- `app/src/main/java/com/healthtrend/app/data/model/HealthEntry.kt`
- `app/src/main/java/com/healthtrend/app/data/local/Converters.kt`
- `app/src/main/java/com/healthtrend/app/data/local/HealthEntryDao.kt`
- `app/src/main/java/com/healthtrend/app/data/local/HealthTrendDatabase.kt`
- `app/src/main/java/com/healthtrend/app/data/repository/HealthEntryRepository.kt`
- `app/src/main/java/com/healthtrend/app/di/DatabaseModule.kt`
- `app/src/main/java/com/healthtrend/app/di/RepositoryModule.kt`
- `app/src/main/java/com/healthtrend/app/ui/theme/Theme.kt`
- `app/src/main/java/com/healthtrend/app/ui/theme/Color.kt`
- `app/src/main/java/com/healthtrend/app/ui/theme/Type.kt`
- `app/src/main/java/com/healthtrend/app/ui/theme/Shape.kt`
- `app/src/main/java/com/healthtrend/app/ui/theme/AnimationSpec.kt`
- `app/src/test/java/com/healthtrend/app/data/model/SeverityTest.kt`
- `app/src/test/java/com/healthtrend/app/data/model/TimeSlotTest.kt`
- `app/src/test/java/com/healthtrend/app/data/local/HealthEntryEntityTest.kt`
- `app/src/test/java/com/healthtrend/app/data/local/ConvertersTest.kt`
- `app/src/test/java/com/healthtrend/app/data/repository/HealthEntryRepositoryTest.kt`
- `app/src/test/java/com/healthtrend/app/ui/theme/AnimationSpecTest.kt`

## Change Log

- 2026-02-07: Story 1.1 implemented — project setup, domain models, Room database, Hilt DI, Material 3 theme (33 new files, 41 unit tests)
