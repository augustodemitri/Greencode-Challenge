package com.example.greencodechallenge.presentation.history

import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import com.example.greencodechallenge.utils.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.drop
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.last
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import java.util.Calendar

@OptIn(ExperimentalCoroutinesApi::class)
class HistoryViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository: ConversionHistoryRepository = mockk()
    private val dispatcher = mainDispatcherRule.testDispatcher
    private val scope = TestScope(dispatcher)
    private lateinit var viewModel: HistoryViewModel

    private val todayTimestamp = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val yesterdayTimestamp = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -1)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val lastWeekTimestamp = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -7)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    private val oldTimestamp = Calendar.getInstance().apply {
        add(Calendar.DAY_OF_YEAR, -10)
        set(Calendar.HOUR_OF_DAY, 12)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    @Before
    fun setUp() {
        coEvery { repository.getAllConversionsFlow() } returns flowOf(listOf(
            ConversionHistory(1, "USD", "EUR", 100.0, 90.0, 0.9, todayTimestamp),
            ConversionHistory(2, "EUR", "GBP", 200.0, 170.0, 0.85, yesterdayTimestamp),
            ConversionHistory(3, "GBP", "JPY", 300.0, 45000.0, 150.0, lastWeekTimestamp),
            ConversionHistory(4, "JPY", "USD", 400.0, 3.0, 0.0075, oldTimestamp)
        ))

        viewModel = HistoryViewModel(repository, dispatcher)
    }

    @Test
    fun `given repository with conversions_When get conversions_Then emits all conversions`() = scope.runTest {
        // Given
        advanceUntilIdle()
        coVerify { repository.getAllConversionsFlow() }

        // When
        val result = viewModel.conversions.drop(1).first()
        advanceUntilIdle()

        // Then
        assertEquals(4, result.size)
        assertEquals("USD", result[0].fromCurrency)
        assertEquals("EUR", result[1].fromCurrency)
        assertEquals("GBP", result[2].fromCurrency)
        assertEquals("JPY", result[3].fromCurrency)
    }

    @Test
    fun `given repository with conversions_When get today conversions_Then emits only today's conversions`() = scope.runTest {
        // Given
        advanceUntilIdle()
        coVerify { repository.getAllConversionsFlow() }

        // When
        val result = viewModel.todayConversions.drop(1).first()
        advanceUntilIdle()

        // Then
        assertEquals(1, result.size)
        assertEquals("USD", result[0].fromCurrency)
        assertEquals(todayTimestamp, result[0].timestamp)
    }

    @Test
    fun `given repository with conversions_When get last week conversions_Then emits only last week's conversions`() = scope.runTest {
        // Given
        advanceUntilIdle()
        coVerify { repository.getAllConversionsFlow() }

        // When
        val result = viewModel.lastWeekConversions.drop(1).first()
        advanceUntilIdle()

        // Then
        assertEquals(3, result.size)
        assertEquals("USD", result[0].fromCurrency)
        assertEquals("EUR", result[1].fromCurrency)
        assertEquals("GBP", result[2].fromCurrency)
    }

    @Test
    fun `given repository with conversions_When clear history_Then calls repository clear method`() = scope.runTest {
        // Given
        coEvery { repository.clearAllConversions() } returns Unit

        // When
        viewModel.clearHistory()
        advanceUntilIdle()

        // Then
        coVerify { repository.clearAllConversions() }
    }

    @Test
    fun `given empty repository_When get conversions_Then emits empty list`() = scope.runTest {
        // Given
        coEvery { repository.getAllConversionsFlow() } returns flowOf(emptyList())
        viewModel = HistoryViewModel(repository, dispatcher)

        // When
        advanceUntilIdle()
        coVerify { repository.getAllConversionsFlow() }
        val result = viewModel.conversions.first()
        advanceUntilIdle()

        // Then
        assertEquals(0, result.size)
    }
}