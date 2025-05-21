package com.example.greencodechallenge.presentation.conversion

import com.example.greencodechallenge.R
import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import com.example.greencodechallenge.domain.usecase.ConvertCurrencyUseCase
import com.example.greencodechallenge.domain.usecase.GetExchangeRatesUseCase
import com.example.greencodechallenge.domain.utils.ResourceProvider
import com.example.greencodechallenge.utils.LogTestUtils
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConversionViewModelTest {
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase = mockk()
    private val convertCurrencyUseCase: ConvertCurrencyUseCase = mockk()
    private val conversionHistoryRepository: ConversionHistoryRepository = mockk(relaxed = true)
    private val resourceProvider: ResourceProvider = mockk()
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var viewModel: ConversionViewModel

    @Before
    fun setUp() {
        // Mock Android Log
        LogTestUtils.mockLog()

        coEvery { resourceProvider.getString(R.string.error_empty_amount) } returns "Please enter an amount"
        coEvery { resourceProvider.getString(R.string.error_invalid_amount) } returns "Invalid amount"
        coEvery { resourceProvider.getString(R.string.error_unknown) } returns "Unknown error"
        coEvery { resourceProvider.getString(R.string.error_conversion) } returns "Conversion error"
        coEvery { resourceProvider.getString(R.string.error_no_rates) } returns "No rates available"
        coEvery { resourceProvider.getString(R.string.error_empty_rates) } returns "No exchange rates available"
        coEvery { resourceProvider.getString(R.string.exchange_rate_format, any(), any(), any()) } returns "1 %s = %s %s"
        coEvery { resourceProvider.getString(R.string.last_updated_format, any()) } returns "Last updated: %s"

        viewModel = ConversionViewModel(
            getExchangeRatesUseCase,
            convertCurrencyUseCase,
            conversionHistoryRepository,
            resourceProvider,
            dispatcher
        )
    }

    @Test
    fun `given valid amount and currencies_When convertCurrency_Then emits Success with result`() = scope.runTest {
        // Given
        val exchangeRate = ExchangeRate("USD", mapOf("EUR" to 0.9), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(exchangeRate)
        coEvery { convertCurrencyUseCase.execute(1.0, "EUR", "USD", any()) } returns Result.success(1.11)
        coEvery { convertCurrencyUseCase.execute(100.0, "EUR", "USD", any()) } returns Result.success(111.11)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()
        viewModel.convertCurrency("100", "EUR", "USD")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is ConversionUiState.Success)
        val successState = state as ConversionUiState.Success
        assertEquals("EUR", successState.fromCurrency)
        assertEquals("USD", successState.toCurrency)
        assertTrue(successState.result.isNotEmpty())
    }

    @Test
    fun `given empty amount_When convertCurrency_Then emits Error`() = scope.runTest {
        // Given
        val exchangeRate = ExchangeRate("USD", mapOf("EUR" to 0.9), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(exchangeRate)
        coEvery { convertCurrencyUseCase.execute(1.0, "EUR", "USD", any()) } returns Result.success(1.11)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()
        viewModel.convertCurrency("", "EUR", "USD")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is ConversionUiState.Error)
        assertEquals("Please enter an amount", (state as ConversionUiState.Error).message)
    }

    @Test
    fun `given invalid amount_When convertCurrency_Then emits Error`() = scope.runTest {
        // Given
        val exchangeRate = ExchangeRate("USD", mapOf("EUR" to 0.9), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(exchangeRate)
        coEvery { convertCurrencyUseCase.execute(1.0, "EUR", "USD", any()) } returns Result.success(1.11)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()
        viewModel.convertCurrency("abc", "EUR", "USD")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is ConversionUiState.Error)
        assertEquals("Invalid amount", (state as ConversionUiState.Error).message)
    }

    @Test
    fun `given network error_When fetchExchangeRates_Then emits Error`() = scope.runTest {
        // Given
        coEvery { getExchangeRatesUseCase(any()) } returns Result.failure(IOException("Network error"))

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is ConversionUiState.Error)
        assertEquals("Network error", (state as ConversionUiState.Error).message)
    }

    @Test
    fun `given empty exchange rates_When fetchExchangeRates_Then emits Error`() = scope.runTest {
        // Given
        val emptyExchangeRate = ExchangeRate("USD", emptyMap(), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(emptyExchangeRate)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Error but got $state", state is ConversionUiState.Error)
        assertEquals("No exchange rates available", (state as ConversionUiState.Error).message)
    }

    @Test
    fun `given successful conversion_When convertCurrency_Then saves to history`() = scope.runTest {
        // Given
        val exchangeRate = ExchangeRate("USD", mapOf("EUR" to 0.9), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(exchangeRate)
        coEvery { convertCurrencyUseCase.execute(1.0, "EUR", "USD", any()) } returns Result.success(1.11)
        coEvery { convertCurrencyUseCase.execute(100.0, "EUR", "USD", any()) } returns Result.success(111.11)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()
        viewModel.convertCurrency("100", "EUR", "USD")
        advanceUntilIdle()

        // Then
        coVerify { conversionHistoryRepository.saveConversion(any()) }
    }

    @Test
    fun `given same currencies_When updateExchangeRate_Then updates ratio correctly`() = scope.runTest {
        // Given
        val exchangeRate = ExchangeRate("USD", mapOf("USD" to 1.0), 1234567890, null)
        coEvery { getExchangeRatesUseCase(any()) } returns Result.success(exchangeRate)
        coEvery { convertCurrencyUseCase.execute(1.0, "USD", "USD", any()) } returns Result.success(1.0)

        // When
        viewModel.fetchExchangeRates()
        advanceUntilIdle()
        viewModel.updateExchangeRate("USD", "USD")
        advanceUntilIdle()

        // Then
        val state = viewModel.uiState.value
        assertTrue("Expected Success but got $state", state is ConversionUiState.Success)
        val successState = state as ConversionUiState.Success
        assertEquals("USD", successState.fromCurrency)
        assertEquals("USD", successState.toCurrency)
    }
} 