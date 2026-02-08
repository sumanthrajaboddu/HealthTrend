package com.healthtrend.app.di

import com.healthtrend.app.data.local.AppSettingsDao
import com.healthtrend.app.data.local.HealthEntryDao
import com.healthtrend.app.data.repository.AppSettingsRepository
import com.healthtrend.app.data.repository.HealthEntryRepository
import com.healthtrend.app.data.sync.SyncTrigger
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing repositories.
 */
@Module
@InstallIn(SingletonComponent::class)
object RepositoryModule {

    @Provides
    @Singleton
    fun provideHealthEntryRepository(
        healthEntryDao: HealthEntryDao,
        syncTrigger: SyncTrigger
    ): HealthEntryRepository {
        return HealthEntryRepository(healthEntryDao, syncTrigger)
    }

    @Provides
    @Singleton
    fun provideAppSettingsRepository(
        appSettingsDao: AppSettingsDao
    ): AppSettingsRepository {
        return AppSettingsRepository(appSettingsDao)
    }
}
