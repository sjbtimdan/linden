package org.sjbtimdan.linden.ui.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextContains
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

            onAllNodes(hasSetTextAction())[0].performTextInput("Main")
            onNodeWithText("USD").performClick()
            onNodeWithText("Save").performClick()

            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("USD").assertIsDisplayed()
        }
    }

    "creating an account with an initial balance persists it" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Account").performClick()
            onAllNodes(hasSetTextAction())[0].performTextInput("Main")
            onAllNodes(hasSetTextAction())[1].performTextInput("1500.50")
            onNodeWithText("Save").performClick()

            viewModel.accounts.value.single().account.initialBalance shouldBe 150_050
        }
    }

    "dialog has an initial balance field" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Account").performClick()
            onNodeWithText("Initial balance").assertIsDisplayed()
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
            viewModel.createAccount("Savings", Currency.EUR, initialBalance = 5_000)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Savings").performClick()
            onNodeWithText("Edit Account").assertIsDisplayed()
            onAllNodes(hasSetTextAction())[1].assertTextContains("50.00")
        }
    }

    "displays the current balance with its currency on each account" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 150_050)
            viewModel.createAccount("Savings", Currency.USD, initialBalance = 12_345)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("1,500.50 CHF").assertIsDisplayed()
            onNodeWithText("123.45 $").assertIsDisplayed()
        }
    }

    "displays the total in the default currency at the top" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 10_000)
            viewModel.createAccount("Savings", Currency.CHF, initialBalance = 5_000)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Total").assertIsDisplayed()
            onNodeWithText("150.00 CHF").assertIsDisplayed()
        }
    }

})
