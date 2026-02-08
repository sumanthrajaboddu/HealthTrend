# Core User Experience

## Defining Experience

The defining experience of HealthTrend is the **Day Card tap-to-log interaction**. Everything in the app exists to support this single, repeatable moment: Uncle opens the app, sees today's card with four time slots, taps the relevant one, selects a severity level, and he's done. This interaction must complete in under 5 seconds and feel so natural it becomes muscle memory within the first day of use.

The Day Card is always the landing screen. Today's date, four slots, current status at a glance. No navigation required to reach the primary action. The current time-of-day slot is subtly highlighted to guide attention without being pushy.

Retroactive logging (filling in a missed time slot) should feel identical to real-time logging — same screen, same tap, same flow. No special "edit mode" or confirmation dialogs. Tap the empty Morning slot at 9pm, pick severity, done. The app doesn't judge when you log.

## Platform Strategy

- **Platform:** Native Android application (APK sideload, no Play Store)
- **Interaction model:** Touch-first, optimized for one-handed phone use
- **Offline capability:** Full offline functionality with invisible background sync to Google Sheets when connectivity returns. No loading states, no sync indicators beyond a subtle reassurance icon
- **Device considerations:** Must perform smoothly on mid-range Android devices. Animations and micro-interactions should be lightweight (Lottie or native Android transitions) to avoid any performance overhead
- **Multi-device awareness:** The son may install the app on his own phone and monitor from the same Google Sheet. The app should work cleanly as a read-only viewer when browsing another device's data via the shared Sheet, though this is not a primary design target for MVP

## Effortless Interactions

1. **Tap-to-log (the core loop):** Open app → see today → tap slot → pick severity → done. Zero navigation, zero confirmation dialogs, zero typing. The entire flow is taps only.
2. **Day browsing:** Swiping left/right through days should feel like flipping pages in a calendar. Smooth, continuous, with clear date context so Uncle always knows "where" he is in time.
3. **Auto-highlight current slot:** The app knows what time of day it is. The relevant slot glows or pulses gently — not aggressively, just enough to say "this is probably what you're here for."
4. **Invisible sync:** Data flows to Google Sheets silently. Uncle never thinks about saving, syncing, or connectivity. If offline, entries queue invisibly and sync when back online.
5. **Retroactive entry:** Logging a missed slot feels exactly like logging a current one. No friction, no warnings, no different flow.

## Critical Success Moments

1. **First launch → first entry (< 60 seconds):** After initial setup (done by Raja), Uncle's first experience is: open app, see today's card, tap a slot, pick green/yellow/orange/red. If he completes this in under a minute with zero confusion, the app has succeeded at first contact.
2. **The "all four logged" completion moment:** When all four slots for the day are filled, a subtle micro-animation (soft checkmark, gentle color bloom, brief haptic feedback) creates a small moment of satisfaction — the Duolingo effect. Not flashy, not loud, just a quiet "well done" that rewards the habit.
3. **The doctor visit:** Uncle opens the analytics screen, taps export, shares a PDF that the doctor can actually read and react to. If the doctor says something meaningful based on the data, the entire app's existence is validated.
4. **The son's check-in:** The son opens the Google Sheet (or his own app install) and sees that Uncle has been logging consistently. He doesn't need to call and ask "are you tracking?" — the data speaks for itself. This is passive caregiving through shared data.

## Experience Principles

1. **"Open and done" — The app respects Uncle's time.** Every interaction should feel like it takes less time than expected. If something can be one tap instead of two, it's one tap.
2. **"Show, don't ask" — The interface communicates through color, position, and gentle animation, not text or prompts.** The Day Card's state is immediately readable at a glance. Severity colors tell the story. The highlighted slot says "you're probably here for this."
3. **"Quiet delight" — Small, lightweight moments of satisfaction reward the habit without demanding attention.** Micro-animations on completion, smooth transitions between days, gentle haptic feedback. Never intrusive, always earned.
4. **"Invisible infrastructure" — Sync, storage, and data flow are never the user's problem.** Google Sheets integration, offline queuing, and data persistence all happen silently. Uncle only sees his Day Card and his colors.
5. **"Data as care" — The analytics and export features transform personal tracking into a shared health conversation.** The son monitors quietly, the doctor sees trends clearly, and Uncle feels supported without being surveilled.
