package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

private suspend fun testDependencies(firstRun: Boolean = false): AppDependencies = AppDependencies(
    database = lindenDatabase(),
    initialTheme = ThemeMode.SYSTEM,
    initialCurrency = Currency.CHF,
    firstRun = firstRun,
    fxRatesSource = FakeFxRatesSource(),
)

class AppRootViewModelTest : StringSpec({
    "loads dependencies eagerly and reaches Ready" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies() }
            (viewModel.state.value is AppRootState.Ready) shouldBe true
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

    "completeFirstRun writes the currency, seeds, and reaches Ready" {
        onTestMain {
            val viewModel = AppRootViewModel { testDependencies(firstRun = true) }
            val dependencies = (viewModel.state.value as AppRootState.FirstRun).dependencies

            viewModel.completeFirstRun(Currency.EUR)
            // The completion runs on Dispatchers.IO; wait for the Ready emission.
            viewModel.state.first { it is AppRootState.Ready }

            SettingsDao(dependencies.database.settingsQueries).getDefaultCurrency() shouldBe Currency.EUR
            val accounts = AccountDao(dependencies.database.accountQueries).getAll().first()
            accounts.map { it.currency } shouldBe listOf(Currency.EUR, Currency.EUR)
        }
    }
})
