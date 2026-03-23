# HealthTrend

An Android app for tracking daily symptom severity across four time slots — Morning, Afternoon, Evening, and Night. Designed for patients to log symptoms and for caregivers to monitor trends over time.

## Features

- **Daily Symptom Logging** — Record severity (No Pain, Mild, Moderate, Severe) per time slot with a horizontal day pager, week strip with data indicators, and color-coded tiles
- **Analytics & Trends** — Severity trend chart with configurable date ranges (1 week to 1 year) and time-of-day breakdown cards
- **PDF Reports** — Generate, preview, and share PDF reports including trend charts, time-of-day summaries, and daily log tables
- **Google Sheets Sync** — Two-way sync with Google Sheets via WorkManager, with conflict resolution (newest timestamp wins) and cell-level writes
- **Reminder Notifications** — Configurable per-slot reminders using AlarmManager with exact alarms, persisted across reboots
- **Google Sign-In** — Authentication via Credential Manager for Sheets sync

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.2 |
| UI | Jetpack Compose (BOM 2025.12) · Material 3 |
| Architecture | MVVM · StateFlow · Repository pattern |
| DI | Hilt 2.59 |
| Database | Room 2.8 (KSP) |
| Async | Coroutines · Flow |
| Charts | Vico 2.4 |
| Sync | Google Sheets API v4 · WorkManager 2.11 |
| Auth | Credential Manager 1.5 |
| Navigation | Navigation Compose 2.8 |

**Min SDK:** 26 (Android 8.0) · **Target SDK:** 35 (Android 15) · **Compile SDK:** 36

## Project Structure

```
com.healthtrend.app/
├── data/
│   ├── model/          # HealthEntry, Severity, TimeSlot, AppSettings
│   ├── local/          # Room database, DAOs, type converters
│   ├── repository/     # HealthEntry & AppSettings repositories
│   ├── auth/           # Google Sign-In (Credential Manager)
│   ├── sync/           # Google Sheets two-way sync (WorkManager)
│   ├── notification/   # Reminders (AlarmManager, BootReceiver)
│   └── export/         # PDF report generation
├── di/                 # Hilt modules
├── ui/
│   ├── navigation/     # Bottom nav: Today, Analytics, Settings
│   ├── daycard/        # Day card screen with pager & severity picker
│   ├── analytics/      # Trend chart, breakdown cards, PDF preview
│   ├── settings/       # App config & reminder settings
│   └── theme/          # Material 3 theme, colors, typography
└── util/               # Time abstraction for testing
```

## Build And Run (Beginner Friendly)

### 1) Prerequisites

- Android Studio (latest stable)
- Android SDK installed (API 26+)
- A physical Android phone or emulator
- A Google account (only needed for Sheets sync)

### 2) Clone and open the project

```bash
git clone https://github.com/sumanthrajaboddu/HealthTrend.git
cd HealthTrend
```

Open this folder in Android Studio and wait for Gradle sync to finish.

### 3) Configure `local.properties`

Android Studio usually creates this file for you automatically. If it does not, create `local.properties` in the project root:

```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
```

This project also reads `google.serverClientId` from the same file for Google Sign-In:

```properties
sdk.dir=/Users/<your-username>/Library/Android/sdk
google.serverClientId=YOUR_WEB_CLIENT_ID.apps.googleusercontent.com
```

### 4) Configure Google OAuth (required for Sheets sync)

If you only want to run the app locally without Google Sheets, you can skip this step.

1. Go to [Google Cloud Console](https://console.cloud.google.com/).
2. Create a project (or select an existing one).
3. Enable APIs:
   - Google Sheets API
   - Google Drive API
4. Configure OAuth consent screen (External/Internal, add app name, test users if needed).
5. Create OAuth Client ID credentials:
   - **Android client** (for app sign-in on device)
   - **Web client** (used as `google.serverClientId`)
6. Copy the Web client ID and set:
   - `google.serverClientId=...` in `local.properties`

### 5) Optional release signing (`key.properties`)

`key.properties` is only needed for a signed **release** build. Debug builds work without it.

Create `key.properties` in project root:

```properties
storeFile=keystore/your-release-key.jks
storePassword=YOUR_STORE_PASSWORD
keyAlias=YOUR_KEY_ALIAS
keyPassword=YOUR_KEY_PASSWORD
```

Key names must be exactly:
- `storeFile`
- `storePassword`
- `keyAlias`
- `keyPassword`

### 6) Build and run

- In Android Studio: select app configuration, then **Run**
- Or with terminal:

```bash
./gradlew assembleDebug
```

### 7) Common errors and fixes

- **`SDK location not found`**: check `sdk.dir` in `local.properties`
- **Google Sign-In fails**: verify `google.serverClientId` is from a **Web** OAuth client
- **Release signing errors**: verify `key.properties` key names and file path to `.jks`

## License

All rights reserved.
