package org.sjbtimdan.linden.ui.rates

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.ui.withRatesViewModel

@OptIn(ExperimentalTestApi::class)
class RatesScreenTest : StringSpec({
    "clicking Refresh loads and shows the rate rows for the base currency" {
        withRatesViewModel { viewModel ->
            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("1 CHF").assertExists()
            onNodeWithText("No rates loaded yet.").assertExists()
            onNodeWithText("Refresh").performClick()

            withTimeout(5_000) { viewModel.rates.first { it.isNotEmpty() } }

            onNodeWithText("Rates from 2026-08-13").assertExists()
            onNodeWithText("1 CHF = 1 €").assertExists()
            onNodeWithText("1 CHF = 1 $").assertExists()
        }
    }

    "a failed refresh shows an error row that can be dismissed" {
        withRatesViewModel(fxRatesSource = FakeFxRatesSource { error("network") }) { _, viewModel ->
            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Refresh").performClick()

            withTimeout(5_000) {
                viewModel.ratesRefreshState.first { it is RatesRefreshState.Error }
            }
            onNodeWithText("Refresh failed: network").assertExists()

            onNodeWithText("Dismiss").performClick()
            onAllNodesWithText("Refresh failed: network").assertCountEquals(0)
        }
    }

    "clicking back triggers navigation" {
        withRatesViewModel { viewModel ->
            var navigatedBack = false

            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigatedBack = true },
                )
            }
            onNodeWithText("< Settings").performClick()
            navigatedBack shouldBe true
        }
    }

    "formatRate rounds to 4 decimals and trims trailing zeros" {
        formatRate(1.0) shouldBe "1"
        formatRate(1.0669) shouldBe "1.0669"
        formatRate(0.93737) shouldBe "0.9374"
        formatRate(2.5) shouldBe "2.5"
    }
})
