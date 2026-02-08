# Story 4.1: Notification Scheduler & Reminder Delivery

Status: review

## Story

As Uncle (the patient),
I want to receive a gentle notification at each time slot reminding me to log my symptoms,
So that I build a consistent logging habit without having to remember on my own.

## Acceptance Criteria

1. **Given** app installed and reminders enabled with defaults, **When** 8:00 AM arrives, **Then** notification appears: "Time to log your Morning entry" with app icon.
2. **Given** Uncle taps notification, **When** app opens, **Then** navigates directly to today's Day Card.
3. **Given** Uncle ignores notification, **When** no action taken, **Then** notification sits quietly — no follow-up, no escalation, no repeat.
4. **Given** morning reminder delivered, **When** 1:00 PM arrives, **Then** separate Afternoon notification fires — each slot gets exactly one independent notification.
5. **Given** app requests notification permission on API 33+, **When** permission dialog appears, **Then** only `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, and `RECEIVE_BOOT_COMPLETED` requested — no camera, location, storage, contacts.

## Tasks / Subtasks

- [x] Task 1: Create NotificationScheduler (AC: #1, #4)
  - [x] 1.1 Create `NotificationScheduler` in `data/notification/` package
  - [x] 1.2 Uses `AlarmManager.setExactAndAllowWhileIdle()` for precise timing
  - [x] 1.3 Schedule 4 independent alarms — one per TimeSlot
  - [x] 1.4 Default times from `TimeSlot.defaultReminderTime`: Morning 8:00, Afternoon 13:00, Evening 18:00, Night 22:00
  - [x] 1.5 Each alarm has unique `PendingIntent` with TimeSlot as extra
  - [x] 1.6 If alarm time has passed today, schedule for tomorrow
  - [x] 1.7 Register with Hilt

- [x] Task 2: Create ReminderReceiver (AC: #1, #3)
  - [x] 2.1 Create `ReminderReceiver` extending `BroadcastReceiver` in `data/notification/`
  - [x] 2.2 On receive: extract TimeSlot from intent, show notification
  - [x] 2.3 Reschedule the SAME alarm for next day (repeating daily)
  - [x] 2.4 One notification per slot, no follow-up, no batching
  - [x] 2.5 Register in AndroidManifest

- [x] Task 3: Create notification channel and display (AC: #1, #2)
  - [x] 3.1 Create notification channel "Reminders" with default importance
  - [x] 3.2 Notification content: "Time to log your [TimeSlot.displayName] entry"
  - [x] 3.3 Small icon: app icon
  - [x] 3.4 Tap action: `PendingIntent` opening `MainActivity` with deep link to Day Card for today
  - [x] 3.5 Auto-cancel on tap
  - [x] 3.6 Each slot notification uses unique notification ID (avoid overwriting)

- [x] Task 4: Handle permissions (AC: #5)
  - [x] 4.1 Request `POST_NOTIFICATIONS` on API 33+ (runtime permission)
  - [x] 4.2 Declare `SCHEDULE_EXACT_ALARM` in manifest
  - [x] 4.3 Declare `RECEIVE_BOOT_COMPLETED` in manifest
  - [x] 4.4 NO other permissions — no camera, location, storage, contacts
  - [x] 4.5 Handle permission denial gracefully — reminders just don't fire, no error shown

- [x] Task 5: Initialize reminders on app setup (AC: #1)
  - [x] 5.1 On app launch: if `globalRemindersEnabled` is true, schedule all enabled slot alarms
  - [x] 5.2 Read settings from `AppSettingsRepository` for enabled slots and times
  - [x] 5.3 Only schedule if permission granted

## Dev Notes

### Architecture Compliance

- **AlarmManager** for exact alarms — not WorkManager (reminders need exact timing)
- **BroadcastReceiver** fires notification — lightweight, no coroutine needed
- **NotificationScheduler** is data-layer utility, injected via Hilt
- **No coroutines in Receiver** — fire-and-forget notification display
- **Respect `ANIMATOR_DURATION_SCALE`** — not applicable to notifications but keep in mind for any UI

### Notification Design

- **One notification per slot** — 4 independent alarms, 4 independent notifications
- **No follow-up, no escalation, no repeat** — fire once, sit quietly
- **No bundling/grouping** — each is independent
- **Auto-cancel on tap** — clean notification shade
- **Silent failure** — if permission denied, just don't fire. No error UI.

### Permission Model

| Permission | Where | Purpose |
|-----------|-------|---------|
| `POST_NOTIFICATIONS` | Runtime (API 33+) | Show notifications |
| `SCHEDULE_EXACT_ALARM` | Manifest | Exact alarm timing |
| `RECEIVE_BOOT_COMPLETED` | Manifest | Re-register after restart (Story 4.2) |

### UX Constraints

- Notifications are "gentle" — default importance, no custom sound or vibration pattern
- Notification content uses `TimeSlot.displayName` — NEVER hardcode "Morning"
- Tap opens Day Card for today — not a specific past date
- No gamification, no guilt — missed notification = no consequence

### Project Structure Notes

```
data/notification/
├── NotificationScheduler.kt   # NEW: AlarmManager scheduling
├── ReminderReceiver.kt        # NEW: BroadcastReceiver
└── NotificationHelper.kt      # NEW: channel creation + notification display

