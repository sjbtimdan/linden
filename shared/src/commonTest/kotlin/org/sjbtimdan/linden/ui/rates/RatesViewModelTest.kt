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
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.FxRates
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Instant

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

            val rates = withTimeout(5_000.milliseconds) { viewModel.rates.first { it.isNotEmpty() } }
            rates.size shouldBe Currency.entries.size - 1
            rates.all { it.baseCurrency == Currency.CHF } shouldBe true
            viewModel.ratesRefreshState.value shouldBe RatesRefreshState.Idle
        }
    }

    "refreshRates fetches and caches rates for the given currency" {
        onTestMain {
            val database = lindenDatabase()
            val dao = FxRateDao(database.fxRateQueries)
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(dao, FakeFxRatesSource()),
            )

            viewModel.refreshRates(Currency.USD)

            val rates = withTimeout(5_000.milliseconds) { dao.ratesFor(Currency.USD).first() }
            rates.isNotEmpty() shouldBe true
            rates.all { it.baseCurrency == Currency.USD } shouldBe true
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

            val state = withTimeout(5_000.milliseconds) {
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

            val rates = withTimeout(5_000.milliseconds) {
                viewModel.rates.first { it.isNotEmpty() && it.first().baseCurrency == Currency.EUR }
            }
            rates.size shouldBe Currency.entries.size - 1
            rates.all { it.baseCurrency == Currency.EUR } shouldBe true
        }
    }

    "refreshRatesIfStale fetches when no rates are cached" {
        onTestMain {
            val database = lindenDatabase()
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.refreshRatesIfStale()

            val rates = withTimeout(5_000.milliseconds) { viewModel.rates.first { it.isNotEmpty() } }
            rates.size shouldBe Currency.entries.size - 1
            source.fetchCount shouldBe 1
        }
    }

    "refreshRatesIfStale fetches when the cached rates are older than 24 hours" {
        onTestMain {
            val database = lindenDatabase()
            val dao = FxRateDao(database.fxRateQueries)
            dao.replaceRates(seededRates, NOW.minus(25.hours).toEpochMilliseconds())
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(dao, source, { NOW }),
            )

            viewModel.refreshRatesIfStale()

            val refreshed = withTimeout(5_000.milliseconds) {
                viewModel.rates.first { it.isNotEmpty() && it.first().rate == 1.0 }
            }
            refreshed.size shouldBe Currency.entries.size - 1
            source.fetchCount shouldBe 1
        }
    }

    "refreshRatesIfStale reports an error when the source fails" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource { error("network") },
                    { NOW },
                ),
            )

            viewModel.refreshRatesIfStale()

            val state = withTimeout(5_000.milliseconds) {
                viewModel.ratesRefreshState.first { it is RatesRefreshState.Error }
            }
            (state as RatesRefreshState.Error).message shouldBe "network"
        }
    }

    "a failed startup refresh warns when no rates are cached" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource { error("network") },
                    { NOW },
                ),
            )

            viewModel.refreshRatesIfStale()

            val warning = withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it != null } }
            warning shouldBe RatesWarning.Missing
        }
    }

    "a failed startup refresh warns when cached rates are over a week old" {
        onTestMain {
            val database = lindenDatabase()
            val dao = FxRateDao(database.fxRateQueries)
            dao.replaceRates(seededRates, NOW.minus(8.days).toEpochMilliseconds())
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    dao,
                    FakeFxRatesSource { error("network") },
                    { NOW },
                ),
                clock = { NOW },
            )

            viewModel.refreshRatesIfStale()

            val warning = withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it != null } }
            warning shouldBe RatesWarning.Outdated
        }
    }

    "a failed startup refresh does not warn when cached rates are recent" {
        onTestMain {
            val database = lindenDatabase()
            val dao = FxRateDao(database.fxRateQueries)
            dao.replaceRates(seededRates, NOW.minus(2.days).toEpochMilliseconds())
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    dao,
                    FakeFxRatesSource { error("network") },
                    { NOW },
                ),
                clock = { NOW },
            )

            viewModel.refreshRatesIfStale()

            withTimeout(5_000.milliseconds) {
                viewModel.ratesRefreshState.first { it is RatesRefreshState.Error }
            }
            viewModel.ratesWarning.value shouldBe null
        }
    }

    "a successful refresh clears the rates warning" {
        onTestMain {
            val database = lindenDatabase()
            var fail = true
            val source = FakeFxRatesSource { base ->
                if (fail) {
                    error("network")
                } else {
                    FxRates(
                        base = base,
                        date = "2026-08-13",
                        rates = Currency.entries.filter { it != base }.associateWith { 1.0 },
                    )
                }
            }
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.refreshRatesIfStale()
            withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it != null } }

            fail = false
            viewModel.refreshRates()
            withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it == null } }
            viewModel.ratesWarning.value shouldBe null
        }
    }

    "setting a rate manually clears the rates warning" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource { error("network") },
                    { NOW },
                ),
            )

            viewModel.refreshRatesIfStale()
            withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it != null } }

            viewModel.setRate(Currency.EUR, 1.5)
            withTimeout(5_000.milliseconds) { viewModel.ratesWarning.first { it == null } }
            viewModel.ratesWarning.value shouldBe null
        }
    }

    "setRate exposes the manually edited rate for the base currency" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
            )

            viewModel.setRate(Currency.EUR, 1.5)

            val rate = withTimeout(5_000.milliseconds) {
                viewModel.rates.first { it.any { r -> r.quoteCurrency == Currency.EUR } }
                    .first { it.quoteCurrency == Currency.EUR }
            }
            rate.baseCurrency shouldBe Currency.CHF
            rate.rate shouldBe 1.5
        }
    }

    "refreshRatesIfStale does not fetch when automatic updates are disabled" {
        onTestMain {
            val database = lindenDatabase()
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.setAutoUpdateRates(false)
            withTimeout(5_000.milliseconds) { viewModel.autoUpdateRates.first { !it } }

            viewModel.refreshRatesIfStale()

            source.fetchCount shouldBe 0
        }
    }

    "refreshRatesIfStale fetches again after automatic updates are re-enabled" {
        onTestMain {
            val database = lindenDatabase()
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.setAutoUpdateRates(false)
            withTimeout(5_000.milliseconds) { viewModel.autoUpdateRates.first { !it } }
            viewModel.refreshRatesIfStale()
            source.fetchCount shouldBe 0

            viewModel.setAutoUpdateRates(true)
            withTimeout(5_000.milliseconds) { viewModel.autoUpdateRates.first { it } }
            viewModel.refreshRatesIfStale()

            withTimeout(5_000.milliseconds) { viewModel.rates.first { it.isNotEmpty() } }
            source.fetchCount shouldBe 1
        }
    }

    "a default currency change does not refresh when automatic updates are disabled" {
        onTestMain {
            val database = lindenDatabase()
            val settingsDao = SettingsDao(database.settingsQueries)
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = settingsDao,
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.setAutoUpdateRates(false)
            withTimeout(5_000.milliseconds) { viewModel.autoUpdateRates.first { !it } }

            settingsDao.setDefaultCurrency(Currency.EUR)
            withTimeout(5_000.milliseconds) { viewModel.base.first { it == Currency.EUR } }

            source.fetchCount shouldBe 0
        }
    }

    "a manual refresh still works when automatic updates are disabled" {
        onTestMain {
            val database = lindenDatabase()
            val source = FakeFxRatesSource()
            val viewModel = RatesViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                fxRatesRepository = FxRatesRepository(FxRateDao(database.fxRateQueries), source, { NOW }),
            )

            viewModel.setAutoUpdateRates(false)
            withTimeout(5_000.milliseconds) { viewModel.autoUpdateRates.first { !it } }

            viewModel.refreshRates()

            withTimeout(5_000.milliseconds) { viewModel.rates.first { it.isNotEmpty() } }
            source.fetchCount shouldBe 1
        }
    }
}) {
    companion object {
        private val NOW = Instant.parse("2026-08-13T12:00:00Z")
        private val seededRates = listOf(
            FxRate(Currency.CHF, Currency.EUR, 1.0669, "2026-08-12"),
        )
    }
}
