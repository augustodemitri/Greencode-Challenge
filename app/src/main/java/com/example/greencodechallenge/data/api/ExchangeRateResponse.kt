package com.example.greencodechallenge.data.api

import com.example.greencodechallenge.domain.model.ExchangeRate
import com.google.gson.annotations.SerializedName

data class ExchangeRateResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("terms")
    val terms: String,
    @SerializedName("privacy")
    val privacy: String,
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("source")
    val source: String,
    @SerializedName("quotes")
    val quotes: Map<String, Double>
) {
    fun toExchangeRate(): ExchangeRate {
        return ExchangeRate(
            baseCurrency = source,
            rates = quotes.mapKeys { entry -> 
                // La API devuelve pares como "USDEUR", necesitamos extraer "EUR"
                entry.key.substring(source.length)
            },
            timestamp = timestamp
        )
    }
} 