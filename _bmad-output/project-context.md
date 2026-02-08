---
project_name: 'HealthTrend'
user_name: 'Raja'
date: '2026-02-07'
status: 'complete'
sections_completed: ['technology_stack', 'language_rules', 'framework_rules', 'testing_rules', 'code_quality', 'workflow_rules', 'critical_rules']
existing_patterns_found: 18
rule_count: 52
optimized_for_llm: true
---

# Project Context for AI Agents

_This file contains critical rules and patterns that AI agents must follow when implementing code in HealthTrend. Focus on unobvious details that agents might otherwise miss. For full architectural details, see `planning-artifacts/architecture.md`._

---

## Technology Stack & Versions

| Core | Version | Notes |
|------|---------|-------|
| AGP | 9.0.0 | Built-in Kotlin support — do NOT apply `org.jetbrains.kotlin.android` plugin. Use `org.jetbrains.kotlin.plugin.compose` instead. |
| Kotlin | 2.0+ | Compose compiler merged into Kotlin — no separate compose-compiler dependency. |
| Compose BOM | 2025.12.00 | ALL Compose library versions managed by BOM — never specify individual Compose versions. |
| Material 3 | via BOM | Dynamic color DISABLED. Fixed severity palette only. |
| Room | 2.8.4 | Use KSP (not kapt). Room DAOs return `Flow<T>` for observable, `suspend` for one-shot. |
| Hilt | Latest stable | `@HiltAndroidApp` on Application. `@HiltViewModel` + `hiltViewModel()` in Compose. |
| WorkManager | 2.11.1 | `CoroutineWorker` for sync. Use `ExistingWorkPolicy.KEEP` for immediate sync. |
| Credential Manager | 1.5.0 | Replaces legacy Google Sign-In SDK. One-time sign-in, persistent forever. |
| Vico | 2.x stable | Use `vico-compose-m3` module. Line charts via `LineCartesianLayer`. |
| Google Sheets API | v4 | Scope: `spreadsheets`. Cell-level reads/writes. Never full-row overwrites. |
| Gradle | 9.1.0+ | Required by AGP 9.0. JDK 17 required. SDK Build Tools 36.0.0. |

## Critical Implementation Rules

### Kotlin & Compose Rules

- **Never use `LiveData`** — always `StateFlow`. Collect with `collectAsStateWithLifecycle()`, never `collectAsState()`.
- **UiState is always a `sealed interface`**, never a data class or open class. Every screen gets one.
- **Composable functions are PascalCase** (`TimeSlotTile`, `SeverityPicker`), not camelCase.
- **Never use `GlobalScope`** — all coroutines scoped to `viewModelScope` or `CoroutineWorker`.
- **Repository functions are always `suspend`** — they never launch their own coroutines. ViewModels call `viewModelScope.launch { }`.
- **Never access DAOs from ViewModels directly.** Always go through Repository.
- **All text sizes in `sp`, all other dimensions in `dp`.** No hardcoded pixel values. No `dp` for text.

### Severity & TimeSlot Model (CRITICAL — Cross-Cutting)

- **`Severity` and `TimeSlot` enums are the SINGLE SOURCE OF TRUTH** for colors, labels, icons, and numeric values.
- **NEVER hardcode severity colors** (`#4CAF50`, etc.) anywhere in UI code — always reference `Severity.color` or `Severity.softColor`.
- **NEVER hardcode time slot labels** ("Morning", etc.) — always reference `TimeSlot.displayName`.
- **Severity numeric values** (0=NO_PAIN, 1=MILD, 2=MODERATE, 3=SEVERE) are used in Room storage, analytics Y-axis, and Sheet comparisons. The enum is the mapping.
- **Severity display names** ("No Pain", "Mild", "Moderate", "Severe") are the exact text written to Google Sheets cells. No variations.

### Two-Way Sync Protocol (CRITICAL — Must Be Exact)

- **Direction:** Push local entries to Sheet, then pull all Sheet data to local. Always push first.
- **Push rule:** Only push entries where `synced = false`. For each, read Sheet timestamp first. Write ONLY if local `updatedAt` > Sheet timestamp.
- **Pull rule:** Read all Sheet rows. For each cell, update local ONLY if Sheet timestamp > local `updatedAt`.
- **NEVER write empty/null to a Sheet cell.** Only write cells that have actual severity data.
- **Cell-level writes only** — never overwrite an entire row. Use specific cell ranges (e.g., `B5` for Morning of row 5).
- **Date is the row key** in Column A. Format: `YYYY-MM-DD` (ISO 8601).
- **Timestamps in Columns F–I** are epoch milliseconds (Long). These are sync metadata, not display data.
- **Conflict resolution:** Newest timestamp wins. Fully automatic. No user prompt.
- **Sync errors are ALWAYS SILENT.** WorkManager exponential backoff handles retries. Zero user-facing error states.

