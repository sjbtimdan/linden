package org.sjbtimdan.linden.model

data class FxRate(
    val baseCurrency: Currency,
    val quoteCurrency: Currency,
    val rate: Double,
    val date: String,
)

data class FxRates(
    val base: Currency,
    val date: String,
    val rates: Map<Currency, Double>,
)
