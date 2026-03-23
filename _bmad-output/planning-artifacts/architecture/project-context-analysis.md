# Project Context Analysis

## Requirements Overview

**Functional Requirements (44 FRs across 7 categories):**

| Category | FR Count | Architectural Significance |
|----------|----------|---------------------------|
| Symptom Logging (FR1–FR9) | 9 | Core interaction — inline expansion picker, instant local save, haptic feedback, completion animation |
| Day Navigation (FR10–FR16) | 7 | Horizontal swipe pager, week strip with data indicators, date-driven content loading |
| Data Persistence & Sync (FR17–FR22) | 6 | Local-first Room DB, one-way push to Google Sheets, invisible offline queue, idempotent sync |
| Analytics & Trends (FR23–FR26) | 4 | Chart rendering from local data, date range filtering, consistent severity color mapping |
| Report Generation (FR27–FR30) | 4 | PDF generation with embedded charts, preview, Android share sheet integration |
| Notifications (FR31–FR38) | 8 | AlarmManager exact alarms, per-slot scheduling, boot persistence, notification channel |
| App Configuration (FR39–FR44) | 6 | Google OAuth Sign-In, Sheet URL config, patient name, auto-save settings |

**Non-Functional Requirements (29 NFRs across 5 categories):**

| Category | Key Constraints | Architectural Impact |
|----------|----------------|---------------------|
| Performance (NFR1–NFR7) | <100ms entry save, <1s launch, <250ms swipe, <500ms chart render, <3s PDF, 60fps | Local-first eliminates network latency from critical path. All UI operations against Room DB. |
| Security (NFR8–NFR12) | Secure OAuth token storage, no third-party SDKs, minimal permissions, scoped API access | Platform keystore for tokens. No analytics/tracking SDKs. Google Sheets API scoped to spreadsheets.readonly + spreadsheets write. |
| Accessibility (NFR13–NFR20) | WCAG AA, 48dp+ touch targets, TalkBack, sp units, colorblind-safe, respect reduce-motion | Built into every custom component from start. Severity levels use icon + text + color triple encoding. |
| Integration (NFR21–NFR24) | Google Sheets API quota compliance, silent token refresh, 30-day offline tolerance, idempotent sync | Batch sync reduces API calls. Token refresh via Google Sign-In SDK. Sync queue with timestamps for idempotency. |
| Reliability (NFR25–NFR29) | Zero data loss, atomic writes, unlimited offline, boot-persistent reminders, API failure tolerance | Room transactional writes. WorkManager for sync. AlarmManager + BootReceiver for notifications. |

**Scale & Complexity:**

- Primary domain: Native Android Mobile
- Complexity level: Low
- Estimated architectural components: ~12 (3 screens + 7 custom UI components + sync layer + notification scheduler)
- Data volume: ~4 entries/day, ~1,460/year — trivial for SQLite
- External integrations: 1 (Google Sheets API v4)
- Users: 1 (single-user app)

## Technical Constraints & Dependencies

| Constraint | Source | Impact |
|-----------|--------|--------|
| Kotlin + Jetpack Compose + Material 3 | PRD | Framework and UI toolkit locked |
| Min SDK 26, Target SDK 35 | PRD | API surface defined; exact alarms available from API 19 |
| Portrait-only, Light theme only | PRD + UX | Simplifies layout; single color theme to maintain |
| APK sideload distribution | PRD | No Play Store compliance needed; no app signing requirements beyond debug |
| Google Sheets API v4 + OAuth 2.0 | PRD | Single external dependency; requires Google Play Services on device |
| MVVM + Repository pattern | PRD | Architecture pattern pre-selected |
| No third-party analytics or crash reporting | NFR10 | No Firebase Crashlytics, no Mixpanel — manual observation only |
| Fixed severity colors (no Material You dynamic) | UX | Custom theme overrides dynamic color extraction |
| Room/SQLite local database | PRD | Built into Android — zero-cost, zero-infrastructure local storage on the device |

## Cross-Cutting Concerns Identified

| Concern | Affected Components | Resolution Strategy |
|---------|-------------------|-------------------|
| Severity data model & color mapping | Day Card, Analytics, PDF, Google Sheets sync | Single source of truth: sealed class/enum with color, label, icon, and numeric value mappings |
| Offline capability | All data operations, sync, UI state | Room as primary store; all reads/writes local; sync is background-only fire-and-forget |
| Accessibility (WCAG AA) | All 7 custom components + navigation | Semantics annotations on every composable; triple-encoding (color + icon + text) for severity |
| Animation consistency | Picker, color fill, day swipe, tab switch | Centralized animation spec (duration/easing constants); respect system reduce-motion setting |
| Date/time awareness | Day Card highlight, notifications, analytics grouping | Single time utility for slot-to-time mapping, consistent date formatting |
