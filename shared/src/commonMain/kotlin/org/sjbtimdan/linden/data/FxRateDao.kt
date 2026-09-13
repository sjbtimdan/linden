package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import org.sjbtimdan.linden.FxRateEntity
import org.sjbtimdan.linden.FxRateQueries
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.util.asListFlow

class FxRateDao(private val queries: FxRateQueries) {
    suspend fun replaceRates(rates: List<FxRate>, fetchedAt: Long) {
        rates.forEach { rate ->
            queries.insertOrReplace(
                baseCurrency = rate.baseCurrency.name,
                quoteCurrency = rate.quoteCurrency.name,
                rate = rate.rate,
                date = rate.date,
                fetchedAt = fetchedAt,
            )
        }
    }

    suspend fun setRate(rate: FxRate, fetchedAt: Long) {
        queries.insertOrReplace(
            baseCurrency = rate.baseCurrency.name,
            quoteCurrency = rate.quoteCurrency.name,
            rate = rate.rate,
            date = rate.date,
            fetchedAt = fetchedAt,
        )
    }

    suspend fun lastFetchedAt(base: Currency): Long? = queries.selectFetchedAtByBase(base.name).awaitAsOneOrNull()

    fun ratesFor(base: Currency): Flow<List<FxRate>> = queries.selectByBase(base.name).asListFlow { it.toFxRate() }

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
