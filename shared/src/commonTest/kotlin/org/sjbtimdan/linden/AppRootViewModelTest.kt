package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.log.AppFailure
import org.sjbtimdan.linden.log.RecordingAppLogger
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

private suspend fun testDependencies(firstRun: Boolean = false): AppDependencies = AppDependencies(
    database = lindenDatabase(),
    initialTheme = ThemeMode.SYSTEM,
    initialCurrency = Currency.CHF,
    firstRun = firstRun,
    initialHideEntryTotal = false,
    initialLanguage = AppLanguage.SYSTEM,
    fxRatesSource = FakeFxRatesSource(),
)

class AppRootViewModelTest : StringSpec({
    "loads dependencies eagerly and reaches Ready" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies() }
            val ready = viewModel.state.value as AppRootState.Ready
            ready.justCompletedSetup shouldBe false
        }
    }

    "stays Loading until dependency creation resolves" {
        onTestMain {
            val gate = CompletableDeferred<AppDependencies>()
            val viewModel = AppRootViewModel { gate.await() }

            viewModel.state.value shouldBe AppRootState.Loading

            gate.complete(testDependencies())
            (viewModel.state.value is AppRootState.Ready) shouldBe true
        }
    }

    "reports Failed when dependency creation throws" {
        onTestMain {
            val viewModel = AppRootViewModel { throw IllegalStateException("boom") }
            viewModel.state.value shouldBe AppRootState.Failed
        }
    }

    "reports a startup failure to the logger" {
        onTestMain {
            val logger = RecordingAppLogger()
            val viewModel = AppRootViewModel(logger) { throw IllegalStateException("boom") }

            viewModel.state.value shouldBe AppRootState.Failed
            logger.reports shouldBe listOf(AppFailure.Startup)
        }
    }

    "retry recovers after a failure" {
        onTestMain {
            var attempts = 0
            val viewModel = AppRootViewModel {
                attempts++
                if (attempts == 1) throw IllegalStateException("boom")
                testDependencies()
            }

            viewModel.state.value shouldBe AppRootState.Failed

            viewModel.retry()
            (viewModel.state.value is AppRootState.Ready) shouldBe true
        }
    }

    "reaches FirstRun when dependencies report a first run" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies(firstRun = true) }
            (viewModel.state.value is AppRootState.FirstRun) shouldBe true
        }
    }

    "completeFirstRun writes language, theme and currency, seeds, and reaches Ready" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies(firstRun = true) }
            val dependencies = (viewModel.state.value as AppRootState.FirstRun).dependencies

            viewModel.completeFirstRun(AppLanguage.FRENCH, Currency.EUR, ThemeMode.DARK)
            // The completion runs on Dispatchers.IO; wait for the Ready emission.
            val ready = viewModel.state.first { it is AppRootState.Ready } as AppRootState.Ready

            val settings = SettingsDao(dependencies.database.settingsQueries)
            settings.getLanguage() shouldBe AppLanguage.FRENCH
            settings.getTheme() shouldBe ThemeMode.DARK
            settings.getDefaultCurrency() shouldBe Currency.EUR
            val accounts = AccountDao(dependencies.database.accountQueries).getAll().first()
            accounts.map { it.currency } shouldBe listOf(Currency.EUR, Currency.EUR)
            ready.startInCaptureSettings shouldBe false
            ready.justCompletedSetup shouldBe true
        }
    }

    "completeFirstRun opens payment capture when the wizard offers it" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies(firstRun = true) }

            viewModel.completeFirstRun(
                AppLanguage.ENGLISH,
                Currency.CHF,
                ThemeMode.SYSTEM,
                openCaptureSettings = true,
            )
            val ready = viewModel.state.first { it is AppRootState.Ready } as AppRootState.Ready

            ready.startInCaptureSettings shouldBe true
            ready.justCompletedSetup shouldBe true
        }
    }
})
