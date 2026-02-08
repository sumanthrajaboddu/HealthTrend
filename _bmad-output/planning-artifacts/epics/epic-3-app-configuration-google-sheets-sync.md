# Epic 3: App Configuration & Google Sheets Sync

Raja sets up the app — signs in with Google, pastes the Sheet URL, enters Uncle's name. Data syncs silently to Google Sheets so Raja can monitor from anywhere.

## Story 3.1: Settings Screen & App Configuration

**As** Raja (the admin),
**I want** a Settings screen where I can enter the patient name, configure the Google Sheet URL, and manage all app settings with auto-save,
**So that** I can set up the app for Uncle in one sitting without any "save" buttons.

**Acceptance Criteria:**

**Given** the Settings screen requires persistent storage for app configuration
**When** Story 3.1 is implemented
**Then** an AppSettings Room entity is created with fields for patientName, sheetUrl, googleAccountEmail, globalRemindersEnabled, and per-slot reminder toggles/times
**And** an AppSettingsDao provides Flow-based queries and suspend update functions
**And** an AppSettingsRepository is registered with Hilt for dependency injection

**Given** Raja navigates to the Settings tab
**When** the Settings screen renders
**Then** it displays fields for patient name, Google Sheet URL, and a share Sheet link button

**Given** Raja types "Uncle" in the patient name field
**When** the text is entered
**Then** it is immediately persisted to Room AppSettings without any save button or confirmation

**Given** Raja pastes a Google Sheet URL
**When** the URL is entered
**Then** it is auto-saved to AppSettings and basic format validation shows a red outline if the URL appears invalid

**Given** a Google Sheet URL is configured
**When** Raja taps "Share Sheet Link"
**Then** the Android share sheet opens with the Sheet URL as shared content

**Given** TalkBack is enabled
**When** Raja navigates to each settings field
**Then** each field announces its label and current value

## Story 3.2: Google Sign-In with Credential Manager

**As** Raja (the admin),
**I want** to sign in with a Google account once so that the app can access Google Sheets for data sync,
**So that** Uncle never sees a sign-in prompt again after initial setup.

**Acceptance Criteria:**

**Given** the app has never been signed in
**When** Raja taps "Sign in with Google" on the Settings screen
**Then** the Credential Manager sign-in flow appears, Raja selects an account, and upon success the Settings screen shows "Signed in as raja@example.com"

**Given** Raja has signed in previously
**When** the app is reopened after a device restart
**Then** the sign-in persists — Settings shows the signed-in account and no re-auth prompt appears

**Given** Raja is signed in
**When** Raja taps "Sign out"
**Then** the credential is cleared, the Settings screen shows "Not signed in", and sync stops until re-authenticated

**Given** the OAuth token expires
**When** the app needs to access Google Sheets
**Then** Credential Manager silently refreshes the token without user intervention

**Given** token refresh fails (e.g., Google password changed)
**When** the app detects the failure
**Then** a single "Please sign in again" message appears in Settings — not a blocking modal, not a popup, not a toast

**Given** no third-party SDKs are included
**When** the app is built
**Then** no Firebase, analytics, crash reporting, or tracking libraries are present in the dependency tree

## Story 3.3: Two-Way Google Sheets Sync

**As** Uncle (the patient),
**I want** my symptom entries to sync automatically to a Google Sheet in the background without any action from me,
**So that** Raja can monitor my data from anywhere and my entries are safely backed up.

**Implementation Reference:** This is the most technically complex story. Refer to `architecture/core-architectural-decisions.md` → Data Architecture section for the complete two-way timestamp sync design, including: push phase (only entries where synced = false), pull phase (compare Sheet timestamp vs local updatedAt), cell-level writes (never overwrite entire rows), sync triggers (immediate via WorkManager one-time, periodic hourly fallback, on app launch), and ExistingWorkPolicy.KEEP to prevent duplicate syncs.

**Implementation Checklist:**
1. Create SyncWorker (CoroutineWorker) with push-then-pull phases
2. Create SyncManager to enqueue immediate and periodic sync via WorkManager
3. Create GoogleSheetsService for API v4 read/write operations
4. Implement timestamp comparison before every write (both directions)
5. Use BackoffPolicy.EXPONENTIAL with 30-second initial delay for retries
6. Trigger immediate sync from HealthEntryRepository after every save
7. Register periodic sync on app launch

**Acceptance Criteria:**

**Given** Uncle logs a severity entry locally
**When** the entry is saved to Room
**Then** SyncManager enqueues an immediate one-time WorkManager sync request
**And** the entry has synced = false and updatedAt set to current epoch millis

**Given** the sync worker runs with unsynced entries
**When** the push phase executes
**Then** for each unsynced entry, it reads the corresponding Sheet timestamp, writes severity + timestamp ONLY if local updatedAt > Sheet timestamp, and marks the entry synced = true

**Given** another device has written newer data to the Sheet
**When** the pull phase executes
**Then** for each Sheet cell where Sheet timestamp > local updatedAt, the local entry is created or updated
**And** for cells where local is already newer, the local entry is kept unchanged

**Given** Uncle has been offline for 2 weeks with 50 unsynced entries
**When** connectivity is restored and sync runs
**Then** all 50 entries push to the Sheet correctly and any Sheet changes pull to local — zero data loss

**Given** the Google Sheets API is unavailable (server error or quota exceeded)
**When** the sync worker encounters the error
**Then** it returns Result.retry() with exponential backoff (30s initial delay) and no error is shown to the user

**Given** the app is online and functioning
**When** Uncle uses the app normally
**Then** there are zero sync indicators, zero connectivity banners, zero "last synced" timestamps — the app behaves identically online and offline

**Given** sync runs multiple times for the same data
**When** the push and pull phases execute
**Then** the result is idempotent — duplicate syncs produce the same correct data with no corruption

**Given** a new date entry is pushed to the Sheet
**When** the row doesn't exist yet
**Then** a new row is appended with the date in Column A (YYYY-MM-DD format) and severity text uses exact display names ("No Pain", "Mild", "Moderate", "Severe")

---
