package com.example.greencodechallenge.presentation.detail

import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
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
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class ConversionDetailViewModelTest {
    private val repository: ConversionHistoryRepository = mockk()
    private val dispatcher = StandardTestDispatcher()
    private val scope = TestScope(dispatcher)
    private lateinit var viewModel: ConversionDetailViewModel

    @Before
    fun setUp() {
        LogTestUtils.mockLog()
        viewModel = ConversionDetailViewModel(repository, dispatcher)
    }

    @Test
    fun `given valid id_When loadConversion_Then emits detail state`() = scope.runTest {
        // Given
        val history = ConversionHistory(1, "USD", "EUR", 100.0, 90.0, 0.9, 1234567890)
        coEvery { repository.getConversionById(1) } returns history

        // When
        viewModel.loadConversion(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertEquals("USD", state?.fromCurrency)
        assertEquals("EUR", state?.toCurrency)
        assertEquals(100.0, state?.originalAmount ?: 0.0, 0.01)
        assertEquals(90.0, state?.convertedAmount ?: 0.0, 0.01)
        assertEquals(0.9, state?.conversionRate ?: 0.0, 0.01)
    }

    @Test
    fun `given invalid id_When loadConversion_Then emits null state`() = scope.runTest {
        // Given
        coEvery { repository.getConversionById(999) } returns null

        // When
        viewModel.loadConversion(999)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertNull(state)
    }

    @Test
    fun `given repository error_When loadConversion_Then emits null state`() = scope.runTest {
        // Given
        coEvery { repository.getConversionById(1) } throws IOException("Database error")

        // When
        viewModel.loadConversion(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertNull(state)
    }

    @Test
    fun `given same currency conversion_When loadConversion_Then emits detail state with rate 1`() = scope.runTest {
        // Given
        val history = ConversionHistory(1, "USD", "USD", 100.0, 100.0, 1.0, 1234567890)
        coEvery { repository.getConversionById(1) } returns history

        // When
        viewModel.loadConversion(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertEquals("USD", state?.fromCurrency)
        assertEquals("USD", state?.toCurrency)
        assertEquals(100.0, state?.originalAmount ?: 0.0, 0.01)
        assertEquals(100.0, state?.convertedAmount ?: 0.0, 0.01)
        assertEquals(1.0, state?.conversionRate ?: 0.0, 0.01)
    }

    @Test
    fun `given large amount conversion_When loadConversion_Then emits detail state with correct values`() = scope.runTest {
        // Given
        val history = ConversionHistory(
            id = 1,
            fromCurrency = "USD",
            toCurrency = "JPY",
            originalAmount = 1000000.0, // 1 million USD
            convertedAmount = 150000000.0, // 150 million JPY
            conversionRate = 150.0,
            timestamp = 1234567890
        )
        coEvery { repository.getConversionById(1) } returns history

        // When
        viewModel.loadConversion(1)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertEquals("USD", state?.fromCurrency)
        assertEquals("JPY", state?.toCurrency)
        assertEquals(1000000.0, state?.originalAmount ?: 0.0, 0.01)
        assertEquals(150000000.0, state?.convertedAmount ?: 0.0, 0.01)
        assertEquals(150.0, state?.conversionRate ?: 0.0, 0.01)
    }

    @Test
    fun `given multiple loadConversion calls_When loadConversion_Then emits latest state`() = scope.runTest {
        // Given
        val history1 = ConversionHistory(1, "USD", "EUR", 100.0, 90.0, 0.9, 1234567890)
        val history2 = ConversionHistory(2, "EUR", "GBP", 200.0, 170.0, 0.85, 1234567891)
        coEvery { repository.getConversionById(1) } returns history1
        coEvery { repository.getConversionById(2) } returns history2

        // When
        viewModel.loadConversion(1)
        advanceUntilIdle()
        viewModel.loadConversion(2)
        advanceUntilIdle()

        // Then
        val state = viewModel.conversion.value
        assertEquals("EUR", state?.fromCurrency)
        assertEquals("GBP", state?.toCurrency)
        assertEquals(200.0, state?.originalAmount ?: 0.0, 0.01)
        assertEquals(170.0, state?.convertedAmount ?: 0.0, 0.01)
        assertEquals(0.85, state?.conversionRate ?: 0.0, 0.01)
    }
} 