# Design System Foundation

## Design System Choice

**Material Design 3 (Material You)** — Android's native design system, implemented via Jetpack Compose Material 3 components.

This is the natural and only logical choice for HealthTrend: a native Android app designed to feel like it belongs on the device, built by a solo developer, for users already fluent in Material Design patterns through WhatsApp, YouTube, and the stock Android experience.

## Rationale for Selection

1. **Zero learning curve for the user** — Uncle interacts with Material Design components every day. Bottom navigation bars, cards, tap targets, system typography — these are patterns his muscle memory already knows. HealthTrend inherits all of that familiarity for free.
2. **Native integration with Jetpack Compose** — Material 3 components ship as first-party Android libraries. No third-party dependencies, no version management overhead, no risk of library abandonment. `MaterialTheme`, `Card`, `NavigationBar`, `Surface` — all available out of the box.
3. **Built-in accessibility** — Material 3 enforces minimum touch target sizes (48dp), supports dynamic type scaling, provides semantic color roles for contrast compliance, and integrates with TalkBack screen reader. These aren't features to add later — they're defaults.
4. **"No personality, only presence" alignment** — Material 3 is deliberately invisible as a design system. It doesn't brand the app — it makes the app feel like Android. This aligns perfectly with the emotional design principle that HealthTrend should feel like it came with the phone.
5. **Solo developer efficiency** — Pre-built, well-documented components with extensive Android developer community support. Reduces design and implementation time significantly for a one-person team.

## Implementation Approach

**Framework:** Jetpack Compose with Material 3 (`androidx.compose.material3`)

**Component Strategy:**
- Use Material 3 standard components wherever possible: `NavigationBar`, `Card`, `Surface`, `TopAppBar`, `FilledTonalButton`
- Custom components only where Material 3 doesn't provide a direct match: the Day Card time slot grid, severity level selector, and analytics charts
- Follow Material 3 layout guidelines: screen-level scaffolding with `Scaffold`, consistent padding with `WindowInsets`, responsive sizing

**Typography:** Material 3 type scale using system default font (Roboto). No custom fonts. Headlines for dates, body for labels, label for metadata.

**Elevation & Surfaces:** Material 3 tonal elevation system. Day Card uses a `Card` surface with subtle elevation. Bottom navigation bar uses standard `NavigationBar` elevation. Minimal layering to keep the interface flat and scannable.

## Customization Strategy

**Color System — Fixed severity palette (overriding Material You dynamic color):**

The single most important customization is locking the color system. Material You normally derives colors from the device wallpaper, but HealthTrend's severity colors must be fixed for medical data clarity:

| Severity Level | Color | Hex (approximate) | Material 3 Role |
|---------------|-------|-------------------|----------------|
| No Pain | Green | `#4CAF50` | Custom — fixed, not theme-derived |
| Mild | Yellow | `#FFC107` | Custom — fixed, not theme-derived |
| Moderate | Orange | `#FF9800` | Custom — fixed, not theme-derived |
| Severe | Red | `#F44336` | Custom — fixed, not theme-derived |

**App chrome colors** (navigation bar, backgrounds, surfaces) will use Material 3's neutral tonal palette — light grays and whites that let the severity colors stand out as the only bold color in the interface.

**Accessibility consideration:** Each severity level will include a secondary indicator (icon or pattern) alongside color to ensure colorblind users can distinguish levels. Labels are always present as text backup.

**Dark theme:** Not planned for MVP. The app uses a light theme only to maintain consistent severity color perception and simplify development. Dark theme can be added in a future iteration with carefully tested severity color variants.

**Animation & Motion:** Material 3 motion system for transitions (shared element transitions between Day Card and analytics, smooth bottom nav switching). Completion micro-animation for "all four logged" moment uses a lightweight custom animation — subtle color bloom and optional haptic feedback, kept under 300ms to feel snappy, not decorative.
