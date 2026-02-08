# Epic 2: Day Navigation & History

Uncle can swipe between days, use the week strip to jump to any day, and log retroactively for missed slots — all with the exact same tap-to-log flow.

## Story 2.1: Horizontal Day Pager & Date Navigation

**As** Uncle (the patient),
**I want** to swipe left and right on the Day Card to browse previous days and return to today,
**So that** I can review past entries and fill in anything I missed.

**Acceptance Criteria:**

**Given** Uncle is viewing today's Day Card
**When** Uncle swipes left
**Then** yesterday's Day Card slides in smoothly (250ms), the date header updates to yesterday's date, and entries for that day load from Room

**Given** Uncle is viewing yesterday's Day Card
**When** Uncle swipes right
**Then** today's Day Card slides in and the date header updates to today's date

**Given** Uncle is viewing today's Day Card
**When** Uncle swipes right (toward the future)
**Then** the pager does not advance beyond today — either the swipe is blocked or future pages show empty non-interactive cards

**Given** Uncle has swiped to a past date
**When** Uncle taps the "Today" tab in bottom navigation
**Then** the pager animates to today's Day Card and the date header shows today's date

**Given** Uncle swipes to March 3, 2026
**When** the Day Card for that date renders
**Then** it loads entries from Room for March 3 and displays logged slots with severity colors and empty slots with dashes

**Given** TalkBack is enabled
**When** the page changes to a new date
**Then** TalkBack announces the new date context (e.g., "Thursday, February 5")

## Story 2.2: Week Strip Navigation & Data Indicators

**As** Uncle (the patient),
**I want** to see a week strip above the Day Card showing which days have logged entries, and tap any day to jump directly to it,
**So that** I can quickly navigate my logging history and spot gaps.

**Acceptance Criteria:**

**Given** the Day Card screen is displayed
**When** the week strip renders
**Then** it shows 7 days for the current week with abbreviated names, date numbers, today visually highlighted, and the currently viewed day selected
**And** day cells are 44dp x 56dp minimum touch targets

**Given** Uncle has logged entries on Monday and Wednesday
**When** the week strip renders for that week
**Then** Monday and Wednesday show data indicator dots, other days do not

**Given** Uncle taps Thursday in the week strip
**When** the tap is registered
**Then** the HorizontalPager navigates to Thursday's Day Card and the date header updates to Thursday

**Given** Uncle swipes the Day Card pager to a different day
**When** the pager settles on the new day
**Then** the week strip selection updates to highlight that day (bidirectional sync)

**Given** Uncle is viewing a day in the first week of February
**When** Uncle taps the left arrow on the week strip (or navigates before Monday)
**Then** the week strip updates to show the last week of January with correct data indicators

**Given** TalkBack is enabled
**When** the user navigates to a day cell in the week strip
**Then** TalkBack announces the full date, whether it's today, and whether it has data (e.g., "Thursday, February 6, today, has data. Double tap to view.")

**Given** all actions in the week strip
**When** performed via tap
**Then** every action is achievable via single tap — swipe day navigation has the week strip as its tap alternative

---
