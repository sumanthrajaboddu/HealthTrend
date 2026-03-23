package com.healthtrend.app.di

import com.healthtrend.app.data.notification.NotificationScheduler
import com.healthtrend.app.data.notification.ReminderScheduler
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing notification components.
 * Binds [NotificationScheduler] to [ReminderScheduler] interface for testability.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class NotificationModule {

    @Binds
    @Singleton
    abstract fun bindReminderScheduler(impl: NotificationScheduler): ReminderScheduler
}
