package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.FxRates

class FxRatesRepository(
    private val dao: FxRateDao,
    private val source: FxRatesSource,
) {
    suspend fun refreshRates(base: Currency): FxRates {
        val fetched = source.fetchLatestRates(base, Currency.entries.filter { it != base })
        dao.replaceRates(fetched.toFxRates())
        return fetched
    }

    fun ratesFor(base: Currency): Flow<List<FxRate>> = dao.ratesFor(base)

    private fun FxRates.toFxRates() = rates.map { (quote, rate) ->
        FxRate(baseCurrency = base, quoteCurrency = quote, rate = rate, date = date)
    }
}
