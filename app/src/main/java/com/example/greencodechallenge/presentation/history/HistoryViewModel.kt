package com.example.greencodechallenge.presentation.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.di.IoDispatcher
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(
    private val conversionHistoryRepository: ConversionHistoryRepository,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {

    private val allConversionsFlow = conversionHistoryRepository.getAllConversionsFlow()

    val conversions: StateFlow<List<ConversionHistory>> = allConversionsFlow
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val todayConversions: StateFlow<List<ConversionHistory>> = allConversionsFlow
        .map { list ->
            val calendar = Calendar.getInstance()
            calendar.set(Calendar.HOUR_OF_DAY, 0)
            calendar.set(Calendar.MINUTE, 0)
            calendar.set(Calendar.SECOND, 0)
            calendar.set(Calendar.MILLISECOND, 0)
            val startOfDay = calendar.timeInMillis
            
            list.filter { it.timestamp >= startOfDay }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    val lastWeekConversions: StateFlow<List<ConversionHistory>> = allConversionsFlow
        .map { list ->
            val calendar = Calendar.getInstance()
            calendar.add(Calendar.DAY_OF_YEAR, -7)
            val weekAgo = calendar.timeInMillis
            
            list.filter { it.timestamp >= weekAgo }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Lazily,
            initialValue = emptyList()
        )

    fun clearHistory() {
        viewModelScope.launch(ioDispatcher) {
            conversionHistoryRepository.clearAllConversions()
        }
    }
} 