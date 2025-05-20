package com.example.greencodechallenge.data.api

import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

interface ExchangeRateApi {
    @GET("live")
    suspend fun getLatestRates(
        @Query("source") baseCurrency: String,
        @Query("currencies") currencies: String? = null
    ): Response<ExchangeRateResponse>

    @GET("convert")
    suspend fun convertCurrency(
        @Query("from") fromCurrency: String,
        @Query("to") toCurrency: String,
        @Query("amount") amount: Double
    ): Response<ConvertResponse>
} 