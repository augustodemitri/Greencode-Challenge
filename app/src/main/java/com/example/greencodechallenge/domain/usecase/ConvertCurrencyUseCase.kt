package com.example.greencodechallenge.domain.usecase

import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class ConvertCurrencyUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    /**
     * Convierte una cantidad de una moneda a otra.
     * Si las tasas ya están disponibles localmente, usamos esas para la conversión
     * Si no, hacemos una llamada directa a la API
     */
    suspend fun execute(
        amount: Double,
        fromCurrency: String,
        toCurrency: String,
        exchangeRate: ExchangeRate? = null
    ): Result<Double> {
        // Si no tenemos tasas locales, usar la API
        if (exchangeRate == null) {
            return repository.convertCurrency(amount, fromCurrency, toCurrency)
        }

        // Caso contrario, usar tasas locales
        return try {
            // Si la moneda base ya es la moneda de origen, convertimos directamente
            if (exchangeRate.baseCurrency == fromCurrency) {
                val rate = exchangeRate.rates[toCurrency]
                    ?: return Result.failure(Exception("Target currency not found"))
                if (rate <= 0) {
                    return Result.failure(Exception("Invalid exchange rate"))
                }
                Result.success(amount * rate)
            } else {
                // Necesitamos hacer una conversión a través de la moneda base
                val baseRate = exchangeRate.rates[fromCurrency]
                    ?: return Result.failure(Exception("Source currency not found"))
                if (baseRate <= 0) {
                    return Result.failure(Exception("Invalid exchange rate"))
                }
                
                // Convertir a la moneda base primero
                val amountInBaseCurrency = amount / baseRate
                
                if (toCurrency == exchangeRate.baseCurrency) {
                    Result.success(amountInBaseCurrency)
                } else {
                    // Luego convertimos de la moneda base a la moneda destino
                    val targetRate = exchangeRate.rates[toCurrency]
                        ?: return Result.failure(Exception("Target currency not found"))
                    if (targetRate <= 0) {
                        return Result.failure(Exception("Invalid exchange rate"))
                    }
                    Result.success(amountInBaseCurrency * targetRate)
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
} 