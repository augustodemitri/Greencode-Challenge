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
            
            if (response.isSuccessful) {
                val body = response.body()
                
                if (body != null) {
                    val exchangeRate = body.toExchangeRate()
                    if (exchangeRate != null) {
                        // Verificar si la respuesta viene del cache
                        val isFromCache = response.raw().cacheResponse != null
                        val cacheTimestamp = if (isFromCache) {
                            val cacheHeader = response.raw().header("X-Cache-Timestamp")
                            cacheHeader?.toLongOrNull()?.div(1000) // convertir a segundos
                        } else {
                            null
                        }
                        val finalExchangeRate = exchangeRate.copy(cacheTimestamp = cacheTimestamp)
                        Result.success(finalExchangeRate)
                    } else {
                        val errorMessage = body.error?.info ?: "Error desconocido en la respuesta de la API"
                        Result.failure(Exception(errorMessage))
                    }
                } else {
                    Result.failure(Exception("Error: Respuesta vacía de la API"))
                }
            } else {
                val errorBody = response.errorBody()?.string()
                Result.failure(Exception("Error al obtener tasas: ${errorBody ?: response.message()}"))
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
                    Result.failure(Exception("Error en la API: ${response.code()} - ${response.message()}"))
                }
            } else {
                Result.failure(Exception("Error en la conversión: ${response.code()} - ${response.message()}"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 