package org.sjbtimdan.linden.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

class FxRatesRepositoryTest : StringSpec({
    "refreshRates caches the fetched rates in the database" {
        val database = lindenDatabase()
        val repository = FxRatesRepository(FxRateDao(database.fxRateQueries), FakeFxRatesSource(), clock)

        repository.refreshRates(Currency.CHF)

        val expected = Currency.entries.filter { it != Currency.CHF }.map {
            FxRate(Currency.CHF, it, 1.0, "2026-08-13")
        }
        repository.ratesFor(Currency.CHF).first() shouldBe expected
    }

    "refreshRates replaces previously cached rates for the base" {
        val database = lindenDatabase()
        val repository = FxRatesRepository(FxRateDao(database.fxRateQueries), FakeFxRatesSource(), clock)

        repository.refreshRates(Currency.CHF)
        repository.refreshRates(Currency.CHF)

        val expected = Currency.entries.filter { it != Currency.CHF }.map {
            FxRate(Currency.CHF, it, 1.0, "2026-08-13")
        }
        repository.ratesFor(Currency.CHF).first() shouldBe expected
        repository.ratesFor(Currency.CHF).first().size shouldBe Currency.entries.size - 1
    }

    "a failed refresh leaves the cached rates untouched" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        val working = FxRatesRepository(dao, FakeFxRatesSource(), clock)
        working.refreshRates(Currency.CHF)

        val failing = FxRatesRepository(
            dao,
            FakeFxRatesSource { error("network") },
            clock,
        )
        shouldThrow<IllegalStateException> { failing.refreshRates(Currency.CHF) }

        working.ratesFor(Currency.CHF).first().size shouldBe Currency.entries.size - 1
    }

    "refreshIfStale does not fetch when the cached rates are fresh" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        dao.replaceRates(existingRates, now.minus(1.hours).toEpochMilliseconds())
        val source = FakeFxRatesSource()
        val repository = FxRatesRepository(dao, source, clock)

        repository.refreshIfStale(Currency.CHF)

        source.fetchCount shouldBe 0
        repository.ratesFor(Currency.CHF).first() shouldBe existingRates
    }

    "refreshIfStale fetches when the cached rates are older than 24 hours" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        dao.replaceRates(existingRates, now.minus(25.hours).toEpochMilliseconds())
        val source = FakeFxRatesSource()
        val repository = FxRatesRepository(dao, source, clock)

        repository.refreshIfStale(Currency.CHF)

        source.fetchCount shouldBe 1
        repository.ratesFor(Currency.CHF).first() shouldBe Currency.entries.filter { it != Currency.CHF }.map {
            FxRate(Currency.CHF, it, 1.0, "2026-08-13")
        }
    }

    "refreshIfStale fetches when no rates are cached" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        val source = FakeFxRatesSource()
        val repository = FxRatesRepository(dao, source, clock)

        repository.refreshIfStale(Currency.CHF)

        source.fetchCount shouldBe 1
    }

    "isStale reports rates fetched exactly 24 hours ago as stale" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        dao.replaceRates(existingRates, now.minus(24.hours).toEpochMilliseconds())
        val repository = FxRatesRepository(dao, FakeFxRatesSource(), clock)

        repository.isStale(Currency.CHF) shouldBe true
    }

    "setRate stores a manually entered rate stamped with today and now" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        val repository = FxRatesRepository(dao, FakeFxRatesSource(), clock, today = { TODAY })

        repository.setRate(Currency.CHF, Currency.EUR, 1.5)

        repository.ratesFor(Currency.CHF).first() shouldBe
            listOf(FxRate(Currency.CHF, Currency.EUR, 1.5, TODAY.toString()))
        dao.lastFetchedAt(Currency.CHF) shouldBe now.toEpochMilliseconds()
        repository.isStale(Currency.CHF) shouldBe false
    }

    "a manually entered rate suppresses a stale refresh" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        val source = FakeFxRatesSource()
        val repository = FxRatesRepository(dao, source, clock, today = { TODAY })

        repository.setRate(Currency.CHF, Currency.EUR, 1.5)
        repository.refreshIfStale(Currency.CHF)

        source.fetchCount shouldBe 0
    }

    "refreshRates overwrites a manually entered rate" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)
        val repository = FxRatesRepository(dao, FakeFxRatesSource(), clock, today = { TODAY })

        repository.setRate(Currency.CHF, Currency.EUR, 1.5)
        repository.refreshRates(Currency.CHF)

        repository.ratesFor(Currency.CHF).first().first { it.quoteCurrency == Currency.EUR }.rate shouldBe 1.0
    }
}) {
    companion object {
        private val now = Instant.parse("2026-08-13T12:00:00Z")
        private val clock: () -> Instant = { now }
        private val TODAY = LocalDate(2026, 8, 13)
        private val existingRates = listOf(
            FxRate(Currency.CHF, Currency.EUR, 1.0669, "2026-08-12"),
            FxRate(Currency.CHF, Currency.USD, 1.2306, "2026-08-12"),
        )
    }
}
