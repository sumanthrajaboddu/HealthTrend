package com.healthtrend.app.di

import com.healthtrend.app.data.export.AndroidPdfGenerator
import com.healthtrend.app.data.export.PdfGenerator
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing PDF export dependencies (Story 6.1 subtask 2.10).
 */
@Module
@InstallIn(SingletonComponent::class)
abstract class ExportModule {

    @Binds
    @Singleton
    abstract fun bindPdfGenerator(impl: AndroidPdfGenerator): PdfGenerator
}
