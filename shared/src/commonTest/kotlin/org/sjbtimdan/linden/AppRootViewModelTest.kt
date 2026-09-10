package org.sjbtimdan.linden

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CompletableDeferred
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.onTestMain

private suspend fun testDependencies(): AppDependencies = AppDependencies(
    database = lindenDatabase(),
    initialTheme = ThemeMode.SYSTEM,
    initialCurrency = Currency.CHF,
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
})
