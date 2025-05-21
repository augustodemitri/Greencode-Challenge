package com.example.greencodechallenge.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.greencodechallenge.data.local.dao.ConversionHistoryDao
import com.example.greencodechallenge.data.local.entity.ConversionHistory

@Database(entities = [ConversionHistory::class], version = 1, exportSchema = false)
abstract class AppDatabase : RoomDatabase() {
    abstract fun conversionHistoryDao(): ConversionHistoryDao

    companion object {
        private const val DATABASE_NAME = "currency_conversion_db"
        
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    DATABASE_NAME
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
} 