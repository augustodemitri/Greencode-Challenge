package com.example.greencodechallenge.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import kotlinx.coroutines.flow.Flow

@Dao
interface ConversionHistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertConversion(conversion: ConversionHistory): Long

    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC")
    fun getAllConversionsFlow(): Flow<List<ConversionHistory>>

    @Query("SELECT * FROM conversion_history ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentConversionsFlow(limit: Int): Flow<List<ConversionHistory>>

    @Query("DELETE FROM conversion_history")
    suspend fun deleteAllConversions()
} 