package org.sjbtimdan.linden.ui.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.withAccountViewModel

@OptIn(ExperimentalTestApi::class)
class AccountListScreenTest : StringSpec({
    "displays empty state when no accounts" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("No accounts yet.").assertIsDisplayed()
            onNodeWithText("+ New Account").assertIsDisplayed()
        }
    }

    "creating an account via dialog shows it in the list" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Account").performClick()
            onNodeWithText("New Account").assertIsDisplayed()

            onNode(hasSetTextAction()).performTextInput("Main")
            onNodeWithText("USD").performClick()
            onNodeWithText("Save").performClick()

            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("USD").assertIsDisplayed()
        }
    }

    "dialog offers all currencies as selectable chips" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Account").performClick()
            Currency.entries.forEach { currency ->
                onNodeWithText(currency.name).assertIsDisplayed()
            }
        }
    }

    "back button triggers navigation" {
        withAccountViewModel { viewModel ->
            var navigatedBack = false

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigatedBack = true },
                )
            }

            onNodeWithText("< Settings").performClick()
            navigatedBack shouldBe true
        }
    }

    "opening edit dialog shows current values" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Savings", Currency.EUR)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Savings").performClick()
            onNodeWithText("Edit Account").assertIsDisplayed()
        }
    }

})
