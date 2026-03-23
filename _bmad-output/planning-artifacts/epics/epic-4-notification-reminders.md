# Epic 4: Notification Reminders

Uncle gets a gentle buzz at each time slot reminding him to log — one notification, no follow-up, no guilt. Reminders survive device restarts.

**Dependencies:** Epic 1 (Day Card — notification tap navigates to today's Day Card) and Epic 3 Story 3.1 (Settings screen — reminder configuration UI lives on the Settings screen, and reminder preferences are persisted via the AppSettings entity created in Story 3.1). Epic 3 must be implemented before Epic 4.

## Story 4.1: Notification Scheduler & Reminder Delivery

**As** Uncle (the patient),
**I want** to receive a gentle notification at each time slot reminding me to log my symptoms,
**So that** I build a consistent logging habit without having to remember on my own.

**Acceptance Criteria:**

**Given** the app is installed and reminders are enabled with defaults
**When** 8:00 AM arrives
**Then** a notification appears: "Time to log your Morning entry" with the app icon

**Given** Uncle taps the notification
**When** the app opens
**Then** it navigates directly to today's Day Card

**Given** Uncle ignores the notification
**When** no action is taken
**Then** the notification sits quietly — no follow-up notification, no escalation, no repeat

**Given** a morning reminder was delivered
**When** 1:00 PM arrives
**Then** a separate Afternoon notification fires — each slot gets exactly one independent notification

**Given** the app requests notification permission on API 33+
**When** the permission dialog appears
**Then** only POST_NOTIFICATIONS, SCHEDULE_EXACT_ALARM, and RECEIVE_BOOT_COMPLETED are requested — no camera, location, storage, or contacts

## Story 4.2: Reminder Configuration & Boot Persistence

**As** Raja (the admin),
**I want** to enable/disable reminders globally and per time slot, configure each slot's reminder time, and have reminders survive device restarts,
**So that** Uncle gets reminded at the right times and the system is reliable without maintenance.

**Acceptance Criteria:**

**Given** Raja opens the Settings screen
**When** the reminder section renders
**Then** it shows a global reminders toggle (enabled by default), four per-slot toggles, and four time pickers with defaults (8 AM, 1 PM, 6 PM, 10 PM)

**Given** Raja disables the global reminders toggle
**When** the toggle is turned off
**Then** all four alarms are cancelled and no reminder notifications fire for any slot
**And** the setting is auto-saved immediately

**Given** Raja disables only the Evening reminder
**When** the toggle is turned off
**Then** only the Evening alarm is cancelled — Morning, Afternoon, and Night continue to fire at their configured times

**Given** Raja changes the Morning reminder time from 8:00 AM to 7:30 AM
**When** the time is saved
**Then** the old 8:00 AM alarm is cancelled and a new alarm is scheduled for 7:30 AM

**Given** Uncle's phone is restarted
**When** the device finishes booting
**Then** BootReceiver fires, NotificationScheduler re-registers all active alarms based on saved AppSettings
**And** reminders continue firing at their configured times without any user action

**Given** TalkBack is enabled
**When** Raja navigates to the Morning reminder toggle
**Then** TalkBack announces "Morning reminder, enabled, 8:00 AM. Double tap to toggle."

---
