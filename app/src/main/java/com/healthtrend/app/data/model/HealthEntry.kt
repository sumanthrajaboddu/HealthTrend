package com.healthtrend.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Room entity for health entries.
 * Composite unique constraint on (date, time_slot) — one entry per slot per day.
 */
@Entity(
    tableName = "health_entries",
    indices = [
        Index(value = ["date", "time_slot"], unique = true)
    ]
)
data class HealthEntry(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "date")
    val date: String, // YYYY-MM-DD format

    @ColumnInfo(name = "time_slot")
    val timeSlot: TimeSlot,

    @ColumnInfo(name = "severity")
    val severity: Severity,

    @ColumnInfo(name = "is_synced")
    val synced: Boolean = false,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis()
)
