package org.sjbtimdan.linden.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class FxRatesRepositoryTest : StringSpec({
    "refreshRates caches the fetched rates in the database" {
        val database = lindenDatabase()
        val repository = FxRatesRepository(FxRateDao(database.fxRateQueries), FakeFxRatesSource())

        repository.refreshRates(Currency.CHF)

        val expected = Currency.entries.filter { it != Currency.CHF }.map {
            FxRate(Currency.CHF, it, 1.0, "2026-08-13")
        }
        repository.ratesFor(Currency.CHF).first() shouldBe expected
    }

    "refreshRates replaces previously cached rates for the base" {
        val database = lindenDatabase()
        val repository = FxRatesRepository(FxRateDao(database.fxRateQueries), FakeFxRatesSource())

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
        val working = FxRatesRepository(dao, FakeFxRatesSource())
        working.refreshRates(Currency.CHF)

        val failing = FxRatesRepository(
            dao,
            FakeFxRatesSource { error("network") },
        )
        shouldThrow<IllegalStateException> { failing.refreshRates(Currency.CHF) }

        working.ratesFor(Currency.CHF).first().size shouldBe Currency.entries.size - 1
    }
})
