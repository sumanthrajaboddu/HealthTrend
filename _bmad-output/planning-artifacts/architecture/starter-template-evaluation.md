# Starter Template Evaluation

## Primary Technology Domain

Native Android Mobile — Kotlin + Jetpack Compose + Material 3, as established in the PRD. The starter options are specific to the Android ecosystem.

## Starter Options Considered

| Option | Description | Verdict |
|--------|------------|---------|
| Android Studio "Empty Compose Activity" | Google's official project wizard. Creates a minimal Compose project with correct Gradle setup, Material 3 dependency, and a single Activity. | **Selected** — clean, minimal, current |
| Community Compose templates | Pre-built templates with navigation, DI, and architecture layers baked in. | Not recommended — opinionated choices may conflict with requirements, maintenance uncertain |
| NowInAndroid-style architecture templates | Google's sample app showing "production" patterns. Heavily modularized. | Not recommended — massively over-engineered for a 3-screen personal app |

## Selected Starter: Android Studio Empty Compose Activity

**Rationale:** For a low-complexity, 3-screen app built by a solo developer, the minimal official template is the right foundation. It provides correct AGP/Compose/Material 3 wiring, and we layer on exactly the libraries needed — nothing more.

**Initialization:**

```
Android Studio → New Project → Empty Compose Activity
  Name: HealthTrend
  Package: com.healthtrend.app
  Language: Kotlin
  Minimum SDK: API 26 (Android 8.0)
  Build configuration language: Kotlin DSL (build.gradle.kts)
```

## Technology Stack — Current Versions

**Core Platform (provided by starter):**

| Technology | Version | Role |
|-----------|---------|------|
| Android Gradle Plugin (AGP) | 9.0.0 (Jan 2026) | Build system |
| Gradle | 9.1.0+ | Build runner (required by AGP 9.0) |
| Kotlin | 2.0+ | Language (Compose compiler now merged into Kotlin) |
| Compose BOM | 2025.12.00 | Version-aligned Compose libraries |
| Material 3 | via Compose BOM | UI component library |
| JDK | 17 | Required by AGP 9.0 |
| SDK Build Tools | 36.0.0 | Required by AGP 9.0 |

**Note on AGP 9.0:** Major release (January 2026). Built-in Kotlin support is now default — the separate `org.jetbrains.kotlin.android` plugin is no longer needed. Apply `org.jetbrains.kotlin.plugin.compose` for Compose support instead.

**Libraries to Add:**

| Library | Version | Purpose | Why This One |
|---------|---------|---------|-------------|
| Room | 2.8.4 (stable) | Local SQLite database | Official Jetpack. Uses KSP (not kapt). Zero-cost local storage on device. |
| Hilt | Latest stable | Dependency injection | Google's official DI for Android. Compile-time safety. @HiltViewModel integrates with Compose. |
| Vico | 2.x stable | Charting for Analytics | Most actively maintained Compose-native charting library. Has vico-compose-m3 module for Material 3. |
| WorkManager | 2.11.1 | Background Google Sheets sync | Jetpack official. Survives app close and device restart. Built-in retry with backoff. |
| Credential Manager | 1.5.0 (stable) | Google Sign-In (OAuth 2.0) | Replaces legacy Google Sign-In SDK. Google's current recommended auth approach (2025 Q4+). |
| Google Sheets API | v4-rev20251110-2.0.0 | Google Sheets data push | Official Google API client. |
| KSP | Matching Kotlin version | Kotlin Symbol Processing | Required by Room compiler. Faster than kapt. |
| Kotlin Coroutines | via BOM | Async operations | Room queries, network calls, WorkManager. |
| Navigation Compose | via Compose BOM | Screen navigation | Bottom nav integration with 3 destinations. |

**Libraries Explicitly NOT Included (per NFR10):**

- No Firebase (no Crashlytics, no Analytics, no Cloud Messaging)
- No third-party analytics SDKs
- No crash reporting SDKs
- No Retrofit/OkHttp (Google API client handles HTTP)

## Styling Solution

Material 3 via Jetpack Compose with custom theme disabling dynamic color. Fixed severity color palette with soft variants. Material 3 neutral tonal palette for app chrome. Roboto system font. sp units for text, dp for spacing. Light theme only.

## Code Organization

Single-module MVVM + Repository project:

```
com.healthtrend.app/
├── data/
│   ├── local/          # Room database, DAOs, entities
│   ├── remote/         # Google Sheets API service
│   ├── repository/     # Repository implementations
│   └── sync/           # WorkManager sync workers
├── di/                 # Hilt modules
├── domain/
│   └── model/          # Domain models (Severity enum, Entry, etc.)
├── ui/
│   ├── theme/          # Material 3 theme, colors, typography
│   ├── components/     # Shared composables (SeverityPill, TimeSlotTile, etc.)
│   ├── daycard/        # Day Card screen + ViewModel
│   ├── analytics/      # Analytics screen + ViewModel
│   └── settings/       # Settings screen + ViewModel
├── notification/       # AlarmManager scheduler, BootReceiver
└── HealthTrendApp.kt   # Application class (@HiltAndroidApp)
```

**Why single-module?** This is a 3-screen personal app with ~12 components. Multi-module architecture adds Gradle complexity and inter-module dependency management that provides zero benefit at this scale.

## Development Experience

- Hot reload: Compose Preview + Live Edit in Android Studio
- Debugging: Standard Android Studio debugger + Layout Inspector for Compose
- Testing: Manual on 2-3 real devices (per UX spec testing matrix)

**Note:** Project initialization using Android Studio's wizard should be the first implementation step.
