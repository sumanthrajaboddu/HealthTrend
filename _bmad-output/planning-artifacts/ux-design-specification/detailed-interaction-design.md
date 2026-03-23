# Detailed Interaction Design

## Defining Experience

**"Tap today, pick a color, done."**

This is how users will describe HealthTrend. The defining experience is the Day Card inline severity selection — a two-tap interaction that completes in under 3 seconds and feels as natural as tapping a toggle. If this interaction feels perfect, the entire app succeeds. If it feels clunky, nothing else matters.

**The one-sentence pitch:** Open the app, see today's card, tap a time slot, tap a color, it's saved.

## User Mental Model

**How users think about this task:**
Uncle's mental model is a paper chart on the fridge — four rows (Morning, Afternoon, Evening, Night), and he marks how he feels. HealthTrend is that chart, digitized, with the addition of automatic storage and analysis. There is no "app" mental model here — it's a form with four fields that he fills in throughout the day.

**Key mental model implications:**
- **The Day Card is THE app.** Uncle doesn't think in terms of "screens" or "tabs." The Day Card is HealthTrend. Analytics and settings are secondary tools he rarely visits.
- **Time slots are fixed and predictable.** Morning, Afternoon, Evening, Night — always in that order, always four, never changing. This predictability is a feature, not a limitation.
- **Color = feeling.** Green means good, red means bad, yellow and orange are in between. This mapping is universal and requires zero explanation.
- **Today is always the default.** Uncle doesn't think about "which day am I on?" — he expects to see today. Past days are browsable but secondary.

**No existing digital solution to unlearn:** Uncle doesn't have an existing symptom tracking app to compare against. This means no bad habits to break, but also no digital patterns to leverage. The paper chart metaphor is our strongest anchor.

## Success Criteria

| Criteria | Target | Why It Matters |
|----------|--------|---------------|
| Taps to log one entry | 2 (tap slot + tap severity) | Absolute minimum interaction cost |
| Time from app open to entry saved | < 3 seconds | Faster than any competing approach |
| Cognitive load during logging | Zero — no reading, no decisions beyond severity | Uncle should be on autopilot |
| Error recovery | Same flow — tap slot again, pick different color | No undo buttons, no confirmation dialogs |
| Accidental tap recovery | Back/dismiss on severity picker | Tapped wrong slot? Just close the picker |
| Visual confirmation of save | Immediate inline feedback (color fill + subtle affirmation) | Uncle knows it worked without reading text |
| Consistency across states | New entry, edit, and retroactive entry all use identical flow | One pattern to learn, works everywhere |

## Novel UX Patterns

**Pattern type: Established patterns combined in a focused way**

HealthTrend doesn't invent new interaction patterns — it combines proven ones with extreme discipline:

- **Inline expansion** (established) — The severity picker expands within the slot on the Day Card itself, not in a separate modal or bottom sheet. This keeps the user's eyes and attention in one place. Similar to inline editing in list apps, but applied to a card grid.
- **Color-as-data** (established) — Using color to represent severity is a medical/traffic-light convention that requires no learning. The innovation is making color the *primary* data representation, not a supplement to numbers or text.
- **Tap-to-toggle with visual state** (established) — Tapping a filled slot re-opens the picker with the current selection highlighted. This is the same pattern as re-selecting an option in a form dropdown — familiar and predictable.

**The "novel" element is the restraint.** Most health apps add complexity over time. HealthTrend's innovation is refusing to. The interaction on day 1 is identical to day 365.

## Experience Mechanics

**1. INITIATION — Tap a Time Slot**

- Uncle sees today's Day Card with four time slots arranged vertically: Morning, Afternoon, Evening, Night
- The current time-of-day slot has a subtle highlight (gentle glow or tinted border) to guide attention
- Empty slots show a neutral, unfilled state (light gray or outlined) — not alarming, not inviting, just present
- Filled slots show their severity color (green/yellow/orange/red) with a label
- Uncle taps any slot — empty or filled — to interact

**2. INTERACTION — Inline Severity Picker**

- The tapped slot expands inline on the Day Card, revealing four severity options in a horizontal row:
  - 🟢 No Pain | 🟡 Mild | 🟠 Moderate | 🔴 Severe
- Each option is a tappable color circle or rounded button with a text label beneath
- A small back/dismiss control (← arrow or × icon) appears at the edge of the expanded picker, allowing Uncle to close without selecting if he tapped the wrong slot by accident
- If editing an existing entry, the current severity is visually highlighted (ring, border, or check indicator) so Uncle can see what's currently selected
- The rest of the Day Card remains visible but slightly dimmed to focus attention on the picker
- Touch targets are generous (48dp minimum per Material 3) — easy to tap the right color without precision

**3. FEEDBACK — Immediate Confirmation**

- Uncle taps a severity color. The picker collapses *instantly* — no animation delay on the collapse
- The slot fills with the selected color in a brief, satisfying visual transition (color blooms from center or fades in smoothly, ~150ms)
- A subtle affirmation accompanies the fill: a brief haptic pulse (if device supports it) and/or a micro-animation (soft checkmark that fades in and out within 300ms)
- The affirmation is implicit, not interruptive — it confirms "saved" without demanding attention
- If this was the fourth and final slot of the day, an additional subtle "all complete" moment occurs: all four slots glow briefly together, a gentle collective bloom acknowledging the full day is logged
- No toast messages, no "Saved!" text, no snackbars. The color *being there* is the confirmation

**4. COMPLETION — Back to Day Card**

- After the picker collapses, Uncle is right back on the Day Card with the updated slot
- No navigation occurred — he never left the screen
- The Day Card reflects the new state immediately
- If he wants to change it, he taps the same slot again — same flow, same picker, current color highlighted, tap a different one
- If he's done for now, he just... leaves. Closes the app, locks the phone, whatever. There's no "save" button, no "done" action. The entry is persisted the moment he tapped the severity color
- Data syncs to Google Sheets silently in the background

**5. EDGE CASES**

- **Accidental slot tap:** Uncle taps Morning instead of Afternoon. The picker expands for Morning. He taps the back/dismiss control. Picker collapses. No change made. He taps Afternoon instead.
- **Changing an entry:** Uncle taps a filled slot (e.g., orange/Moderate). Picker expands with orange highlighted. He taps yellow/Mild. Picker collapses, slot updates to yellow. No confirmation dialog.
- **Retroactive logging:** Uncle opens the app at 9pm and taps the empty Morning slot. Same picker, same flow, same everything. The app doesn't ask "are you sure you want to log for this morning?" It just works.
- **Browsing past days and editing:** Uncle swipes to yesterday's Day Card. He can tap any slot to edit — same inline picker, same flow. Historical entries are fully editable with the same two-tap pattern.
