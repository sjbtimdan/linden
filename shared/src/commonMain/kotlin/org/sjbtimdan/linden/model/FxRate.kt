package org.sjbtimdan.linden.model

import androidx.compose.runtime.Immutable

@Immutable
data class FxRate(
    val baseCurrency: Currency,
    val quoteCurrency: Currency,
    val rate: Double,
    val date: String,
)

@Immutable
data class FxRates(
    val base: Currency,
    val date: String,
    val rates: Map<Currency, Double>,
)
