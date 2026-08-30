package org.sjbtimdan.linden.ui.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
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

            onAllNodes(hasSetTextAction())[1].performTextInput("Main")
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
            onAllNodes(hasSetTextAction())[1].performTextInput("Main")
            onAllNodes(hasSetTextAction())[2].performTextInput("1500.50")
            onNodeWithText("Save").performClick()

            viewModel.accounts.value.single().initialBalance shouldBe 150_050
        }
    }

    "editing an account's initial balance to a negative value persists it" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 5_000)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Main").performClick()
            onNodeWithText("Edit Account").assertIsDisplayed()
            onAllNodesWithContentDescription("Clear")[1].performClick()
            onAllNodes(hasSetTextAction())[2].performTextInput("-500")
            onNodeWithText("Save").performClick()

            viewModel.accounts.value.single().initialBalance shouldBe -50_000
        }
    }

    "entering an invalid initial balance shows a warning and does not save" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 5_000)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Main").performClick()
            onNodeWithText("Edit Account").assertIsDisplayed()
            onAllNodesWithContentDescription("Clear")[1].performClick()
            onAllNodes(hasSetTextAction())[2].performTextInput("abc")
            onNodeWithText("Save").performClick()

            onNodeWithText("Enter a valid amount").assertIsDisplayed()
            viewModel.accounts.value.single().initialBalance shouldBe 5_000
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

    "currency chips are disabled when editing an account with entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.accountsWithEntries.first { main.id in it }

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Main").performClick()
            onNodeWithText("Edit Account").assertIsDisplayed()
            onNodeWithText("Currency cannot be changed: this account has entries.").assertIsDisplayed()
            onNodeWithText("EUR").assertIsNotEnabled()
        }
    }

    "currency chips stay enabled when editing an account without entries" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Main").performClick()
            onNodeWithText("EUR").assertIsEnabled()
        }
    }

    "currency chips stay enabled when creating a new account" {
        withAccountViewModel { viewModel ->
            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Account").performClick()
            onNodeWithText("EUR").assertIsEnabled()
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

            onNodeWithContentDescription("Back").performClick()
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
            onAllNodes(hasSetTextAction())[2].assertTextContains("50.00")
        }
    }

    "displays the initial balance with its currency and an Initial balance label on each account" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 150_050)
            viewModel.createAccount("Savings", Currency.USD, initialBalance = 12_345)
            // An entry makes the current balance differ from the initial balance,
            // so the settings view must show the initial balance, not the current one.
            val main = accountDao.getAll().first().first { it.name == "Main" }
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("1,500.50 CHF").assertIsDisplayed()
            onNodeWithText("123.45 $").assertIsDisplayed()
            onAllNodes(hasText("Initial balance")).assertCountEquals(2)
        }
    }

    "search filters the account list and clears on the clear button" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            viewModel.createAccount("Savings", Currency.USD)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onAllNodes(hasSetTextAction())[0].performTextInput("main")

            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Savings").assertDoesNotExist()

            onNodeWithContentDescription("Clear").performClick()

            onNodeWithText("Savings").assertIsDisplayed()
        }
    }

    "search with no matches shows the no-matches message" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)

            setContent {
                AccountListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onAllNodes(hasSetTextAction())[0].performTextInput("nonexistent")

            onNodeWithText("No matching accounts.").assertIsDisplayed()
        }
    }
})
