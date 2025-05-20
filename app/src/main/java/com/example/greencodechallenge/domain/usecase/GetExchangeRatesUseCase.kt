package com.example.greencodechallenge.domain.usecase

import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import javax.inject.Inject

class GetExchangeRatesUseCase @Inject constructor(
    private val repository: ExchangeRateRepository
) {
    suspend operator fun invoke(baseCurrency: String): Result<ExchangeRate> {
        return repository.getLatestRates(baseCurrency)
    }
} 