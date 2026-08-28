package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import kotlinx.datetime.todayIn
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.FxRates
import kotlin.time.Clock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class FxRatesRepository(
    private val dao: FxRateDao,
    private val source: FxRatesSource,
    private val clock: () -> Instant = { Clock.System.now() },
    private val today: () -> LocalDate = { Clock.System.todayIn(TimeZone.currentSystemDefault()) },
) {
    suspend fun refreshRates(base: Currency): FxRates {
        val fetched = source.fetchLatestRates(base, Currency.entries.filter { it != base })
        dao.replaceRates(fetched.toFxRates(), clock().toEpochMilliseconds())
        return fetched
    }

    suspend fun setRate(base: Currency, quote: Currency, rate: Double) {
        val now = clock()
        dao.setRate(
            rate = FxRate(
                baseCurrency = base,
                quoteCurrency = quote,
                rate = rate,
                date = today().toString(),
            ),
            fetchedAt = now.toEpochMilliseconds(),
        )
    }

    suspend fun refreshIfStale(base: Currency) {
        if (!isStale(base)) return
        refreshRates(base)
    }

    suspend fun isStale(base: Currency): Boolean {
        val fetchedAt = dao.lastFetchedAt(base) ?: return true
        return clock() - Instant.fromEpochMilliseconds(fetchedAt) >= RATES_STALE_AFTER
    }

    suspend fun lastFetchedAt(base: Currency): Long? = dao.lastFetchedAt(base)

    fun ratesFor(base: Currency): Flow<List<FxRate>> = dao.ratesFor(base)

    private fun FxRates.toFxRates() = rates.map { (quote, rate) ->
        FxRate(baseCurrency = base, quoteCurrency = quote, rate = rate, date = date)
    }

    private companion object {
        val RATES_STALE_AFTER: Duration = 24.hours
    }
}
