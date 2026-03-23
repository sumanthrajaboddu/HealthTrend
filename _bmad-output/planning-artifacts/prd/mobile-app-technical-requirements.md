# Mobile App Technical Requirements

## Platform

- **Framework:** Jetpack Compose with Material 3 (`androidx.compose.material3`)
- **Minimum SDK:** API 26 (Android 8.0) — covers 95%+ of active devices
- **Target SDK:** API 35 (Android 15)
- **Language:** Kotlin
- **Architecture:** MVVM with Repository pattern
- **Distribution:** APK sideload only
- **Orientation:** Portrait locked
- **Theme:** Light only (dark theme post-MVP)

## Device Permissions

| Permission | Purpose | Required |
|-----------|---------|----------|
| `INTERNET` | Google Sheets API sync | Yes |
| `POST_NOTIFICATIONS` (API 33+) | Logging reminders | Yes |
| `SCHEDULE_EXACT_ALARM` | Reliable reminder timing | Yes |
| `RECEIVE_BOOT_COMPLETED` | Reschedule reminders after restart | Yes |
| `VIBRATE` | Haptic feedback on entry save | Optional |

Minimal permission footprint — no camera, location, storage, or contacts.

## Offline Strategy

- **Local Storage:** Room (SQLite) as primary data store
- **Sync:** Fire-and-forget background push to Google Sheets
- **Conflict Resolution:** Local is truth — one-way push, entries save locally first
- **Duration:** Unlimited offline operation
- **Queue:** Pending entries batch-synced on reconnection
- **User Awareness:** Zero — no connectivity indicators, no sync status, no "pending" states

## Notification Strategy

- **Type:** Local scheduled notifications (not server push)
- **Scheduling:** `AlarmManager` with exact alarms
- **Channel:** Single "Logging Reminders" channel
- **Per-Slot Config:** Independent toggle + time for each slot
- **Defaults:** 8:00 AM, 1:00 PM, 6:00 PM, 10:00 PM
- **Behavior:** One notification per slot, no batching/escalation/follow-up
- **Tap Action:** Opens app to today's Day Card
- **Boot Persistence:** Re-registered via `RECEIVE_BOOT_COMPLETED`

## Implementation Notes

- **Google Sheets API:** v4 with OAuth 2.0 (Google Sign-In SDK)
- **PDF Generation:** `PdfDocument` or lightweight library
- **Charting:** MPAndroidChart or Compose-native charting
- **Dependencies:** Minimal third-party — prefer Jetpack libraries
- **Testing:** Manual on 2–3 real devices (see UX spec testing matrix)
- **Performance:** 60fps on mid-range devices, instant local operations

---
