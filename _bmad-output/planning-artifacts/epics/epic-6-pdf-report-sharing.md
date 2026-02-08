# Epic 6: PDF Report & Sharing

Uncle taps Export, generates a clean PDF with his name, the trend chart, time-of-day summary, and a daily log table — then shares it with the doctor via WhatsApp.

## Story 6.1: PDF Report Generation, Preview & Sharing

**As** Uncle (the patient),
**I want** to generate a PDF report of my symptom data for a selected date range, preview it, and share it with my doctor,
**So that** my doctor can make data-driven treatment decisions during our visit.

**Acceptance Criteria:**

**Given** Uncle is viewing the Analytics screen with 1 Month selected
**When** Uncle taps "Export PDF"
**Then** a loading spinner appears (the only spinner in the entire app) while the PDF generates

**Given** the PDF is being generated
**When** generation completes (< 3 seconds)
**Then** a preview of the PDF is displayed showing: patient name, date range header, trend chart, time-of-day summary, and a daily log table with severity entries per slot

**Given** the PDF includes a daily log table
**When** the table renders
**Then** each row shows: Date, Morning severity, Afternoon severity, Evening severity, Night severity — using severity display names and color coding where supported

**Given** the patient name is set to "Uncle" in Settings
**When** the PDF generates
**Then** the header reads "Uncle" as the patient name
**And** if no patient name is set, the header shows a sensible default or blank — no error

**Given** the PDF preview is displayed
**When** Uncle taps "Share"
**Then** the Android share sheet opens with the PDF file attached, offering WhatsApp, email, print, and other available share targets

**Given** the date range has no entries
**When** the PDF generates
**Then** it produces a valid PDF with empty tables and no chart data — not an error state

**Given** TalkBack is enabled
**When** the user focuses on the Export PDF button
**Then** TalkBack announces "Export PDF report. Double tap to generate."
