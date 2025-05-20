package com.example.greencodechallenge.domain.utils

import java.text.NumberFormat
import java.util.Locale

object CurrencyFormatter {

    /**
     * Formatea una cadena de resultado de conversión usando el Locale del dispositivo.
     */
    fun formatConversionResult(
        amount: Double,
        fromCurrency: String,
        convertedAmount: Double,
        toCurrency: String,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 2
        
        return "${formatter.format(amount)} $fromCurrency = ${formatter.format(convertedAmount)} $toCurrency"
    }
    
    /**
     * Formatea un número para mostrar como parte del ratio de conversión.
     * @param value Valor a formatear
     * @param locale El locale para el formato
     * @return El valor formateado como string con 4 decimales máximo
     */
    fun formatNumber(
        value: Double,
        locale: Locale = Locale.getDefault()
    ): String {
        val formatter = NumberFormat.getNumberInstance(locale)
        formatter.minimumFractionDigits = 2
        formatter.maximumFractionDigits = 4
        return formatter.format(value)
    }
} 