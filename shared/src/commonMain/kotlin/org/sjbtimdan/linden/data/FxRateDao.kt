package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.FxRateEntity
import org.sjbtimdan.linden.FxRateQueries
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class FxRateDao(private val queries: FxRateQueries) {
    suspend fun replaceRates(rates: List<FxRate>) {
        rates.forEach { rate ->
            queries.insertOrReplace(
                baseCurrency = rate.baseCurrency.name,
                quoteCurrency = rate.quoteCurrency.name,
                rate = rate.rate,
                date = rate.date,
            )
        }
    }

    fun ratesFor(base: Currency): Flow<List<FxRate>> {
        return queries.selectByBase(base.name)
            .asFlow()
            .map { it.awaitAsList().map { row -> row.toFxRate() } }
    }

    suspend fun deleteRates(base: Currency) {
        queries.deleteByBase(base.name)
    }

    private fun FxRateEntity.toFxRate() = FxRate(
        baseCurrency = Currency.fromCode(baseCurrency),
        quoteCurrency = Currency.fromCode(quoteCurrency),
        rate = rate,
        date = date,
    )
}
