package com.example.greencodechallenge.domain.usecase

import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class GetExchangeRatesUseCaseTest {
    private val repository: ExchangeRateRepository = mockk()
    private lateinit var useCase: GetExchangeRatesUseCase

    @Before
    fun setUp() {
        useCase = GetExchangeRatesUseCase(repository)
    }

    @Test
    fun `given valid base currency_When invoke_Then returns exchange rates`() = runTest {
        // Given
        val baseCurrency = "USD"
        val expectedRates = ExchangeRate(
            baseCurrency = baseCurrency,
            rates = mapOf(
                "EUR" to 0.85,
                "JPY" to 150.0,
                "GBP" to 0.75
            ),
            timestamp = 1234567890
        )
        coEvery { repository.getLatestRates(baseCurrency) } returns Result.success(expectedRates)

        // When
        val result = useCase(baseCurrency)

        // Then
        assertTrue(result.isSuccess)
        val rates = result.getOrNull()
        assertEquals(baseCurrency, rates?.baseCurrency)
        assertEquals(3, rates?.rates?.size)
        assertEquals(0.85, rates?.rates?.get("EUR"))
        assertEquals(150.0, rates?.rates?.get("JPY"))
        assertEquals(0.75, rates?.rates?.get("GBP"))
    }

    @Test
    fun `given empty rates_When invoke_Then returns empty exchange rates`() = runTest {
        // Given
        val baseCurrency = "USD"
        val expectedRates = ExchangeRate(
            baseCurrency = baseCurrency,
            rates = emptyMap(),
            timestamp = 1234567890
        )
        coEvery { repository.getLatestRates(baseCurrency) } returns Result.success(expectedRates)

        // When
        val result = useCase(baseCurrency)

        // Then
        assertTrue(result.isSuccess)
        val rates = result.getOrNull()
        assertEquals(baseCurrency, rates?.baseCurrency)
        assertTrue(rates?.rates?.isEmpty() ?: false)
    }

    @Test
    fun `given network error_When invoke_Then returns failure`() = runTest {
        // Given
        val baseCurrency = "USD"
        val error = IOException("Network error")
        coEvery { repository.getLatestRates(baseCurrency) } returns Result.failure(error)

        // When
        val result = useCase(baseCurrency)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `given API error_When invoke_Then returns failure`() = runTest {
        // Given
        val baseCurrency = "USD"
        val error = Exception("API error: Invalid currency")
        coEvery { repository.getLatestRates(baseCurrency) } returns Result.failure(error)

        // When
        val result = useCase(baseCurrency)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }
} 