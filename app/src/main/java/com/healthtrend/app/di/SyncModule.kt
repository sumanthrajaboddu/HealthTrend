package com.healthtrend.app.di

import com.healthtrend.app.data.sync.GoogleSheetsService
import com.healthtrend.app.data.sync.SheetsClient
import com.healthtrend.app.data.sync.SyncManager
import com.healthtrend.app.data.sync.SyncTrigger
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for sync components.
 * Binds SheetsClient interface to GoogleSheetsService implementation.
 * Binds SyncTrigger interface to SyncManager implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class SyncModule {

    @Binds
    @Singleton
    abstract fun bindSheetsClient(
        googleSheetsService: GoogleSheetsService
    ): SheetsClient

    @Binds
    @Singleton
    abstract fun bindSyncTrigger(
        syncManager: SyncManager
    ): SyncTrigger
}
