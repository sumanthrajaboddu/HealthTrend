package com.healthtrend.app.data.sync

/**
 * Fake SyncTrigger for unit testing.
 * Tracks whether sync was triggered without requiring Android Context.
 */
class FakeSyncTrigger : SyncTrigger {

    var syncEnqueueCount: Int = 0
        private set

    override fun enqueueImmediateSync() {
        syncEnqueueCount++
    }

    fun reset() {
        syncEnqueueCount = 0
    }
}
