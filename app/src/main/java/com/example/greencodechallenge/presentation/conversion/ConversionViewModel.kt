package com.example.greencodechallenge.presentation.conversion

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greencodechallenge.R
import com.example.greencodechallenge.data.local.entity.ConversionHistory
import com.example.greencodechallenge.di.IoDispatcher
import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.repository.ConversionHistoryRepository
import com.example.greencodechallenge.domain.usecase.ConvertCurrencyUseCase
import com.example.greencodechallenge.domain.usecase.GetExchangeRatesUseCase
import com.example.greencodechallenge.domain.utils.CurrencyFormatter
import com.example.greencodechallenge.domain.utils.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val conversionHistoryRepository: ConversionHistoryRepository,
    private val resourceProvider: ResourceProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _uiState = MutableStateFlow<ConversionUiState>(ConversionUiState.Loading)
    val uiState: StateFlow<ConversionUiState> = _uiState.asStateFlow()

    private var selectedFromCurrency: String? = null
    private var selectedToCurrency: String? = null
    private var currentRates: ExchangeRate? = null
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    fun fetchExchangeRates(baseCurrency: String? = null) {
        viewModelScope.launch(ioDispatcher) {
            try {
                _uiState.value = ConversionUiState.Loading
                
                getExchangeRatesUseCase(baseCurrency ?: "USD").fold(
                    onSuccess = { exchangeRate ->
                        if (exchangeRate.rates.isEmpty()) {
                            _uiState.value = ConversionUiState.Error(
                                resourceProvider.getString(R.string.error_empty_rates)
                            )
                            return@fold
                        }
                        
                        currentRates = exchangeRate
                        updateSuccessState()
                    },
                    onFailure = { throwable ->
                        _uiState.value = ConversionUiState.Error(
                            throwable.message ?: resourceProvider.getString(R.string.error_unknown)
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = ConversionUiState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_unknown)
                )
            }
        }
    }

    private fun updateSuccessState() {
        currentRates?.let { rates ->
            val currencies = mutableListOf(rates.baseCurrency)
            currencies.addAll(rates.rates.keys)
            val distinctCurrencies = currencies.distinct().sorted()
            
            val fromCurrency = selectedFromCurrency?.takeIf { it in distinctCurrencies } 
                ?: distinctCurrencies.first()
            val toCurrency = selectedToCurrency?.takeIf { it in distinctCurrencies }
                ?: if (distinctCurrencies.size > 1) distinctCurrencies[1] else distinctCurrencies.first()
            
            selectedFromCurrency = fromCurrency
            selectedToCurrency = toCurrency
            
            viewModelScope.launch(ioDispatcher) {
                convertCurrencyUseCase.execute(
                    amount = 1.0,
                    fromCurrency = fromCurrency,
                    toCurrency = toCurrency,
                    exchangeRate = rates
                ).fold(
                    onSuccess = { convertedAmount ->
                        val ratio = resourceProvider.getString(
                            R.string.exchange_rate_format,
                            fromCurrency,
                            CurrencyFormatter.formatNumber(convertedAmount),
                            toCurrency
                        )
                        
                        // Try to get the cache timestamp from the ExchangeRate model (set in repository)
                        val timestampToShow = rates.cacheTimestamp ?: rates.timestamp
                        val lastUpdated = resourceProvider.getString(
                            R.string.last_updated_format,
                            dateFormat.format(Date(timestampToShow * 1000))
                        )

                        _uiState.value = ConversionUiState.Success(
                            currencies = distinctCurrencies,
                            fromCurrency = fromCurrency,
                            toCurrency = toCurrency,
                            ratio = ratio,
                            result = "",
                            lastUpdated = lastUpdated
                        )
                    },
                    onFailure = { throwable ->
                        _uiState.value = ConversionUiState.Error(
                            throwable.message ?: resourceProvider.getString(R.string.error_conversion)
                        )
                    }
                )
            }
        }
    }

    fun convertCurrency(amount: String, fromCurrency: String, toCurrency: String) {
        if (amount.isBlank()) {
            _uiState.value = ConversionUiState.Error(resourceProvider.getString(R.string.error_empty_amount))
            return
        }

        val amountDouble = amount.toDoubleOrNull()
        if (amountDouble == null) {
            _uiState.value = ConversionUiState.Error(resourceProvider.getString(R.string.error_invalid_amount))
            return
        }

        viewModelScope.launch(ioDispatcher) {
            try {
                currentRates?.let { rates ->
                    convertCurrencyUseCase.execute(
                        amount = amountDouble,
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        exchangeRate = rates
                    ).fold(
                        onSuccess = { convertedAmount ->
                            val result = CurrencyFormatter.formatConversionResult(
                                amount = amountDouble,
                                fromCurrency = fromCurrency,
                                convertedAmount = convertedAmount,
                                toCurrency = toCurrency,
                                locale = Locale.getDefault()
                            )
                            
                            _uiState.update { currentState ->
                                if (currentState is ConversionUiState.Success) {
                                    currentState.copy(
                                        result = result,
                                        ratio = resourceProvider.getString(
                                            R.string.exchange_rate_format,
                                            fromCurrency,
                                            CurrencyFormatter.formatNumber(convertedAmount / amountDouble),
                                            toCurrency
                                        )
                                    )
                                } else {
                                    currentState
                                }
                            }
                            
                            saveConversionToHistory(amountDouble, convertedAmount, fromCurrency, toCurrency, convertedAmount / amountDouble)
                        },
                        onFailure = { throwable ->
                            _uiState.value = ConversionUiState.Error(
                                throwable.message ?: resourceProvider.getString(R.string.error_conversion)
                            )
                        }
                    )
                } ?: run {
                    _uiState.value = ConversionUiState.Error(resourceProvider.getString(R.string.error_no_rates))
                }
            } catch (e: Exception) {
                _uiState.value = ConversionUiState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_unknown)
                )
            }
        }
    }

    fun updateExchangeRate(fromCurrency: String, toCurrency: String) {
        selectedFromCurrency = fromCurrency
        selectedToCurrency = toCurrency
        
        viewModelScope.launch(ioDispatcher) {
            try {
                currentRates?.let { rates ->
                    convertCurrencyUseCase.execute(
                        amount = 1.0,
                        fromCurrency = fromCurrency,
                        toCurrency = toCurrency,
                        exchangeRate = rates
                    ).fold(
                        onSuccess = { convertedAmount ->
                            _uiState.update { currentState ->
                                if (currentState is ConversionUiState.Success) {
                                    currentState.copy(
                                        ratio = resourceProvider.getString(
                                            R.string.exchange_rate_format,
                                            fromCurrency,
                                            CurrencyFormatter.formatNumber(convertedAmount),
                                            toCurrency
                                        )
                                    )
                                } else {
                                    currentState
                                }
                            }
                        },
                        onFailure = { throwable ->
                            _uiState.value = ConversionUiState.Error(
                                throwable.message ?: resourceProvider.getString(R.string.error_conversion)
                            )
                        }
                    )
                } ?: run {
                    _uiState.value = ConversionUiState.Error(resourceProvider.getString(R.string.error_no_rates))
                }
            } catch (e: Exception) {
                _uiState.value = ConversionUiState.Error(
                    e.message ?: resourceProvider.getString(R.string.error_unknown)
                )
            }
        }
    }

    private fun saveConversionToHistory(
        originalAmount: Double,
        convertedAmount: Double,
        fromCurrency: String,
        toCurrency: String,
        conversionRate: Double
    ) {
        viewModelScope.launch(ioDispatcher) {
            val conversionHistory = ConversionHistory(
                fromCurrency = fromCurrency,
                toCurrency = toCurrency,
                originalAmount = originalAmount,
                convertedAmount = convertedAmount,
                conversionRate = conversionRate
            )
            
            try {
                conversionHistoryRepository.saveConversion(conversionHistory)
                Log.d(TAG, "Conversion saved to history")
            } catch (e: Exception) {
                Log.e(TAG, "Error saving conversion to history: ${e.message}")
            }
        }
    }

    companion object {
        const val TAG = "ConversionViewModel"
    }
} 