di/
└── NotificationModule.kt      # NEW: provides NotificationScheduler
```

### Dependencies on Stories 1.1, 1.2, 3.1

- Requires: TimeSlot enum (1.1), Day Card navigation (1.2), AppSettings with reminder fields (3.1)
- Epic 4 depends on Epic 3 Story 3.1 (Settings with reminder preferences)

### References

- [Source: project-context.md#UX Constraints]
- [Source: requirements-inventory.md#FR31, FR35, FR36, FR38]
- [Source: requirements-inventory.md#NFR11, NFR28]
- [Source: epic-4-notification-reminders.md#Story 4.1]

## Dev Agent Record

### Agent Model Used

Claude claude-4.6-opus (Cursor)

### Debug Log References

- No build environment (gradlew missing) — tests written and verified logically; run full suite before merging.

### Completion Notes List

- **Task 1:** Created `NotificationScheduler` in `data/notification/` with `AlarmManager.setExactAndAllowWhileIdle()`. Pure `calculateNextAlarmDateTime()` function extracted for testability. Each TimeSlot gets unique `PendingIntent` via `getAlarmRequestCode()`. `scheduleAllActive(settings)` reads per-slot enabled flags and times from `AppSettings`. Registered as `@Singleton` with `@Inject` constructor for Hilt.
- **Task 2:** Created `ReminderReceiver` extending `BroadcastReceiver`. Extracts TimeSlot + alarm time from intent extras, shows notification via `NotificationHelper`, reschedules same alarm for next day. No coroutines, no database access — fire-and-forget. Registered in `AndroidManifest.xml` with `exported=false`.
- **Task 3:** Created `NotificationHelper` with "Reminders" channel (default importance). Notification text uses `TimeSlot.displayName` (never hardcoded). Small icon = `R.mipmap.ic_launcher`. Tap PendingIntent opens `MainActivity` (Day Card is start destination). `setAutoCancel(true)`. Each slot uses unique notification ID via `getNotificationId()`.
- **Task 4:** Added `POST_NOTIFICATIONS`, `SCHEDULE_EXACT_ALARM`, `RECEIVE_BOOT_COMPLETED` to manifest. Runtime permission request for `POST_NOTIFICATIONS` on API 33+ in `MainActivity`. Permission denial handled gracefully — no error UI, reminders silently don't fire. No other permissions (no camera, location, storage, contacts).
- **Task 5:** Updated `HealthTrendApplication.onCreate()` to create notification channel and schedule all active reminders. Uses `applicationScope` (SupervisorJob + Dispatchers.IO) for one-shot settings read. Reads `AppSettings` via `AppSettingsRepository.getSettingsOnce()`. Only schedules if `globalRemindersEnabled` is true.
- **Tests:** `NotificationSchedulerTest` — 19 tests covering `calculateNextAlarmDateTime` (future/past/exact/midnight/minutes edge cases), request code uniqueness, notification ID uniqueness, reminder text correctness (all 4 slots), `getEnabledForSlot`/`getTimeForSlot` mapping, `parseAlarmTime` parsing and fallback. `NotificationPermissionsTest` — 3 tests for constant consistency and intent extra key uniqueness.

### File List

- `app/src/main/java/com/healthtrend/app/data/notification/NotificationScheduler.kt` — NEW
- `app/src/main/java/com/healthtrend/app/data/notification/NotificationHelper.kt` — NEW
- `app/src/main/java/com/healthtrend/app/data/notification/ReminderReceiver.kt` — NEW
- `app/src/main/java/com/healthtrend/app/di/NotificationModule.kt` — NEW
- `app/src/main/java/com/healthtrend/app/HealthTrendApplication.kt` — MODIFIED (notification init)
- `app/src/main/java/com/healthtrend/app/MainActivity.kt` — MODIFIED (POST_NOTIFICATIONS runtime permission)
- `app/src/main/AndroidManifest.xml` — MODIFIED (3 permissions + ReminderReceiver registration)
- `app/src/test/java/com/healthtrend/app/data/notification/NotificationSchedulerTest.kt` — NEW (19 tests)
- `app/src/test/java/com/healthtrend/app/data/notification/NotificationPermissionsTest.kt` — NEW (3 tests)
