package com.example.greencodechallenge.presentation.conversion

sealed interface ConversionUiState {
    data class Success(
        val currencies: List<String>,
        val fromCurrency: String,
        val toCurrency: String,
        val ratio: String,
        val result: String,
        val lastUpdated: String
    ) : ConversionUiState

    data class Error(val message: String) : ConversionUiState
    data object Loading : ConversionUiState
} 