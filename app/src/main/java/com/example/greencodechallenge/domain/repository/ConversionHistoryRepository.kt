package com.example.greencodechallenge.domain.repository

import androidx.lifecycle.LiveData
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import kotlinx.coroutines.flow.Flow

interface ConversionHistoryRepository {
    suspend fun saveConversion(conversion: ConversionHistory): Long
    fun getAllConversionsFlow(): Flow<List<ConversionHistory>>
    fun getRecentConversionsFlow(limit: Int): Flow<List<ConversionHistory>>
    suspend fun clearAllConversions()
    suspend fun getConversionById(id: Long): ConversionHistory?
} 