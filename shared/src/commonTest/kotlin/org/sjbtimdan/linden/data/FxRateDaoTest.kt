package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.FxRate

class FxRateDaoTest : StringSpec({
    "replaceRates stores rates for a base and ratesFor returns them" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)

        dao.ratesFor(Currency.CHF).first() shouldBe emptyList()
        dao.replaceRates(rates)
        dao.ratesFor(Currency.CHF).first() shouldBe rates
    }

    "replaceRates replaces existing rates for the same base" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)

        dao.replaceRates(rates)
        val updated = rates.map { it.copy(rate = it.rate + 0.1) }
        dao.replaceRates(updated)
        dao.ratesFor(Currency.CHF).first() shouldBe updated
    }

    "ratesFor only returns rates for the requested base" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)

        dao.replaceRates(rates)
        dao.replaceRates(rates.map { it.copy(baseCurrency = Currency.EUR, rate = 1.0) })
        dao.ratesFor(Currency.CHF).first() shouldBe rates
        dao.ratesFor(Currency.EUR).first() shouldBe
            rates.map { it.copy(baseCurrency = Currency.EUR, rate = 1.0) }
    }

    "deleteRates removes all rates for a base" {
        val database = lindenDatabase()
        val dao = FxRateDao(database.fxRateQueries)

        dao.replaceRates(rates)
        dao.deleteRates(Currency.CHF)
        dao.ratesFor(Currency.CHF).first() shouldBe emptyList()
    }
}) {
    companion object {
        private val rates = listOf(
            FxRate(Currency.CHF, Currency.EUR, 1.0669, "2026-08-13"),
            FxRate(Currency.CHF, Currency.USD, 1.2306, "2026-08-13"),
        )
    }
}
