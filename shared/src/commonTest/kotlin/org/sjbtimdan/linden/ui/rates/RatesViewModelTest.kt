package org.sjbtimdan.linden.ui.rates

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

class RatesViewModelTest : StringSpec({
    "refreshRates fetches and exposes cached rates for the default currency" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
            )

            viewModel.refreshRates()

            val rates = withTimeout(5_000) { viewModel.rates.first { it.isNotEmpty() } }
            rates.size shouldBe Currency.entries.size - 1
            rates.all { it.baseCurrency == Currency.CHF } shouldBe true
            viewModel.ratesRefreshState.value shouldBe RatesRefreshState.Idle
        }
    }

    "refreshRates reports an error when the source fails" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource { error("network") },
                ),
            )

            viewModel.refreshRates()

            val state = withTimeout(5_000) {
                viewModel.ratesRefreshState.first { it is RatesRefreshState.Error }
            }
            (state as RatesRefreshState.Error).message shouldBe "network"
        }
    }

    "a default currency change refreshes rates for the new base" {
        onTestMain {
            val database = lindenDatabase()
            val settingsDao = SettingsDao(database.settingsQueries)
            val viewModel = RatesViewModel(
                settingsDao = settingsDao,
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
            )

            settingsDao.setDefaultCurrency(Currency.EUR)

            val rates = withTimeout(5_000) {
                viewModel.rates.first { it.isNotEmpty() && it.first().baseCurrency == Currency.EUR }
            }
            rates.size shouldBe Currency.entries.size - 1
            rates.all { it.baseCurrency == Currency.EUR } shouldBe true
        }
    }
})
