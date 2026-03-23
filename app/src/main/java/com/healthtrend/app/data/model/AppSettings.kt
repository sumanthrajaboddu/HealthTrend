package com.healthtrend.app.data.model

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Single-row Room entity for app settings.
 * Always id = 1. Query returns one row.
 * Includes reminder fields for Epic 4 to avoid schema migration later.
 */
@Entity(tableName = "app_settings")
data class AppSettings(
    @PrimaryKey
    val id: Int = 1,

    @ColumnInfo(name = "patient_name")
    val patientName: String = "",

    @ColumnInfo(name = "sheet_url")
    val sheetUrl: String = "",

    @ColumnInfo(name = "google_account_email")
    val googleAccountEmail: String? = null,

    @ColumnInfo(name = "global_reminders_enabled")
    val globalRemindersEnabled: Boolean = true,

    @ColumnInfo(name = "morning_reminder_enabled")
    val morningReminderEnabled: Boolean = true,

    @ColumnInfo(name = "afternoon_reminder_enabled")
    val afternoonReminderEnabled: Boolean = true,

    @ColumnInfo(name = "evening_reminder_enabled")
    val eveningReminderEnabled: Boolean = true,

    @ColumnInfo(name = "night_reminder_enabled")
    val nightReminderEnabled: Boolean = true,

    @ColumnInfo(name = "morning_reminder_time")
    val morningReminderTime: String = "08:00",

    @ColumnInfo(name = "afternoon_reminder_time")
    val afternoonReminderTime: String = "13:00",

    @ColumnInfo(name = "evening_reminder_time")
    val eveningReminderTime: String = "18:00",

    @ColumnInfo(name = "night_reminder_time")
    val nightReminderTime: String = "22:00"
)
