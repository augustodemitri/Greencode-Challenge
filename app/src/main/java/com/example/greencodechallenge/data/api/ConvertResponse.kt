package com.example.greencodechallenge.data.api

import com.google.gson.annotations.SerializedName

data class ConvertResponse(
    @SerializedName("success")
    val success: Boolean,
    @SerializedName("terms")
    val terms: String,
    @SerializedName("privacy")
    val privacy: String,
    @SerializedName("query")
    val query: ConvertQuery,
    @SerializedName("info")
    val info: ConvertInfo,
    @SerializedName("result")
    val result: Double
)

data class ConvertQuery(
    @SerializedName("from")
    val from: String,
    @SerializedName("to")
    val to: String,
    @SerializedName("amount")
    val amount: Double
)

data class ConvertInfo(
    @SerializedName("timestamp")
    val timestamp: Long,
    @SerializedName("quote")
    val quote: Double
) 