### Google Sign-In (Credential Manager)

- **One sign-in, persistent forever.** User NEVER sees a re-auth prompt after initial setup.
- **Token refresh is automatic and silent** via Credential Manager.
- **If token refresh fails** (e.g., password changed), show a single "Please sign in again" in Settings — not a blocking modal.
- **OAuth scope:** `https://www.googleapis.com/auth/spreadsheets` — covers both read and write.

### UX Constraints (CRITICAL — Agents Will Try to Violate These)

- **NO toast messages, NO snackbars, NO "Saved!" confirmations.** The color fill IS the confirmation.
- **NO sync indicators, NO connectivity banners, NO "last synced" timestamps.** Silence is trust.
- **NO loading spinners** except for PDF generation (the ONLY spinner in the entire app).
- **NO confirmation dialogs.** "Are you sure?" does not exist in this app. Tap = done. To undo, tap again.
- **NO onboarding, NO tutorials, NO tooltips, NO "what's new" prompts.** Day Card is the first and only screen on launch.
- **NO gamification.** No streaks, no badges, no "Great job!" messages. Empty slots are neutral, not failures.
- **Empty states are calm and neutral.** No illustrations, no motivational text. Just empty tiles with "—".
- **Severity picker collapses INSTANTLY (0ms) after selection.** No exit animation on the collapse — only on the expand (200ms).

### Accessibility (WCAG AA — Non-Negotiable)

- **Every custom composable needs `Modifier.semantics { }`** with descriptive content labels.
- **Triple encoding for severity:** color + text label + icon. NEVER color alone.
- **All touch targets minimum 48dp x 48dp.** Day Card tiles are 64dp+ height.
- **Check `Settings.Global.ANIMATOR_DURATION_SCALE`** — if system animations disabled, skip ALL custom animations.
- **TalkBack announcements must convey purpose, state, and action.** Example: "Morning, currently Mild. Tap to change severity."
- **Swipe navigation MUST have tap alternative** (week strip + arrow buttons).

### Animation Constants (All in `AnimationSpec.kt`)

- **No animation exceeds 300ms.** This is a hard cap.
- **Picker expand: 200ms ease-out. Picker collapse on selection: 0ms (instant). Color fill: 150ms. Day swipe: 250ms.**
- **Never use inline duration values.** Always reference constants from `AnimationSpec.kt`.

### Room Database Rules

- **Table names: `snake_case`, plural** (`health_entries`, `app_settings`).
- **Column names: `snake_case`** (`time_slot`, `updated_at`, `is_synced`).
- **Composite unique constraint** on `(date, time_slot)` — one entry per slot per day.
- **Room auto-migration** for schema changes. Destructive migration + re-sync from Sheet is acceptable fallback.

### Project Structure Rules

- **One ViewModel per screen.** No shared ViewModels.
- **Screen-specific composables live in their screen's package** (`ui/daycard/TimeSlotTile.kt`).
- **Shared composables (used by 2+ screens) live in `ui/components/`** only.
- **No screen imports composables from another screen's package.**
- **`ui/theme/` is the single source** for colors, typography, shapes, and animation specs.

### Anti-Patterns (NEVER Do These)

| Anti-Pattern | Correct Pattern |
|-------------|----------------|
| `LiveData` in ViewModel | `StateFlow` + `collectAsStateWithLifecycle()` |
| `mutableStateOf()` in ViewModel | `MutableStateFlow` (private) + `StateFlow` (public) |
| DAO called from ViewModel | Call Repository, which calls DAO |
| Hardcoded color hex in composable | Reference `Severity.color` or theme token |
| Full-row write to Google Sheet | Cell-level write with timestamp check |
| Toast/Snackbar after save | Color fill bloom is the only feedback |
| `kapt` for Room compiler | `ksp` — kapt is deprecated |
| `org.jetbrains.kotlin.android` plugin | Not needed with AGP 9.0 |
| Sync error shown to user | Silent retry via WorkManager backoff |
| `collectAsState()` | `collectAsStateWithLifecycle()` |

---

## Usage Guidelines

**For AI Agents:**

- Read this file before implementing any code in HealthTrend
- Follow ALL rules exactly as documented — no exceptions for "readability" or "preference"
- When in doubt, prefer the more restrictive option
- For full architectural details (project structure, data flow, component boundaries), see `planning-artifacts/architecture.md`
- For UX/interaction details, see `planning-artifacts/ux-design-specification/`
- For functional and non-functional requirements, see `planning-artifacts/prd.md`

**For Humans:**

- Keep this file lean and focused on agent needs
- Update when technology stack or patterns change
- Remove rules that become obvious over time
- Add new rules when agents make recurring mistakes

Last Updated: 2026-02-07
