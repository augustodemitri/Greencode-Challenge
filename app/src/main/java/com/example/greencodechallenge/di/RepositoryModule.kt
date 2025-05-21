package com.example.greencodechallenge.di

import com.example.greencodechallenge.data.repository.ConversionHistoryRepositoryImpl
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {
    
    @Binds
    @Singleton
    abstract fun bindConversionHistoryRepository(
        repositoryImpl: ConversionHistoryRepositoryImpl
    ): ConversionHistoryRepository
} 