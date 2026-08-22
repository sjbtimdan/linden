package org.sjbtimdan.linden.data

import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRates

class FakeFxRatesSource(
    private val rates: (Currency) -> FxRates = { base ->
        FxRates(
            base = base,
            date = "2026-08-13",
            rates = Currency.entries.filter { it != base }.associateWith { 1.0 },
        )
    },
) : FxRatesSource {
    var fetchCount = 0
        private set

    override suspend fun fetchLatestRates(base: Currency, symbols: List<Currency>): FxRates {
        fetchCount++
        return rates(base)
    }
}
