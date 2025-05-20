package com.example.greencodechallenge.presentation.conversion

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.greencodechallenge.R
import com.example.greencodechallenge.di.IoDispatcher
import com.example.greencodechallenge.domain.model.ExchangeRate
import com.example.greencodechallenge.domain.usecase.ConvertCurrencyUseCase
import com.example.greencodechallenge.domain.usecase.GetExchangeRatesUseCase
import com.example.greencodechallenge.domain.utils.CurrencyFormatter
import com.example.greencodechallenge.domain.utils.ResourceProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class ConversionViewModel @Inject constructor(
    private val getExchangeRatesUseCase: GetExchangeRatesUseCase,
    private val convertCurrencyUseCase: ConvertCurrencyUseCase,
    private val resourceProvider: ResourceProvider,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {

    private val _availableCurrencies = MutableLiveData<List<String>>()
    val availableCurrencies: LiveData<List<String>> = _availableCurrencies

    private val _conversionResult = MutableLiveData<String>()
    val conversionResult: LiveData<String> = _conversionResult
    
    private val _conversionRatio = MutableLiveData<String>()
    val conversionRatio: LiveData<String> = _conversionRatio
    
    private val _lastUpdated = MutableLiveData<String>()
    val lastUpdated: LiveData<String> = _lastUpdated

    private val _isLoading = MutableLiveData<Boolean>()
    val isLoading: LiveData<Boolean> = _isLoading

    private val _error = MutableLiveData<String>()
    val error: LiveData<String> = _error

    private var currentRates: ExchangeRate? = null
    private var defaultFrom = ""
    private var defaultTo = ""
    private var lastRatesUpdateTime: Long = 0L
    private val dateFormat = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())

    init {
        _conversionRatio.value = ""
        _lastUpdated.value = ""
        
        // Cargar datos al iniciar
        fetchExchangeRates()
    }
    
    private fun shouldUpdateRates(): Boolean {
        // Si no tenemos tasas o nunca se han actualizado (lastRatesUpdateTime = 0), debemos actualizar
        if (currentRates == null || lastRatesUpdateTime == 0L) {
            return true
        }
        
        // Si ha pasado el intervalo de actualización, debemos actualizar
        return System.currentTimeMillis() - lastRatesUpdateTime > RATES_UPDATE_INTERVAL
    }
    
    private fun updateLastUpdatedText() {
        if (lastRatesUpdateTime > 0) {
            val formattedDate = dateFormat.format(Date(lastRatesUpdateTime))
            _lastUpdated.postValue(resourceProvider.getString(R.string.last_updated_format, formattedDate))
        }
    }

    fun fetchExchangeRates(baseCurrency: String? = null) {
        // Solo actualizar si es necesario
        if (!shouldUpdateRates() && currentRates != null) {
            val timeMessage = if (lastRatesUpdateTime > 0) {
                "last updated: ${dateFormat.format(Date(lastRatesUpdateTime))}"
            } else {
                "(no previous update)"
            }
            Log.d(TAG, "Using cached rates, $timeMessage")
            
            currentRates?.let { rates ->
                val currencies = mutableListOf(rates.baseCurrency)
                currencies.addAll(rates.rates.keys)
                
                val distinctCurrencies = currencies.distinct().sorted()
                _availableCurrencies.postValue(distinctCurrencies)
            }
            return
        }
        
        Log.d(TAG, "Fetching new rates from API")
        _isLoading.postValue(true)
        
        viewModelScope.launch(ioDispatcher) {
            try {
                getExchangeRatesUseCase(baseCurrency ?: "USD").fold(
                    onSuccess = { exchangeRate ->
                        if (exchangeRate.rates.isEmpty()) {
                            Log.e(TAG, "Received empty rates from API")
                            _error.postValue(resourceProvider.getString(R.string.error_empty_rates))
                            _isLoading.postValue(false)
                            return@fold
                        }
                        
                        currentRates = exchangeRate
                        lastRatesUpdateTime = System.currentTimeMillis()
                        updateLastUpdatedText()
                        
                        val currencies = mutableListOf(exchangeRate.baseCurrency)
                        currencies.addAll(exchangeRate.rates.keys)
                        
                        val distinctCurrencies = currencies.distinct().sorted()
                        
                        // Guardar las monedas por defecto para inicialización
                        if (distinctCurrencies.isNotEmpty()) {
                            defaultFrom = distinctCurrencies.first()
                            defaultTo = if (distinctCurrencies.size > 1) distinctCurrencies[1] else distinctCurrencies.first()
                            
                            // Pre-calcular el ratio inicial con las tasas disponibles
                            val initialRatio = calculateRatio(defaultFrom, defaultTo, exchangeRate)
                            if (initialRatio > 0) {
                                val ratioText = resourceProvider.getString(
                                    R.string.exchange_rate_format,
                                    defaultFrom,
                                    CurrencyFormatter.formatNumber(initialRatio),
                                    defaultTo
                                )
                                _conversionRatio.postValue(ratioText)
                            }
                        }
                        
                        _availableCurrencies.postValue(distinctCurrencies)
                        _isLoading.postValue(false)
                        
                        Log.d(TAG, "Rates updated at: ${dateFormat.format(Date(lastRatesUpdateTime))}")
                    },
                    onFailure = { throwable ->
                        Log.e(TAG, "Error fetching rates: ${throwable.message}")
                        _error.postValue(throwable.message ?: resourceProvider.getString(R.string.error_unknown))
                        _isLoading.postValue(false)
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "Exception fetching rates: ${e.message}")
                _error.postValue(e.message ?: resourceProvider.getString(R.string.error_unknown))
                _isLoading.postValue(false)
            }
        }
    }
    
    /**
     * Obtiene y muestra solo el ratio de conversión cuando cambian las monedas seleccionadas
     */
    fun updateExchangeRate(fromCurrency: String, toCurrency: String) {
        if (fromCurrency.isEmpty() || toCurrency.isEmpty()) {
            return
        }
        
        // Intentar calcular el ratio con los datos actuales primero
        currentRates?.let { rates ->
            val ratio = calculateRatio(fromCurrency, toCurrency, rates)
            if (ratio > 0) {
                _conversionRatio.value = resourceProvider.getString(
                    R.string.exchange_rate_format,
                    fromCurrency,
                    CurrencyFormatter.formatNumber(ratio),
                    toCurrency
                )
                return
            }
        }
        
        // Si no tenemos datos, los obtenemos, pero respetando el intervalo de actualización
        if (shouldUpdateRates()) {
            Log.d(TAG, "Need to update rates, fetching from API")
            _conversionRatio.value = resourceProvider.getString(R.string.loading_exchange_rate)
            
            viewModelScope.launch(ioDispatcher) {
                try {
                    getExchangeRatesUseCase("USD").fold(
                        onSuccess = { exchangeRate ->
                            currentRates = exchangeRate
                            lastRatesUpdateTime = System.currentTimeMillis()
                            updateLastUpdatedText()
                            
                            val ratio = calculateRatio(fromCurrency, toCurrency, exchangeRate)
                            
                            if (ratio > 0) {
                                val ratioText = resourceProvider.getString(
                                    R.string.exchange_rate_format,
                                    fromCurrency,
                                    CurrencyFormatter.formatNumber(ratio),
                                    toCurrency
                                )
                                _conversionRatio.postValue(ratioText)
                            } else {
                                _conversionRatio.postValue(resourceProvider.getString(R.string.exchange_rate_not_available))
                            }
                            
                            Log.d(TAG, "Rates updated at: ${dateFormat.format(Date(lastRatesUpdateTime))}")
                        },
                        onFailure = { throwable ->
                            Log.e(TAG, "Error updating rates: ${throwable.message}")
                            _conversionRatio.postValue(resourceProvider.getString(R.string.error_fetching_rates))
                            _error.postValue(throwable.message ?: resourceProvider.getString(R.string.error_fetching_rates))
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception updating rates: ${e.message}")
                    _conversionRatio.postValue(resourceProvider.getString(R.string.error_fetching_rates))
                    _error.postValue(e.message ?: resourceProvider.getString(R.string.error_unknown))
                }
            }
        } else {
            // Si ya intentamos calcular con los datos en caché y falló, y no es momento de actualizar,
            // simplemente mostramos el mensaje de error
            val timeMessage = if (lastRatesUpdateTime > 0) {
                "from: ${dateFormat.format(Date(lastRatesUpdateTime))}"
            } else {
                "(no previous update)"
            }
            Log.d(TAG, "Using cached rates $timeMessage, but calculation failed")
            _conversionRatio.value = resourceProvider.getString(R.string.exchange_rate_not_available)
        }
    }

    /**
     * Calcula el ratio de una moneda a otra usando las tasas recibidas
     */
    private fun calculateRatio(fromCurrency: String, toCurrency: String, exchangeRate: ExchangeRate): Double {
        return if (fromCurrency == toCurrency) {
            1.0
        } else if (fromCurrency == exchangeRate.baseCurrency) {
            // Si la moneda base es la moneda origen, usamos directamente la tasa
            exchangeRate.rates[toCurrency] ?: 0.0
        } else if (toCurrency == exchangeRate.baseCurrency) {
            // Convertir a la moneda base (relación inversa)
            val fromRate = exchangeRate.rates[fromCurrency] ?: 0.0
            if (fromRate > 0) 1.0 / fromRate else 0.0
        } else {
            // Conversión entre dos monedas que no son la base
            val fromRate = exchangeRate.rates[fromCurrency] ?: 0.0
            val toRate = exchangeRate.rates[toCurrency] ?: 0.0
            if (fromRate > 0) toRate / fromRate else 0.0
        }
    }

    fun convertCurrency(amount: String, fromCurrency: String, toCurrency: String) {
        if (amount.isBlank()) {
            _error.value = resourceProvider.getString(R.string.error_empty_amount)
            return
        }

        val amountDouble = amount.toDoubleOrNull()
        if (amountDouble == null) {
            _error.value = resourceProvider.getString(R.string.error_invalid_amount)
            return
        }

        _isLoading.value = true
        
        Log.d(TAG, "Converting $amount from $fromCurrency to $toCurrency")
        
        // Intentar hacer la conversión con los datos actuales primero
        currentRates?.let { rates ->
            try {
                val ratio = calculateRatio(fromCurrency, toCurrency, rates)
                if (ratio > 0) {
                    // Actualizar la visualización del ratio
                    _conversionRatio.value = resourceProvider.getString(
                        R.string.exchange_rate_format,
                        fromCurrency,
                        CurrencyFormatter.formatNumber(ratio),
                        toCurrency
                    )
                    
                    val convertedAmount = amountDouble * ratio
                    _conversionResult.value = CurrencyFormatter.formatConversionResult(
                        amount = amountDouble,
                        fromCurrency = fromCurrency,
                        convertedAmount = convertedAmount,
                        toCurrency = toCurrency,
                        locale = Locale.getDefault()
                    )
                    _isLoading.value = false
                    Log.d(TAG, "Conversion successful using cached rates")
                    return@let
                }
            } catch (e: Exception) {
                Log.e(TAG, e.message.toString())
            }
        }

        if (shouldUpdateRates()) {
            Log.d(TAG, "Need to update rates for conversion, fetching from API")
            
            viewModelScope.launch(ioDispatcher) {
                try {
                    getExchangeRatesUseCase("USD").fold(
                        onSuccess = { exchangeRate ->
                            currentRates = exchangeRate
                            lastRatesUpdateTime = System.currentTimeMillis()
                            updateLastUpdatedText()
                            
                            // Calcular y actualizar el ratio
                            val ratio = calculateRatio(fromCurrency, toCurrency, exchangeRate)
                            if (ratio > 0) {
                                val ratioText = resourceProvider.getString(
                                    R.string.exchange_rate_format,
                                    fromCurrency,
                                    CurrencyFormatter.formatNumber(ratio),
                                    toCurrency
                                )
                                _conversionRatio.postValue(ratioText)
                            }
                            
                            // Ahora hacemos la conversión con las tasas actualizadas
                            val result = convertCurrencyUseCase.execute(
                                amount = amountDouble,
                                fromCurrency = fromCurrency,
                                toCurrency = toCurrency,
                                exchangeRate = exchangeRate
                            )

                            result.fold(
                                onSuccess = { convertedAmount ->
                                    val resultText = CurrencyFormatter.formatConversionResult(
                                        amount = amountDouble,
                                        fromCurrency = fromCurrency,
                                        convertedAmount = convertedAmount,
                                        toCurrency = toCurrency,
                                        locale = Locale.getDefault()
                                    )
                                    _conversionResult.postValue(resultText)
                                    _isLoading.postValue(false)
                                    Log.d(TAG, "Conversion successful with updated rates")
                                },
                                onFailure = { throwable ->
                                    Log.e(TAG, "Conversion error: ${throwable.message}")
                                    _error.postValue(throwable.message ?: resourceProvider.getString(R.string.error_conversion))
                                    _isLoading.postValue(false)
                                }
                            )
                        },
                        onFailure = { throwable ->
                            Log.e(TAG, "Error fetching rates for conversion: ${throwable.message}")
                            _error.postValue(throwable.message ?: resourceProvider.getString(R.string.error_fetching_rates))
                            _isLoading.postValue(false)
                        }
                    )
                } catch (e: Exception) {
                    Log.e(TAG, "Exception during conversion: ${e.message}")
                    _error.postValue(e.message ?: resourceProvider.getString(R.string.error_unknown))
                    _isLoading.postValue(false)
                }
            }
        } else {
            // Si no necesitamos actualizar pero aún así no pudimos calcular, mostrar error
            Log.e(TAG, "Conversion failed with cached rates, but not updating from API")
            _error.postValue(resourceProvider.getString(R.string.error_conversion))
            _isLoading.postValue(false)
        }
    }
    
    companion object {
        const val RATES_UPDATE_INTERVAL = 60 * 60 * 1000 // 1 hora en milisegundos
        const val TAG = "ConversionViewModel"
    }
} 