package com.example.greencodechallenge.domain.model

data class ExchangeRate(
    val baseCurrency: String,
    val rates: Map<String, Double>,
    val timestamp: Long
) 