package org.sjbtimdan.linden.ui.rates

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.isToggleable
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withTimeout
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.model.Currency
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
            onAllNodesWithText("1 CHF = —").assertCountEquals(Currency.entries.size - 1)
            onNodeWithContentDescription("Edit EUR rate").assertExists()
            onNodeWithText("Refresh").performClick()

            withTimeout(5_000) { viewModel.rates.first { it.isNotEmpty() } }

            onNodeWithText("Rates from 2026-08-13").assertExists()
            onNodeWithText("1 CHF = 1 €").assertExists()
            onNodeWithText("1 CHF = 1 $").assertExists()
        }
    }

    "editing a rate saves the new value" {
        withRatesViewModel { _, viewModel ->
            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithContentDescription("Edit EUR rate").performClick()
            onNode(hasSetTextAction()).performTextInput("1.25")
            onNodeWithText("Save").performClick()

            withTimeout(5_000) {
                viewModel.rates.first { it.any { r -> r.quoteCurrency == Currency.EUR } }
            }
            onNodeWithText("1 CHF = 1.25 €").assertExists()
        }
    }

    "invalid rate input cannot be saved" {
        withRatesViewModel { _, viewModel ->
            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithContentDescription("Edit EUR rate").performClick()
            onNode(hasSetTextAction()).performTextInput("abc")
            onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    "toggling update automatically off persists the setting" {
        withRatesViewModel { settingsDao, viewModel ->
            setContent {
                RatesScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNode(isToggleable()).assertIsOn()
            onNode(isToggleable()).performClick()
            onNode(isToggleable()).assertIsOff()

            settingsDao.getAutoUpdateRates() shouldBe false
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
})
