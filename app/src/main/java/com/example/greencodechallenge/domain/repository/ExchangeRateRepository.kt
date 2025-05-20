package com.example.greencodechallenge.domain.repository

import com.example.greencodechallenge.domain.model.ExchangeRate

interface ExchangeRateRepository {
    suspend fun getLatestRates(baseCurrency: String): Result<ExchangeRate>
    
    suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Result<Double>
} 