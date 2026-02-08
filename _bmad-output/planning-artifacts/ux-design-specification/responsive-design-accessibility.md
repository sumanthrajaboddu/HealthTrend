# Responsive Design & Accessibility

## Responsive Strategy

**Platform:** Native Android phone app only (no tablet, desktop, or web targets for MVP).

**Approach:** Single-column, fluid layout that adapts to any phone screen width. No breakpoints, no layout switching. The same layout stretches and compresses gracefully across the full range of Android phone sizes.

**Screen Size Range:**

| Category | Width Range | Example Devices | Considerations |
|----------|-----------|-----------------|---------------|
| Small phone | 320dp – 360dp | Older budget devices, SE-class phones | Ensure all 4 time slot tiles + week strip fit without scrolling. Severity picker options may need tighter spacing. |
| Standard phone | 360dp – 400dp | Most mid-range Android phones | Primary design target. All layouts designed for this range. |
| Large phone | 400dp – 430dp | Samsung Galaxy S series, Pixel Pro | Extra horizontal space absorbed by tile padding. Layout doesn't widen — it breathes more. |
| Extra-large phone | 430dp+ | Samsung Ultra, foldable outer screen | Same layout with more generous spacing. Content max-width not needed — single column stays centered. |

**Fluid Layout Rules:**
1. Screen horizontal margins: fixed 16dp on all sizes
2. Time slot tiles: full width minus margins — they stretch horizontally, not vertically
3. Week strip: 7 day cells distribute evenly across available width
4. Severity picker options: evenly distributed within tile width with minimum 8dp gaps
5. Bottom navigation: standard Material 3 `NavigationBar` handles its own width distribution
6. No horizontal scrolling anywhere in the app — ever

**Orientation:** Portrait only. Landscape is not supported in MVP. The Day Card layout is inherently vertical (stacked tiles), and Uncle will always hold his phone in portrait. Locking orientation simplifies development and prevents layout confusion.

## Font Scaling & Dynamic Type

**Critical consideration:** Uncle (or any Android user) may have their system font size set to Large or Extra Large in Android Settings. HealthTrend must handle this gracefully.

**Strategy:** All text uses `sp` units (scale-independent pixels), which automatically scale with the system font size setting. The layout is designed to accommodate up to 1.5x font scaling without breaking.

| Font Scale | Layout Impact | Handling |
|-----------|---------------|---------|
| Default (1.0x) | Primary design target | Everything fits perfectly |
| Large (1.15x) | Slightly larger text | Layout absorbs naturally — tile height grows slightly, text wraps if needed |
| Extra Large (1.3x) | Noticeably larger text | Severity pill text may wrap to two lines — pill auto-sizes. Week strip day labels may abbreviate (Mon → M) |
| Maximum (1.5x) | Significantly larger text | Day Card area becomes scrollable if tiles exceed screen height. All content still accessible via scroll. Week strip uses single-letter abbreviations |

**Rules:**
1. Never use fixed `dp` for text — always `sp`
2. Tile minimum height grows with font scale (content determines height, not a fixed value)
3. Severity pill badges use auto-width based on text content
4. If the Day Card tiles can't all fit on screen at large font scales, the area scrolls vertically — this is acceptable

## Accessibility Strategy

**Compliance Target: WCAG 2.1 Level AA**

This is the industry standard for good accessibility and is achievable with Material 3 as the foundation. Level AA ensures HealthTrend is usable by people with visual impairments, motor difficulties, and cognitive considerations — all relevant for a health app whose primary user is older.

### Color & Visual Accessibility

| Requirement | WCAG Criteria | HealthTrend Implementation |
|-------------|--------------|---------------------------|
| Text contrast | 4.5:1 minimum (AA) | On Surface (`#1C1B1F`) on Background (`#FFFBFE`) = 15.4:1 — exceeds AAA |
| Large text contrast | 3:1 minimum (AA) | All headline text exceeds 3:1 |
| Non-text contrast | 3:1 minimum (AA) | Severity colors on white backgrounds all exceed 3:1 |
| Color not sole indicator | 1.4.1 | Every severity level has: color + text label + unique icon. Never color alone. |
| Focus visible | 2.4.7 | Material 3 provides default focus indicators. Custom components include visible focus rings. |

**Colorblind safety matrix:**

| Severity | Color | Icon | Text | Distinguishable in all color blindness types? |
|----------|-------|------|------|----------------------------------------------|
| No Pain | Green `#4CAF50` | ✓ (check) | "No Pain" | Yes — icon + text differentiate even if green/red confused |
| Mild | Amber `#FFC107` | ~ (tilde/dash) | "Mild" | Yes — amber is distinct from green/red in most types |
| Moderate | Orange `#FF9800` | ! (single) | "Moderate" | Yes — icon differentiates from Severe even if orange/red confused |
| Severe | Red `#F44336` | !! (double) | "Severe" | Yes — double icon + text differentiates from Moderate |

### Touch & Motor Accessibility

