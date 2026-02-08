# User Journeys

## Journey 1: Uncle's Daily Ritual (Primary User — Happy Path)

**Persona:** Uncle — a patient managing an ongoing health condition. Comfortable with WhatsApp and YouTube but doesn't explore complex features. Values "open, tap, done."

**Opening Scene:** 8:15 AM. Uncle's phone buzzes: "Time to log your Morning entry." Joints feel stiff but manageable.

**Rising Action:** Uncle taps the notification. HealthTrend opens to today's Day Card — four slots stacked vertically, Morning glowing with a subtle purple highlight. He taps Morning. An inline picker expands: No Pain, Mild, Moderate, Severe. He taps yellow "Mild."

**Climax:** Picker collapses instantly. Morning fills with soft amber tint and "Mild" pill badge. Brief haptic pulse. Total time: 3 seconds. Uncle didn't read a single instruction — colors told him everything.

**Resolution:** Uncle locks his phone. Entry syncs to Google Sheets silently. By evening, all four slots are filled. All tiles glow briefly together — quiet acknowledgment of a complete day.

**Requirements revealed:** Day Card, inline severity picker, notification deep link, background sync, haptic feedback, completion animation.

---

## Journey 2: Uncle's Catch-Up (Primary User — Retroactive Logging)

**Opening Scene:** Thursday evening, 8 PM. Uncle forgot to log this morning and wants to check Tuesday's entries.

**Rising Action:** Today's Day Card shows Morning and Afternoon empty, Evening as "Moderate." He taps empty Morning — same picker, same flow — selects "Severe." Swipes left twice to Tuesday. Week strip highlights Tuesday with green dots on logged days.

**Climax:** Tuesday shows all green — "No Pain" across the board. Uncle smiles. Swipes back to today; header reads "Thursday, Feb 5."

**Resolution:** Retroactive logging and history browsing use the exact same patterns. No "edit mode," no confirmation dialogs. Past and present behave identically.

**Requirements revealed:** Retroactive entry, swipe day navigation, week strip with data indicators, date header, history browsing.

---

## Journey 3: The Doctor Visit (Primary + Secondary User — Value Delivery)

**Secondary Persona:** Dr. Sharma — sees 30+ patients daily. Needs to scan trends in a 10-minute consultation.

**Opening Scene:** Checkup Friday afternoon. Uncle opens Analytics tab.

**Rising Action:** Trend chart shows severity fluctuating between Mild and Moderate, mornings consistently worse. Uncle taps "1 Month" — chart reveals clear improvement since medication change three weeks ago. Time-of-day breakdown confirms: mornings average "Moderate," nights "No Pain."

**Climax:** Uncle taps "Export PDF." Date range pre-selected. Brief spinner, then PDF preview: patient name, trend chart, time-of-day summary, daily log table. Shares via WhatsApp.

**Resolution:** Dr. Sharma: "Mornings are improving since the medication change — evenings are still a problem. Let's adjust." Data-driven decision in 30 seconds.

**Requirements revealed:** Analytics with trend chart, date range selector, time-of-day breakdown, PDF generation, preview, share sheet integration.

---

## Journey 4: Raja Sets Up the App (Admin — One-Time)

**Opening Scene:** Raja sideloads the APK onto Uncle's Samsung phone.

**Rising Action:** App launches to Day Card — no onboarding, no tutorial. Raja opens Settings, signs in with Google, pastes Sheet URL, enters Uncle's name, toggles reminders with defaults (8 AM, 1 PM, 6 PM, 10 PM).

**Climax:** Raja hands phone to Uncle: "Tap the purple one, pick a color." Uncle taps, picks green. Slot fills. "That's it?" That's it.

**Resolution:** Setup under 5 minutes. Uncle is autonomous. Raja monitors via Google Sheet.

**Requirements revealed:** Settings (Google Sign-In, Sheet URL, patient name, reminders), no onboarding, immediate Day Card, default times, auto-save.

---

## Journey 5: The Quiet Notification (Primary User — Reminder)

**Opening Scene:** 1:05 PM. Notification: "Time to log your Afternoon entry."

**Path A:** Uncle taps notification → Day Card → taps Afternoon → picks severity → done in 4 seconds.

**Path B:** Uncle ignores it. Notification sits quietly. No follow-up, no escalation, no guilt. Swipe to dismiss — gone, no consequences.

**Resolution:** One notification per slot. Patient, non-judgmental. Never batched, never nagging.

**Requirements revealed:** Scheduled per-slot notifications, configurable times, deep link to Day Card, single-notification behavior.

---

## Journey 6: Raja Checks In (Passive Caregiver)

**Opening Scene:** Raja wonders if Uncle is still logging.

**Rising Action:** Opens shared Google Sheet — entries through this morning, 3–4 per day consistently. Mostly Mild and Moderate, a few Severe on Monday.

**Resolution:** Data tells the story without a phone call. Google Sheet is a passive caregiving channel.

**Requirements revealed:** Reliable sync, consistent data format (Date | Morning | Afternoon | Evening | Night), data accessible outside app.

---

## Journey Requirements Summary

| Capability Area | Revealed By | Priority |
|----------------|------------|----------|
| Day Card + inline severity picker | J1, J2, J4 | MVP Critical |
| Google Sheets background sync | J1, J6 | MVP Critical |
| Swipe day navigation + week strip | J2 | MVP Critical |
| Notification reminders | J5 | MVP Critical |
| Analytics (trend chart + time-of-day) | J3 | MVP Critical |
| PDF export + share | J3 | MVP Critical |
| Settings (Google Sign-In, Sheet URL, reminders) | J4 | MVP Critical |
| Retroactive/edit entry (same flow) | J2 | MVP Critical |
| Offline queue + invisible sync | J1, J2 | MVP Critical |

---
