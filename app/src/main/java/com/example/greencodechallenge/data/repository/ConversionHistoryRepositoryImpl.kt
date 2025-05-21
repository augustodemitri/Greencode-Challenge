package com.example.greencodechallenge.data.repository

import androidx.lifecycle.LiveData
import com.example.greencodechallenge.data.local.dao.ConversionHistoryDao
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConversionHistoryRepositoryImpl @Inject constructor(
    private val conversionHistoryDao: ConversionHistoryDao
) : ConversionHistoryRepository {
    
    override suspend fun saveConversion(conversion: ConversionHistory): Long {
        return conversionHistoryDao.insertConversion(conversion)
    }
    
    override fun getAllConversionsFlow(): Flow<List<ConversionHistory>> {
        return conversionHistoryDao.getAllConversionsFlow()
    }

    override fun getRecentConversionsFlow(limit: Int): Flow<List<ConversionHistory>> {
        return conversionHistoryDao.getRecentConversionsFlow(limit)
    }

    override suspend fun clearAllConversions() {
        conversionHistoryDao.deleteAllConversions()
    }
} 