package com.example.greencodechallenge.di

import android.content.Context
import com.example.greencodechallenge.data.local.AppDatabase
import com.example.greencodechallenge.data.local.dao.ConversionHistoryDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {
    
    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return AppDatabase.getInstance(context)
    }
    
    @Provides
    fun provideConversionHistoryDao(database: AppDatabase): ConversionHistoryDao {
        return database.conversionHistoryDao()
    }
} 