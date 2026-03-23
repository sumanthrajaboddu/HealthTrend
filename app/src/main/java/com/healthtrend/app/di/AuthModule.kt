package com.healthtrend.app.di

import com.healthtrend.app.data.auth.GoogleAuthClient
import com.healthtrend.app.data.auth.GoogleAuthManager
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module for authentication components.
 * Binds GoogleAuthClient interface to GoogleAuthManager implementation.
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class AuthModule {

    @Binds
    @Singleton
    abstract fun bindGoogleAuthClient(
        googleAuthManager: GoogleAuthManager
    ): GoogleAuthClient
}
