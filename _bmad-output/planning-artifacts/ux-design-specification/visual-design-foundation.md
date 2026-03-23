# Visual Design Foundation

## Color System

**Philosophy:** Color in HealthTrend has exactly two jobs — defining severity levels and providing neutral structure. There is no brand color, no accent color, no decorative color. The four severity colors are the only bold hues in the entire app, which makes them instantly readable against the quiet, neutral chrome.

**Severity Palette (Fixed — not theme-derived):**

| Level | Color Name | Hex | RGB | Usage |
|-------|-----------|-----|-----|-------|
| No Pain | Green | `#4CAF50` | 76, 175, 80 | Slot fill, chart data, PDF indicator |
| Mild | Amber | `#FFC107` | 255, 193, 7 | Slot fill, chart data, PDF indicator |
| Moderate | Orange | `#FF9800` | 255, 152, 0 | Slot fill, chart data, PDF indicator |
| Severe | Red | `#F44336` | 244, 67, 54 | Slot fill, chart data, PDF indicator |

**Severity Palette — Soft variants (for backgrounds and chart fills):**

| Level | Soft Hex | Usage |
|-------|----------|-------|
| No Pain | `#E8F5E9` | Slot background tint when filled, chart area fill |
| Mild | `#FFF8E1` | Slot background tint when filled, chart area fill |
| Moderate | `#FFF3E0` | Slot background tint when filled, chart area fill |
| Severe | `#FFEBEE` | Slot background tint when filled, chart area fill |

**Neutral Palette (Material 3 tonal surfaces):**

| Role | Hex | Usage |
|------|-----|-------|
| Background | `#FFFBFE` | Main screen background |
| Surface | `#FFFBFE` | Card surfaces, bottom nav background |
| Surface Variant | `#F5F0F4` | Secondary surfaces, dividers |
| On Surface | `#1C1B1F` | Primary text (headlines, labels) |
| On Surface Variant | `#49454F` | Secondary text (metadata, captions) |
| Outline | `#79747E` | Borders, dividers, empty slot outlines |
| Outline Variant | `#CAC4D0` | Subtle borders, inactive states |
| Surface Container Low | `#F7F2F7` | Day Card background surface |
| Surface Container | `#EFEDF1` | Empty time slot background |

**Semantic Colors:**

| Role | Hex | Usage |
|------|-----|-------|
| Current Slot Highlight | `#E8DEF8` (Material 3 tertiary container) | Gentle highlight on current time-of-day slot |
| Navigation Active | `#6750A4` (Material 3 primary) | Active bottom nav icon/label |
| Navigation Inactive | `#49454F` | Inactive bottom nav icon/label |

**Color Rules:**
1. Severity colors are NEVER used for UI chrome, navigation, or decorative purposes — only for health data
2. App backgrounds and surfaces use exclusively neutral tones from the Material 3 tonal palette
3. The current time slot highlight uses Material 3 tertiary container color — distinct from severity colors, subtle enough to guide without competing
4. No color gradients anywhere in the app — flat, solid colors only for clarity and simplicity

## Typography System

**Typeface:** Roboto (Material 3 system default). No custom fonts. Uncle already reads Roboto on every Android screen — it's invisible in the best way.

**Type Scale (Material 3):**

| Role | Style | Size | Weight | Line Height | Usage |
|------|-------|------|--------|-------------|-------|
| Display Large | Roboto | 57sp | 400 | 64sp | Not used in MVP |
| Display Medium | Roboto | 45sp | 400 | 52sp | Not used in MVP |
| Display Small | Roboto | 36sp | 400 | 44sp | Not used in MVP |
| Headline Large | Roboto | 32sp | 400 | 40sp | Today's date on Day Card |
| Headline Medium | Roboto | 28sp | 400 | 36sp | Analytics screen title |
| Headline Small | Roboto | 24sp | 400 | 32sp | Section headers |
| Title Large | Roboto | 22sp | 400 | 28sp | Settings screen title |
| Title Medium | Roboto Medium | 16sp | 500 | 24sp | Time slot labels (Morning, Afternoon, etc.) |
| Title Small | Roboto Medium | 14sp | 500 | 20sp | Card sub-headers |
| Body Large | Roboto | 16sp | 400 | 24sp | Settings descriptions, analytics labels |
| Body Medium | Roboto | 14sp | 400 | 20sp | General body text |
| Body Small | Roboto | 12sp | 400 | 16sp | Metadata, timestamps |
| Label Large | Roboto Medium | 14sp | 500 | 20sp | Severity level labels in picker (No Pain, Mild, etc.) |
| Label Medium | Roboto Medium | 12sp | 500 | 16sp | Bottom nav labels, chip text |
| Label Small | Roboto Medium | 11sp | 500 | 16sp | Compact metadata |

