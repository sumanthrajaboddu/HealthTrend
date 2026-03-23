package com.healthtrend.app.data.sync

/**
 * Interface for triggering sync — enables testing without Android Context.
 * Repository depends on this interface, not the concrete SyncManager.
 */
interface SyncTrigger {
    /**
     * Enqueue an immediate one-time sync.
     * Called after local data changes (e.g., upsertEntry).
     */
    fun enqueueImmediateSync()
}
