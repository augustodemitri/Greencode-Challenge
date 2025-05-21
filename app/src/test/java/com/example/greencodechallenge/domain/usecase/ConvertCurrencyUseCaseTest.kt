package com.example.greencodechallenge.domain.usecase

import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ExchangeRateRepository
import io.mockk.coEvery
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class ConvertCurrencyUseCaseTest {
    private val repository: ExchangeRateRepository = mockk()
    private lateinit var useCase: ConvertCurrencyUseCase

    @Before
    fun setUp() {
        useCase = ConvertCurrencyUseCase(repository)
    }

    @Test
    fun `given same currency_When execute_Then returns same amount`() = runTest {
        // Given
        val amount = 100.0
        val currency = "USD"
        val exchangeRate = ExchangeRate(currency, mapOf(currency to 1.0), 1234567890)

        // When
        val result = useCase.execute(amount, currency, currency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(amount, result.getOrNull())
    }

    @Test
    fun `given direct conversion_When execute_Then returns correct amount`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 0.85), 1234567890)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(85.0, result.getOrNull())
    }

    @Test
    fun `given indirect conversion_When execute_Then returns correct amount`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "EUR"
        val toCurrency = "JPY"
        val baseCurrency = "USD"
        val exchangeRate = ExchangeRate(
            baseCurrency,
            mapOf(
                fromCurrency to 0.85,
                toCurrency to 150.0
            ),
            1234567890
        )

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        // 100 EUR -> USD = 100/0.85 = 117.6470588235294 USD
        // 117.6470588235294 USD -> JPY = 117.6470588235294 * 150 = 17647.058823529413 JPY
        assertEquals(17647.058823529413, result.getOrNull() ?: 0.0, 0.000000000001)
    }

    @Test
    fun `given missing source currency_When execute_Then returns failure`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "EUR"
        val toCurrency = "JPY"
        val baseCurrency = "USD"
        val exchangeRate = ExchangeRate(
            baseCurrency,
            mapOf(toCurrency to 150.0), // Missing EUR rate
            1234567890
        )

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Source currency not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given missing target currency_When execute_Then returns failure`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "EUR"
        val toCurrency = "JPY"
        val baseCurrency = "USD"
        val exchangeRate = ExchangeRate(
            baseCurrency,
            mapOf(fromCurrency to 0.85), // Missing JPY rate
            1234567890
        )

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isFailure)
        assertEquals("Target currency not found", result.exceptionOrNull()?.message)
    }

    @Test
    fun `given no exchange rate_When execute_Then calls repository`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "EUR"
        val toCurrency = "JPY"
        coEvery { repository.convertCurrency(amount, fromCurrency, toCurrency) } returns Result.success(15000.0)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(15000.0, result.getOrNull())
    }

    @Test
    fun `given repository error_When execute_Then returns failure`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "EUR"
        val toCurrency = "JPY"
        val error = Exception("API Error")
        coEvery { repository.convertCurrency(amount, fromCurrency, toCurrency) } returns Result.failure(error)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency)

        // Then
        assertTrue(result.isFailure)
        assertEquals(error, result.exceptionOrNull())
    }

    @Test
    fun `given zero amount_When execute_Then returns zero`() = runTest {
        // Given
        val amount = 0.0
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 0.85), 1234567890)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0.0, result.getOrNull())
    }

    @Test
    fun `given negative amount_When execute_Then returns negative result`() = runTest {
        // Given
        val amount = -100.0
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 0.85), 1234567890)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(-85.0, result.getOrNull())
    }

    @Test
    fun `given very large amount_When execute_Then handles correctly`() = runTest {
        // Given
        val amount = 1_000_000_000.0 // 1 billion
        val fromCurrency = "USD"
        val toCurrency = "JPY"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 150.0), 1234567890)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(150_000_000_000.0, result.getOrNull())
    }

    @Test
    fun `given very small amount_When execute_Then handles correctly`() = runTest {
        // Given
        val amount = 0.000001 // 1 millionth
        val fromCurrency = "USD"
        val toCurrency = "JPY"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 150.0), 1234567890)

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isSuccess)
        assertEquals(0.00015, result.getOrNull())
    }

    @Test
    fun `given invalid exchange rate_When execute_Then returns failure`() = runTest {
        // Given
        val amount = 100.0
        val fromCurrency = "USD"
        val toCurrency = "EUR"
        val exchangeRate = ExchangeRate(fromCurrency, mapOf(toCurrency to 0.0), 1234567890) // Invalid rate of 0

        // When
        val result = useCase.execute(amount, fromCurrency, toCurrency, exchangeRate)

        // Then
        assertTrue(result.isFailure)
    }
}