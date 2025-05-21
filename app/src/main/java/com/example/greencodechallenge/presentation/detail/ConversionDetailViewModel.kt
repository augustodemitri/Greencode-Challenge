package com.example.greencodechallenge.presentation.detail

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.di.IoDispatcher
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ConversionDetailViewModel @Inject constructor(
    private val conversionHistoryRepository: ConversionHistoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val _conversion = MutableStateFlow<ConversionDetailUiState?>(null)
    val conversion: StateFlow<ConversionDetailUiState?> = _conversion.asStateFlow()

    fun loadConversion(id: Long) {
        viewModelScope.launch(ioDispatcher) {
            try {
                conversionHistoryRepository.getConversionById(id)?.let { history ->
                    _conversion.value = ConversionDetailUiState(
                        id = history.id,
                        fromCurrency = history.fromCurrency,
                        toCurrency = history.toCurrency,
                        originalAmount = history.originalAmount,
                        convertedAmount = history.convertedAmount,
                        conversionRate = history.conversionRate,
                        timestamp = history.timestamp
                    )
                } ?: run {
                    _conversion.value = null
                }
            } catch (e: Exception) {
                _conversion.value = null
            }
        }
    }
}

data class ConversionDetailUiState(
    val id: Long,
    val fromCurrency: String,
    val toCurrency: String,
    val originalAmount: Double,
    val convertedAmount: Double,
    val conversionRate: Double,
    val timestamp: Long
) 