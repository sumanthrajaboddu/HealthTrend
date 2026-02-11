package com.healthtrend.app.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.healthtrend.app.data.model.AppSettings
import com.healthtrend.app.data.model.HealthEntry

/**
 * Room database for HealthTrend.
 * Uses KSP annotation processor (NOT kapt).
 */
@Database(
    entities = [HealthEntry::class, AppSettings::class],
    version = 1,
    exportSchema = true
)
@TypeConverters(Converters::class)
abstract class HealthTrendDatabase : RoomDatabase() {
    abstract fun healthEntryDao(): HealthEntryDao
    abstract fun appSettingsDao(): AppSettingsDao
}
