# Epic 5: Analytics & Trends

Uncle opens the Analytics tab and sees how his symptoms have changed over time — trend chart, time-of-day breakdown, date range selection.

## Story 5.1: Severity Trend Chart & Date Range Selection

**As** Uncle (the patient),
**I want** to see a line chart showing how my symptom severity has changed over time, with selectable date ranges,
**So that** I can spot patterns and see whether I'm improving or getting worse.

**Acceptance Criteria:**

**Given** Uncle navigates to the Analytics tab
**When** the screen renders
**Then** a trend line chart displays severity data for the most recent 1-week period by default
**And** three date range chips are visible: 1 Week (selected), 1 Month, 3 Months

**Given** Uncle has logged entries for the past 7 days
**When** the 1 Week chart renders
**Then** the chart shows a line connecting daily severity points, using severity colors, with Y-axis labeled No Pain (0) through Severe (3)

**Given** Uncle taps the "1 Month" date range chip
**When** the chart updates
**Then** it displays severity data for the past 30 days and the "1 Month" chip is visually selected

**Given** Uncle has no entries for the selected date range
**When** the chart renders
**Then** it shows an empty state — calm, neutral, no motivational text

**Given** the analytics data is queried from Room
**When** the chart renders
**Then** it completes in under 500ms from local data

**Given** TalkBack is enabled
**When** the user focuses on the trend chart
**Then** TalkBack announces a summary: period, average severity, and trend direction

## Story 5.2: Time-of-Day Breakdown Cards

**As** Uncle (the patient),
**I want** to see average severity broken down by time of day across the selected date range,
**So that** I can tell my doctor which part of the day is consistently worse.

**Acceptance Criteria:**

**Given** Uncle is viewing the Analytics screen with 1 Week selected
**When** the time-of-day breakdown renders
**Then** four cards appear below the trend chart — Morning, Afternoon, Evening, Night — each showing the average severity for that slot over the past 7 days

**Given** Morning entries over the past week are: Moderate, Moderate, Severe, Moderate, Mild, Moderate, Moderate
**When** the Morning average card renders
**Then** it shows "Moderate" as the average (rounded), tinted with the Moderate soft color, with the Moderate icon

**Given** Uncle switches the date range to 3 Months
**When** the breakdown cards update
**Then** averages recalculate for the full 3-month range and cards update accordingly

**Given** no entries exist for the Evening slot in the selected range
**When** the Evening card renders
**Then** it shows a neutral empty state (dash or "No data") — no alarm, no encouragement

**Given** TalkBack is enabled
**When** the user navigates to the Afternoon breakdown card
**Then** TalkBack announces "Afternoon average: Mild over 7 days."

---
