# UX Consistency Patterns

## Feedback Patterns

**The core rule: visual state change IS the feedback.** HealthTrend never uses toasts, snackbars, or success messages for normal operations. The interface communicates through color, position, and subtle motion.

### Action Feedback

| Action | Feedback | What the User Sees |
|--------|----------|-------------------|
| Log severity | Instant color fill + haptic pulse | Slot changes from empty/gray to colored with severity pill. 150ms color bloom. Optional haptic. |
| Edit severity | Instant color change | Slot transitions from one severity color to another. Same 150ms bloom. |
| Complete all 4 slots | Subtle collective bloom | All four tiles glow briefly together (~300ms). No text, no popup. |
| Navigate to different day | Smooth slide transition | Day Card slides left/right. Header date updates. Week strip selection updates. |
| Tap week strip day | Immediate card switch | Day Card content updates instantly. Selected day gets purple highlight on strip. |
| Export PDF | Brief loading spinner → preview | `CircularProgressIndicator` during generation (the ONLY loading indicator in the app), then PDF preview appears. |
| Share PDF | Android share sheet opens | Standard OS share sheet — HealthTrend hands off to Android. |
| Sign in with Google | Standard Google sign-in flow | Google SDK handles the UI. On success, email appears in Settings. |
| Save settings | Immediate, no confirmation | Values save as they're entered. No "Save" button. No "Settings saved!" toast. |

### Error Feedback

| Error Scenario | User-Facing Response | Design Rationale |
|----------------|---------------------|-----------------|
| Google Sheets sync fails | Nothing visible. Entry is saved locally. Retry happens silently in background. | "Silence is trust" — Uncle shouldn't know sync exists, let alone that it failed. |
| Google Sign-in fails | Standard Google error shown by SDK (e.g., "Sign-in failed, try again"). Retry button visible. | This is a one-time setup step done by Raja, not Uncle. Standard error handling is acceptable here. |
| Invalid Sheet URL | Red outline on TextField + helper text "Couldn't connect to this sheet" | Only visible in Settings. Uses Material 3 standard error state on `OutlinedTextField`. |
| PDF generation fails | Replace spinner with "Couldn't generate report. Try again." + retry button. | This is rare and happens in a context where the user is actively waiting. Brief, honest error with clear recovery. |
| No data for selected analytics range | Show empty chart area with centered text: "No entries in this date range" | Neutral, informative. Not an error — just a fact. No illustration, no emoji. |
| Network unavailable | Nothing. App works identically offline. | The core design principle. Uncle never sees a connectivity indicator. |

### What HealthTrend NEVER does:

- No toast messages ("Entry saved!", "Synced successfully!")
- No snackbar notifications for normal operations
- No success animations beyond the severity color bloom
- No "undo" snackbar — tap the slot again to change it
- No loading spinners for data entry (local save is instant)
- No "pull to refresh" pattern — there's nothing to refresh

---

## Navigation Patterns

### Screen Architecture

```
┌─────────────────────────┐
│      Top App Bar        │  ← Screen title / date
├─────────────────────────┤
│                         │
│     Screen Content      │  ← Scrollable content area
│                         │
├─────────────────────────┤
│  [Today] [Analytics] [⚙]│  ← Bottom Navigation Bar
└─────────────────────────┘
```

**Bottom Navigation (always visible):**
- 3 tabs: Today (home), Analytics, Settings
- Active tab: Purple icon with Material 3 indicator pill + purple label
- Inactive tabs: Gray icon + gray label
- Tab switching: Content crossfades (Material 3 standard motion)
- Bottom nav is visible on ALL screens — no screen hides it

**Within Day Card (spatial navigation):**
- Horizontal swipe: Navigate between days (left = past, right = future)
- Week strip tap: Jump to specific day
- Today's card is always the default landing
- No "back" button needed — bottom nav handles all screen-level navigation

**Navigation Rules:**
1. The app has exactly 3 screens. No sub-screens, no drill-downs, no nested navigation.
2. The "Today" tab always returns to today's date, even if the user was browsing a different day.
3. Swipe gestures on the Day Card do NOT conflict with system gestures (Android back gesture uses screen edges; day swiping uses the card area).
4. The PDF preview is the only "overlay" — it appears over the Analytics screen and has a clear back/close action.

---

## Empty & Loading States

### Empty States

