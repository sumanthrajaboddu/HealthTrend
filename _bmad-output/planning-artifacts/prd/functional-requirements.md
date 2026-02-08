# Functional Requirements

## Symptom Logging

- **FR1:** User can view today's Day Card showing four time slots (Morning, Afternoon, Evening, Night) with their current state
- **FR2:** User can tap any time slot to open a severity selector with four options (No Pain, Mild, Moderate, Severe)
- **FR3:** User can select a severity level to record an entry for that time slot
- **FR4:** User can dismiss the severity selector without making a selection
- **FR5:** User can change a previously recorded severity by tapping the filled slot and selecting a different level
- **FR6:** User can log entries retroactively for any past time slot using the same flow as current logging
- **FR7:** System auto-highlights the current time-of-day slot based on device time
- **FR8:** System provides immediate visual confirmation when an entry is saved
- **FR9:** System provides a brief visual acknowledgment (≤300ms animation) when all four slots for a day are completed

## Day Navigation & History

- **FR10:** User can swipe horizontally on the Day Card to browse previous and future days
- **FR11:** User can view a week strip showing the current week with day-level navigation
- **FR12:** User can tap any day in the week strip to navigate directly to that day's card
- **FR13:** System displays a data indicator on week strip days that have logged entries
- **FR14:** User can navigate to previous/next weeks via the week strip
- **FR15:** System always displays the current date context so the user knows which day they're viewing
- **FR16:** User can return to today's Day Card via the bottom navigation "Today" tab

## Data Persistence & Sync

- **FR17:** System persists all entries locally on the device immediately upon selection
- **FR18:** System syncs entries to a configured Google Sheet in the background without user action
- **FR19:** System queues entries when offline and syncs automatically when connectivity is restored
- **FR20:** System writes entries to Google Sheets in the format: Date | Morning | Afternoon | Evening | Night
- **FR21:** System operates identically whether online or offline with no user-visible difference
- **FR22:** System silently handles sync failures with automatic retry without notifying the user

## Analytics & Trends

- **FR23:** User can view a severity trend chart over a selectable date range (1 week, 1 month, 3 months)
- **FR24:** User can view a time-of-day breakdown showing average severity per slot across the selected range
- **FR25:** System displays analytics using the same severity color system as the Day Card
- **FR26:** System defaults the analytics view to the most recent 1-week period

## Report Generation & Sharing

- **FR27:** User can generate a PDF report from the analytics screen for a selected date range
- **FR28:** System includes in the PDF: patient name, date range, trend chart, time-of-day summary, and daily log table
- **FR29:** User can preview the generated PDF before sharing
- **FR30:** User can share the PDF via the Android share sheet (WhatsApp, email, print, etc.)

## Notifications & Reminders

- **FR31:** System sends scheduled notification reminders at configurable times for each time slot
- **FR32:** User can enable or disable reminders globally
- **FR33:** User can enable or disable reminders independently per time slot
- **FR34:** User can configure the reminder time for each time slot
- **FR35:** System provides default reminder times: 8 AM, 1 PM, 6 PM, 10 PM
- **FR36:** Tapping a notification opens the app to today's Day Card
- **FR37:** System re-registers reminders after device restart
- **FR38:** System sends one notification per slot with no follow-up, batching, or escalation

## App Configuration

- **FR39:** User can sign in with a Google account for Google Sheets access
- **FR40:** User can configure the target Google Sheet URL or ID
- **FR41:** User can enter a patient name (used in PDF report headers)
- **FR42:** System auto-saves all settings immediately without requiring a save action
- **FR43:** User can sign out of their Google account
- **FR44:** User can share the Google Sheet link via the Android share sheet

---
