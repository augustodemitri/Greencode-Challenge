package com.example.greencodechallenge.data.repository

import com.example.greencodechallenge.data.api.ExchangeRateApi
import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class ExchangeRateRepositoryImpl @Inject constructor(
    private val api: ExchangeRateApi
) : ExchangeRateRepository {
    
    override suspend fun getLatestRates(baseCurrency: String): Result<ExchangeRate> {
        return try {
            val response = api.getLatestRates(baseCurrency)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.toExchangeRate())
                } else {
                    Result.failure(Exception("API error: ${response.errorBody()}"))
                }
            } else {
                Result.failure(Exception("Error fetching exchange rates: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun convertCurrency(
        amount: Double,
        fromCurrency: String,
        toCurrency: String
    ): Result<Double> {
        return try {
            val response = api.convertCurrency(fromCurrency, toCurrency, amount)
            if (response.isSuccessful && response.body() != null) {
                val body = response.body()
                if (body != null && body.success) {
                    Result.success(body.result)
                } else {
                    Result.failure(Exception("API error: ${response.errorBody()}"))
                }
            } else {
                Result.failure(Exception("Error converting currency: ${response.code()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 