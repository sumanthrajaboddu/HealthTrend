# Executive Summary

HealthTrend is a lightweight, single-user Android app for daily health symptom tracking. The primary user (Uncle) logs symptom severity across four daily time slots (Morning, Afternoon, Evening, Night) using a tap-based "Day Card" interface. All entries sync to a Google Sheet for persistence and portability. An analytics dashboard visualizes trends, and a PDF export feature packages insights for doctor visits.

**Core Differentiator:** Purpose-built simplicity — one screen, four taps per day, zero friction. Google Sheets as backend eliminates server infrastructure while keeping data instantly accessible to the patient and caregivers.

**Target Users:**
- **Primary — The Patient (Uncle):** Basic smartphone user who values "open, tap, done." Needs to log severity in under 5 seconds per entry with zero onboarding.
- **Secondary — The Doctor:** Receives PDF reports during visits. Needs to scan trends in 30 seconds. Never interacts with the app directly.
- **Admin — Raja:** Builds and configures the app. Handles one-time setup. Monitors via shared Google Sheet.

**Technology:** Native Android (Kotlin, Jetpack Compose, Material Design 3). Local-first with Room database. Background sync to Google Sheets via OAuth 2.0 (note: upgraded from Product Brief's Service Account approach for better security and standard user-facing auth flow). APK sideload distribution.

**Regulatory Context:** HealthTrend is a personal-use symptom tracking app. It does not make clinical claims, provide medical diagnosis, or facilitate regulated patient-provider data exchange. It is not subject to HIPAA (personal data on a personal Google Sheet, not a covered entity), FDA medical device classification, or other healthcare compliance frameworks. Health data is stored locally and synced only to the user's own configured Google Sheet.

---
