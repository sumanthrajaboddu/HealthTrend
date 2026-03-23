# Design Direction Decision

## Design Directions Explored

Six design directions were generated and presented as interactive HTML mockups (`ux-design-directions.html`):

- **A: Clean Cards** — Classic Material 3 card with icon + label rows, soft tint backgrounds
- **B: Minimal Tiles** — Separated tiles, pill severity badges, maximum whitespace
- **C: Bold Color Blocks** — Full-color filled slots, high visual impact
- **D: Week Strip + Bars** — Week calendar at top, horizontal color bars
- **E: Timeline Cards** — Vertical timeline with progress bar
- **F: Inline Picker Preview** — Core severity picker interaction mockup

## Chosen Direction

**Hybrid: A + B + D — "Calm Tiles with Week Context"**

The final design direction combines carefully selected elements from three directions to create an experience that is spacious, familiar, and emotionally neutral:

**From Direction D — Week Strip Navigation:**
- A horizontal week strip sits at the top of the Day Card screen showing Mon–Sun
- The current day is highlighted with the Material 3 primary color (purple)
- Days that have logged data show a small green indicator dot
- Tapping a day in the strip navigates to that day's card (supplementing swipe navigation)
- The strip provides week-level context at a glance: "how consistent have I been this week?"

**From Direction A — Static Time-of-Day Icons:**
- Each time slot has a fixed icon on the left representing the time of day: ☀ Morning, ☼ Afternoon, 🌙 Evening, ★ Night
- These icons NEVER change regardless of slot state (empty, filled, any severity level)
- The icons are calm, unchanging anchors — they represent *when*, not *how severe*
- This is a deliberate design decision: changing icons to severity indicators (✓, !, !!) would create visual anxiety every time Uncle glances at a filled slot. A "Severe" entry should not be visually alarming on the Day Card — it should be calmly recorded data

**From Direction B — Spacious Tiles with Clean Severity Text:**
- Each time slot is its own separated tile with generous spacing between them
- Empty slots show a neutral "—" indicator with no visual weight
- The current time-of-day slot shows "Tap to log" as a quiet invitation
- Filled slots display severity as a clean text pill badge (e.g., "No Pain", "Mild") with soft background tinting
- Maximum whitespace between tiles creates a calm, uncluttered feeling
- The overall density is low — four tiles with breathing room, not a cramped list

**Combined Slot Anatomy:**

```
┌──────────────────────────────────────────┐
│  ☀  Morning              [No Pain]  🟢  │  ← Filled: icon stays ☀, soft green tint, green pill
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  ☼  Afternoon            Tap to log      │  ← Current: icon stays ☼, subtle purple highlight
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  🌙  Evening                    —        │  ← Empty: icon stays 🌙, neutral gray surface
└──────────────────────────────────────────┘

┌──────────────────────────────────────────┐
│  ★   Night                      —        │  ← Empty: icon stays ★, neutral gray surface
└──────────────────────────────────────────┘
```

## Design Rationale

1. **Static icons eliminate emotional loading.** By keeping icons as time anchors (☀, ☼, 🌙, ★) that never morph into severity indicators, we prevent the Day Card from becoming a "scorecard" that visually judges the user. A severe morning is recorded calmly, not broadcast with an alarm icon.

2. **Week strip adds context without complexity.** Uncle can see at a glance how his week is going (green dots on logged days) without the app calculating streaks or making judgments. The son can also quickly see logging consistency when checking in.

3. **Separated tiles match the calm emotional target.** Each slot has its own space, its own surface. There's no visual crowding, no sense that one bad entry "contaminates" the card. Each time of day is its own calm little container.

4. **Severity as text, not symbol.** Using clean text labels ("No Pain", "Mild", "Moderate", "Severe") with soft color tinting is less visually aggressive than icons or full-color blocks. The words are informative and neutral. The soft background tint provides color-coding without overwhelming.

5. **Familiar patterns from apps Uncle knows.** Bottom tab nav (WhatsApp/YouTube), week strip (calendar apps), tile layout (Settings app) — every component maps to something Uncle has used before.

## Implementation Approach

**Screen Structure (Top to Bottom):**
1. Top App Bar: "Today" label + date subtitle (or specific date when browsing)
2. Week Strip: 7-day horizontal strip with day selection and data indicators
3. Day Card Area: 4 separated time slot tiles, scrollable if needed
4. Bottom Navigation: Today | Analytics | Settings

**Component Mapping to Material 3:**
- Week strip: Custom composable using `Row` + `Surface` chips for each day
- Time slot tiles: `Card` or `ElevatedCard` with custom content layout
- Severity pills: `AssistChip` or custom `Surface` with rounded shape
- Bottom nav: Standard `NavigationBar` with 3 `NavigationBarItem`s
- Inline picker: Custom composable expanding within the Card using `AnimatedVisibility`

**State Variants per Slot Tile:**

| State | Background | Icon | Right Side | Border |
|-------|-----------|------|-----------|--------|
| Empty (not current) | Surface Container (`#EFEDF1`) | Time icon, gray | "—" in gray | None |
| Empty (current slot) | Tertiary Container (`#E8DEF8`) | Time icon, purple tint | "Tap to log" in purple | Subtle purple border |
| Filled — No Pain | Soft green (`#E8F5E9`) | ☀/☼/🌙/★ unchanged | "No Pain" green pill | None |
| Filled — Mild | Soft amber (`#FFF8E1`) | ☀/☼/🌙/★ unchanged | "Mild" amber pill | None |
| Filled — Moderate | Soft orange (`#FFF3E0`) | ☀/☼/🌙/★ unchanged | "Moderate" orange pill | None |
| Filled — Severe | Soft red (`#FFEBEE`) | ☀/☼/🌙/★ unchanged | "Severe" red pill | None |
| Picker expanded | Lighter surface + picker row | Time icon unchanged | Four severity options + dismiss | Purple focus border |
