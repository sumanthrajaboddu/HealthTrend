# Product Scope & Development Strategy

## MVP Strategy

**Approach:** Problem-Solving MVP — deliver the minimum that makes Uncle say "this is useful" on day one. The Day Card + Google Sheets sync is the irreducible nucleus.

**Resource Requirements:** Solo developer (Raja), Kotlin/Jetpack Compose. No backend team (Google Sheets is the backend). No designer (UX spec is complete). Estimated 4–6 weeks.

## Phase 1: MVP

**Core User Journeys Supported:** Daily Logging, Day Browsing, App Setup, Reminder Notifications

| Feature | Justification | Without It? |
|---------|--------------|-------------|
| Day Card with 4 time slots | Core interaction — the entire app | Product doesn't exist |
| Inline severity picker (4 levels) | Core interaction mechanism | Can't log entries |
| Local Room database | Data persistence | Data lost on app close |
| Google Sheets sync (OAuth) | Data portability + caregiver monitoring | Data trapped on device |
| Swipe day navigation + week strip | Browse history, retroactive logging | Can only see today |
| Settings (Sign-In, Sheet URL, name) | One-time setup by Raja | App can't sync |
| Scheduled notifications | Habit formation for Uncle | Uncle forgets to log |
| Analytics screen + charts | Trend visualization for patient and doctor | Can't see patterns |
| PDF export + share | Data-driven doctor conversations | No shareable report |

**Deferrable if time-constrained (Phase 1.5):** Analytics and PDF can ship after the core Day Card + Sync + Reminders, before the first doctor visit.

## Phase 2: Growth (Post-MVP)

- Notes field per entry (e.g., "took medication at 8am")
- Multiple symptom types (track more than one condition)
- Android home screen widget for one-tap logging
- CSV import/export for historical data and backup

## Phase 3: Vision (Future)

- Dark theme with tested severity color variants
- Tap-to-inspect individual analytics data points
- Multi-device awareness (son monitors from same Sheet)
- Play Store distribution

## Out of Scope (MVP)

The following are explicitly excluded from MVP and will not be built in Phase 1:

- Multi-user support or user accounts
- Cloud sync beyond Google Sheets (no Firebase, no custom backend)
- Notes or free-text fields per entry
- Photo attachments
- Play Store distribution (APK sideload only)
- Data import from other apps
- Medication tracking or other health metrics
- Dark theme (light theme only for MVP)

Items may appear in Phase 2/3 roadmap above but are not commitments for initial delivery.

## Risk Mitigation

| Risk | Likelihood | Impact | Mitigation |
|------|-----------|--------|------------|
| Google Sheets API rate limiting | Low | Medium | Local-first architecture; batch sync reduces API calls |
| OAuth token expiration | Medium | Medium | Silent token refresh; graceful fallback to local-only mode |
| Offline sync queue corruption | Low | High | Room transactional writes; retry logic with idempotent operations |
| Notification reliability on OEM Android | Medium | Medium | `AlarmManager` exact alarms; test on target Samsung device; document battery optimization |
| Adoption failure (Uncle stops using it) | Low | High | Extreme simplicity + reminders; sub-5-second interactions |

---
