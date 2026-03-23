package com.healthtrend.app.di

import android.content.Context
import androidx.room.Room
import com.healthtrend.app.data.local.AppSettingsDao
import com.healthtrend.app.data.local.HealthEntryDao
import com.healthtrend.app.data.local.HealthTrendDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing Room database and DAOs as singletons.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): HealthTrendDatabase {
        return Room.databaseBuilder(
            context,
            HealthTrendDatabase::class.java,
            "healthtrend_database"
        ).build()
    }

    @Provides
    @Singleton
    fun provideHealthEntryDao(
        database: HealthTrendDatabase
    ): HealthEntryDao {
        return database.healthEntryDao()
    }

    @Provides
    @Singleton
    fun provideAppSettingsDao(
        database: HealthTrendDatabase
    ): AppSettingsDao {
        return database.appSettingsDao()
    }
}
