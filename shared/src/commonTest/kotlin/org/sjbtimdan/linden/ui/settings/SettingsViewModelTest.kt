package org.sjbtimdan.linden.ui.settings

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import java.io.ByteArrayInputStream
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.imports.buildIvyZip
import org.sjbtimdan.linden.imports.minimalIvyJson
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

class SettingsViewModelTest : StringSpec({
    "setThemeMode(LIGHT) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                dao,
                IvyImporter(database),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.themeMode.value shouldBe ThemeMode.SYSTEM

            viewModel.setThemeMode(ThemeMode.LIGHT)

            dao.getTheme() shouldBe ThemeMode.LIGHT
        }
    }

    "setDefaultCurrency(EUR) updates the database" {
        onTestMain {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                dao,
                IvyImporter(database),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.defaultCurrency.value shouldBe Currency.CHF

            viewModel.setDefaultCurrency(Currency.EUR)

            dao.getDefaultCurrency() shouldBe Currency.EUR
        }
    }

    "importIvy imports a valid backup and reports a success result" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))

            val state = withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Success }
            }

            val result = (state as ImportState.Success).result
            result.accounts shouldBe 3
            result.categories shouldBe 3
            result.transactions shouldBe 5
        }
    }

    "importIvy reports an error for an invalid backup and leaves the database untouched" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip("this is not json")))

            val state = withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Error }
            }

            (state as ImportState.Error).message shouldContain "not a valid Ivy backup"
            database.accountQueries.selectAll().awaitAsList() shouldBe emptyList()
        }
    }

    "clearImportState resets the import state to Idle" {
        onTestMain {
            val database = lindenDatabase()
            val viewModel = SettingsViewModel(
                settingsDao = SettingsDao(database.settingsQueries),
                importer = IvyImporter(database),
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    FakeFxRatesSource(),
                ),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
            )

            viewModel.importIvy(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))
            withTimeout(5_000) {
                viewModel.importState.first { it is ImportState.Success }
            }

            viewModel.clearImportState()

            viewModel.importState.value shouldBe ImportState.Idle
        }
    }
})
