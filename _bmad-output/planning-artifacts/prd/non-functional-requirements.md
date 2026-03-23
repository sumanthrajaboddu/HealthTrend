# Non-Functional Requirements

## Performance

- **NFR1:** Entry logging completes in under 100ms perceived latency (local save is instant)
- **NFR2:** App launches to Day Card in under 1 second from local data
- **NFR3:** Day Card swipe navigation completes in under 250ms
- **NFR4:** Analytics charts render from local data in under 500ms
- **NFR5:** PDF generation completes in under 3 seconds
- **NFR6:** All animations complete within 300ms maximum
- **NFR7:** App maintains 60fps on mid-range Android devices (Samsung Galaxy A54 class)

## Security & Privacy

- **NFR8:** OAuth tokens stored securely using platform-appropriate secure storage mechanisms
- **NFR9:** No health data transmitted to any service other than the user's configured Google Sheet
- **NFR10:** No third-party analytics, tracking, or crash reporting SDKs
- **NFR11:** App requests only minimum permissions required for functionality
- **NFR12:** Google Sheet access scoped to minimum required API permissions

## Accessibility

- **NFR13:** All interactive elements meet WCAG 2.1 Level AA compliance
- **NFR14:** All touch targets meet 48dp × 48dp minimum (Day Card slots: 64dp+)
- **NFR15:** All text contrast ratios meet WCAG AA minimum (4.5:1 normal text, 3:1 large text)
- **NFR16:** Every severity level distinguishable without color alone (icon + text label + color)
- **NFR17:** Full TalkBack screen reader support with descriptive content labels conveying element purpose, state, and available actions
- **NFR18:** All text uses `sp` units to support dynamic font scaling up to 1.5x
- **NFR19:** App respects Android "Remove animations" accessibility setting
- **NFR20:** All actions achievable via single tap (swipe has tap alternatives)

## Integration

- **NFR21:** Google Sheets API usage stays within quota limits under normal operation
- **NFR22:** OAuth token refresh handled silently without user intervention
- **NFR23:** Google Sheets sync tolerates up to 30 days offline with full queue recovery
- **NFR24:** Sync operations are idempotent — duplicate syncs produce correct data

## Reliability

- **NFR25:** Zero data loss — entries survive app crashes, force stops, and device restarts
- **NFR26:** Data writes are atomic — no partial data corruption on app crash, force stop, or interruption
- **NFR27:** App is fully functional without network connectivity for unlimited duration
- **NFR28:** Reminder notifications persist across device restarts
- **NFR29:** App handles Google API unavailability with no user-visible impact