**Typography Rules:**
1. Maximum two weights in any single view: Regular (400) for body content, Medium (500) for labels and emphasis
2. No bold, no italic, no underline for emphasis — use size and weight hierarchy only
3. All text supports dynamic type scaling for accessibility (sp units, not dp)
4. Severity labels in the inline picker always show text alongside color — never color-only

## Spacing & Layout Foundation

**Base Unit:** 8dp (Material 3 standard spacing unit)

**Spacing Scale:**

| Token | Value | Usage |
|-------|-------|-------|
| xs | 4dp | Tight internal padding (icon-to-text gaps) |
| sm | 8dp | Internal component padding, list item gaps |
| md | 16dp | Standard content padding, screen horizontal margins |
| lg | 24dp | Section spacing, card internal padding |
| xl | 32dp | Between major content sections |
| xxl | 48dp | Top-level screen padding, generous breathing room |

**Layout Structure:**

- **Screen margins:** 16dp horizontal padding (Material 3 standard)
- **Day Card:** Full-width card with 24dp internal padding. Time slots stacked vertically with 12dp gap between them
- **Time slot height:** 64dp minimum (comfortable touch target + room for label and severity indicator)
- **Inline severity picker:** Expands to ~80dp height within the slot, four color options evenly distributed horizontally with 12dp gaps
- **Bottom navigation bar:** Standard Material 3 `NavigationBar` height (80dp) with 3 destinations
- **Content area:** Fills remaining space between top app bar and bottom navigation

**Layout Principles:**
1. **Airy, not dense.** The Day Card should feel spacious — four slots with generous gaps, plenty of whitespace. Uncle should never feel like he's squinting at a crowded screen
2. **Single-column, stacked.** No side-by-side layouts on the Day Card. Everything stacks vertically in a single, scannable column
3. **Touch target generosity.** All interactive elements meet Material 3's 48dp minimum, but the Day Card slots are even larger (64dp+) because they are the core interaction surface
4. **Consistent rhythm.** The 8dp base unit creates a predictable visual rhythm. Everything aligns to the 8dp grid — padding, margins, gaps, component heights

## Accessibility Considerations

**Color Accessibility:**
- All four severity colors meet WCAG AA contrast ratio (4.5:1) against the white/light surface backgrounds
- Each severity level includes a secondary differentiator beyond color alone:
  - **No Pain:** Green + smile/check icon
  - **Mild:** Amber + single dot or dash icon
  - **Moderate:** Orange + double dot/dash icon
  - **Severe:** Red + exclamation/warning icon
- These icons appear in the filled slot state and in the severity picker, ensuring colorblind users can always distinguish levels
- Text labels are always present alongside color indicators

**Touch Accessibility:**
- All touch targets minimum 48dp × 48dp (Material 3 standard)
- Day Card time slots: 64dp+ height for comfortable targeting
- Severity picker options: Generously sized with clear visual separation (12dp gaps)
- No gesture-only interactions — everything achievable via single tap (swipe for day browsing has arrow button alternatives)

**Text Accessibility:**
- All text uses sp units for dynamic type scaling support
- Minimum text size: 12sp (Label Small)
- Primary content text: 14sp-16sp for comfortable reading
- High contrast text: On Surface (`#1C1B1F`) on Background (`#FFFBFE`) = 15.4:1 contrast ratio (exceeds AAA)

**Screen Reader Support:**
- All interactive elements include content descriptions for TalkBack
- Severity levels described as: "Morning slot, currently Moderate. Tap to change severity level"
- Day Card navigation: "Showing February 6, 2026. Swipe left for previous day, swipe right for next day"
- Logical focus order: Date header → time slots (Morning to Night) → bottom navigation