| Requirement | Standard | HealthTrend Implementation |
|-------------|---------|---------------------------|
| Minimum touch target | 48dp × 48dp (Material 3) | Time slot tiles: 64dp+ height. Severity picker circles: 48dp+. Week strip cells: 44dp × 56dp minimum. |
| Touch target spacing | 8dp minimum gap | 12dp gap between tiles, 12dp between picker options |
| No complex gestures required | WCAG 2.5.1 | All actions achievable via single tap. Swipe for day navigation has week strip tap alternative. |
| No time-limited actions | WCAG 2.2.1 | No timeouts, no timed interactions anywhere. Picker stays open until user acts. |
| Pointer cancellation | WCAG 2.5.2 | Severity selection registers on tap-up (release), not tap-down. User can drag finger away to cancel. |

### Screen Reader (TalkBack) Accessibility

**Content descriptions for all interactive elements:**

| Element | TalkBack Announcement |
|---------|----------------------|
| Empty slot (not current) | "Morning, not logged. Double tap to log severity." |
| Empty slot (current) | "Afternoon, current time slot, not logged. Double tap to log severity." |
| Filled slot | "Evening, Moderate. Double tap to change severity." |
| Severity picker option | "No Pain. Double tap to select." |
| Severity picker (editing) | "Moderate, currently selected. Double tap to keep, or choose a different severity." |
| Dismiss picker | "Close severity picker. Double tap to close." |
| Week strip day | "Thursday, February 6, today, has data. Double tap to view." |
| Week strip day (no data) | "Wednesday, February 5, no data. Double tap to view." |
| Bottom nav tab | "Analytics tab. Double tap to switch." |
| Analytics chart | "Severity trend over 7 days. Average: Mild. Trend: improving." |
| Export button | "Export PDF report. Double tap to generate." |

**Focus order (logical reading order):**
1. Top App Bar (date)
2. Week strip (left to right)
3. Time slot tiles (Morning → Afternoon → Evening → Night)
4. Bottom navigation (Today → Analytics → Settings)

**When severity picker is open:**
- Focus moves into the picker
- Tab/swipe navigates between severity options and dismiss button
- Selecting an option or dismissing returns focus to the tile

### Cognitive Accessibility

| Consideration | Implementation |
|--------------|---------------|
| Consistent navigation | Bottom nav always present, always in same order, always same behavior |
| Predictable behavior | Same action → same result everywhere. No context-dependent behavior changes. |
| Simple language | "No Pain", "Mild", "Moderate", "Severe" — plain, clinical language. No jargon. |
| No memory demands | Current state always visible. No hidden information. No need to remember what was entered. |
| Error prevention | Severity can be changed anytime — no permanent actions. No "are you sure?" because nothing is irreversible. |
| Clear feedback | Color fill confirms action. No ambiguous states. |

## Testing Strategy

**Device Testing Matrix:**

| Device Category | Test Devices | Priority |
|----------------|-------------|----------|
| Primary target | Mid-range Android phone (e.g., Samsung Galaxy A54 or similar) | Must pass |
| Small screen | Older/smaller phone (5.0"–5.5" screen) | Must pass |
| Large screen | Flagship phone (6.5"+ screen, e.g., Pixel 8 Pro) | Must pass |
| Large font scale | Any device with system font set to Maximum | Must pass |

**Accessibility Testing Checklist:**

- [ ] TalkBack: navigate entire app using only screen reader + gestures
- [ ] TalkBack: complete a full logging flow (open → log severity → verify)
- [ ] TalkBack: navigate analytics and trigger PDF export
- [ ] Font scaling: test at Default, Large, Extra Large, and Maximum
- [ ] Color blindness: simulate Protanopia, Deuteranopia, Tritanopia using Android developer options
- [ ] Touch targets: verify all interactive elements meet 48dp minimum
- [ ] Reduce motion: verify app respects Android "Remove animations" setting
- [ ] Keyboard/D-pad: ensure all actions are reachable (for switch access users)

**Testing approach:** Manual testing on 2-3 real devices during development. No automated accessibility testing framework required for a personal app — but TalkBack walk-through is mandatory before handing to Uncle.

## Implementation Guidelines

**For the developer (Raja):**

1. **Always use `sp` for text, `dp` for everything else.** No exceptions. No hardcoded pixel values.
2. **Use `Modifier.semantics {}` for all custom composables.** Every custom component needs TalkBack content descriptions.
3. **Test with TalkBack enabled** at least once per screen during development. It catches issues automated tools miss.
4. **Lock orientation to portrait** via `android:screenOrientation="portrait"` in the manifest.
5. **Respect `Settings.Global.ANIMATOR_DURATION_SCALE`.** If the user has animations disabled, skip all custom animations (color bloom, picker expand, etc.).
6. **Use Material 3 color roles properly.** Don't hardcode colors — reference theme tokens so that contrast ratios are maintained automatically.
7. **Test the "grandmother test":** Hand the phone to someone unfamiliar with the app. Can they log an entry within 60 seconds with zero guidance? If yes, accessibility is likely good enough.
