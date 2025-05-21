package com.example.greencodechallenge.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "conversion_history")
data class ConversionHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val fromCurrency: String,
    val toCurrency: String,
    val originalAmount: Double,
    val convertedAmount: Double,
    val conversionRate: Double,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun getFormattedDate(): Date {
        return Date(timestamp)
    }
} 