| Screen | Empty Condition | What the User Sees |
|--------|----------------|-------------------|
| Day Card — no entries today | All 4 slots are empty | Four neutral gray tiles with "—" status. Current slot highlighted with "Tap to log". No illustration, no motivational text. Empty is normal. |
| Day Card — past day, no entries | Browsed to a day with no data | Four neutral gray tiles. No "You didn't log this day" message. Just empty slots. |
| Week strip — no data this week | No entries for any day this week | Day cells with no green dots. Just days and dates. No "Start logging!" prompt. |
| Analytics — no data at all | App just installed, zero entries | Chart area with centered text: "No entries yet". Clean, neutral. No illustration. |
| Analytics — no data in range | Data exists but not in selected range | Chart area with centered text: "No entries in this date range". Suggestion below: "Try a different date range". |
| Settings — not signed in | Google account section | `[Sign in with Google]` button. Clear and standard. |

**Empty state principle:** Empty is a *valid state*, not an error state. The UI should look calm and complete even with zero data. No cartoons, no motivational copy, no "get started" prompts.

### Loading States

| Scenario | Loading Treatment |
|----------|------------------|
| App launch | Day Card appears instantly from local data. No splash screen beyond Android default. |
| Day Card data | Instant from local Room/SQLite database. No loading state ever visible. |
| Analytics charts | Instant from local data. Charts render immediately. No skeleton screens. |
| PDF generation | `CircularProgressIndicator` centered in the preview area. The ONLY spinner in the entire app. Expected duration: 1-3 seconds. |
| Google Sheets sync | Invisible. Background operation. No loading indicator. |
| Google Sign-in | Handled by Google SDK — standard Google loading UI. |

**Loading state principle:** If data is local (which it always is for Day Card and Analytics), there is no loading state. The app appears instant. The only user-visible loading is PDF generation.

---

## Form Patterns (Settings Screen)

**Input style:** Material 3 `OutlinedTextField` for all text inputs.

**Validation approach:** Validate on blur (when the user taps away from the field), not on every keystroke.

| Field | Validation | Error State |
|-------|-----------|-------------|
| Google Sheet URL | Check format on blur. Attempt connection on save. | Red outline + "Couldn't connect to this sheet" helper text |
| Patient Name | No validation — any text is valid. Empty is allowed (PDF header will just omit the name). | None — this field cannot be "wrong" |
| Reminder Times | Validated by `TimePickerDialog` — no invalid input possible | None — picker prevents invalid values |

**Auto-save behavior:** All settings save immediately as they're changed. No "Save" button. No "Unsaved changes" warning. Uncle changes a value, it's saved. This matches the tap-to-act pattern from the Day Card.

**Form principles:**
1. No "Save" button anywhere in Settings — everything auto-saves
2. No "required field" indicators — the app works without any settings configured (except Google Sign-in for sync)
3. Error states only appear for connection failures, never for formatting
4. Settings are "configure once, forget forever" — after initial setup, Uncle may never visit this screen again

---

## Interaction Consistency Rules

**The "One Pattern" Rule:** Every similar action uses the same interaction pattern everywhere:

| Action Type | One Pattern | Used In |
|-------------|------------|---------|
| Select severity | Inline picker with 4 color options + dismiss | Day Card (new entry), Day Card (edit), Day Card (retroactive) |
| Navigate between days | Horizontal swipe OR week strip tap | Day Card screen |
| Navigate between screens | Bottom tab bar tap | All screens |
| Choose a date range | Filter chip row | Analytics screen |
| Toggle a setting | Material 3 Switch | Settings screen (reminders) |
| Enter text | OutlinedTextField with auto-save | Settings screen (URL, name) |
| Share content | Android share sheet | PDF export, Google Sheet link sharing |

**No interaction is ever used for two different purposes.** Horizontal swipe always means "different day." Tab tap always means "different screen." There's no context where the same gesture does different things.

## Animation & Motion Consistency

| Animation | Duration | Easing | Usage |
|-----------|----------|--------|-------|
| Severity picker expand | 200ms | Ease-out | Opening inline picker |
| Severity picker collapse | 0ms (instant) | — | After severity selection |
| Color fill bloom | 150ms | Ease-in-out | Slot filling with severity color |
| Haptic pulse | System default | — | Accompanies color fill (if device supports) |
| All-complete day bloom | 300ms | Ease-in-out | All 4 slots filled — collective glow |
| Day Card swipe | 250ms | Material 3 standard | Swiping between days |
| Screen tab switch | 300ms | Material 3 crossfade | Bottom nav tab switching |
| Picker dismiss (✕) | 150ms | Ease-in | Closing picker without selection |

**Motion principles:**
1. Actions that ADD data (severity selection) are instant — no animation delay before the state change
2. State transitions (color fill, day navigation) use brief, smooth animations for spatial continuity
3. No animation exceeds 300ms — everything feels snappy
4. Animations can be disabled system-wide if Android's "reduce motion" accessibility setting is on
