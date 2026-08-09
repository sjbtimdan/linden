package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerScreenTest : StringSpec({
    "displays empty state and new entry button" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
            onNodeWithText("+ New Entry").assertIsDisplayed()
        }
    }

    "creating an expense via the dialog shows it in the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("+ New Entry").performClick()
            onNodeWithText("New Entry").assertIsDisplayed()

            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Save").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("− 12.50").assertIsDisplayed()
        }
    }

    "save is disabled until the form is valid" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("+ New Entry").performClick()
            onNodeWithText("Save").assertIsNotEnabled()
            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    "editing an entry shows current values and saves changes" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Edit Entry").assertIsDisplayed()

            onNodeWithText("Amount").performTextClearance()
            onNodeWithText("Amount").performTextInput("5.00")
            onNodeWithText("Save").performClick()

            onNodeWithText("− 5.00").assertIsDisplayed()
            onNodeWithText("− 4.50").assertDoesNotExist()
        }
    }

    "deleting an entry from the edit dialog removes it" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Delete").performClick()

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "type filter chip narrows the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000, Currency.CHF))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Refund").assertIsDisplayed()

            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "creating a transfer via the dialog shows it in the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("+ New Entry").performClick()
            onNode(hasText("Transfer") and hasRole(Role.RadioButton)).performClick()

            onNodeWithText("Amount (sent)").performTextInput("100")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()
            onNodeWithText("Amount (received)").performTextInput("95")
            onNodeWithText("Description (optional)").performTextInput("Move money")
            onNodeWithText("Save").performClick()

            onNodeWithText("Move money").assertIsDisplayed()
            onNodeWithText("Main → Savings").assertIsDisplayed()
            onNodeWithText("100.00").assertIsDisplayed()
        }
    }

    "search narrows the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200, Currency.CHF))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Search").performTextInput("lunch")

            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "shows settings links when no accounts or categories exist" {
        withLedgerViewModel { viewModel ->
            var settingsNavigations = 0
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { settingsNavigations++ },
                )
            }

            onNodeWithText("+ New Entry").performClick()

            onNodeWithText("Please enter category").assertIsDisplayed()
            onNodeWithText("Please enter account").assertIsDisplayed()

            onNodeWithText("Please enter category").performClick()
            onNodeWithText("Please enter account").performClick()

            settingsNavigations shouldBe 2
        }
    }

    "shows account link for transfer fields when no accounts exist" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = {},
                )
            }

            onNodeWithText("+ New Entry").performClick()
            onNode(hasText("Transfer") and hasRole(Role.RadioButton)).performClick()

            onAllNodesWithText("Please enter account").assertCountEquals(2)
            onNodeWithText("Please enter category").assertDoesNotExist()
        }
    }

    "shows add-second-account link for transfer when only one account exists" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            var settingsNavigations = 0

            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { settingsNavigations++ },
                )
            }

            onNodeWithText("+ New Entry").performClick()
            onNode(hasText("Transfer") and hasRole(Role.RadioButton)).performClick()

            onNodeWithText("Please add a second account").assertIsDisplayed()
            onNodeWithText("Please enter account").assertDoesNotExist()

            onNodeWithText("Please add a second account").performClick()
            settingsNavigations shouldBe 1
        }
    }

    "to account dropdown excludes the from account" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("+ New Entry").performClick()
            onNode(hasText("Transfer") and hasRole(Role.RadioButton)).performClick()

            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()

            onNodeWithText("To account").performClick()
            onAllNodesWithText("Main").assertCountEquals(1)
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }
})

private fun hasRole(role: Role): SemanticsMatcher =
    SemanticsMatcher.expectValue(SemanticsProperties.Role, role)

private suspend fun seed(
    accountDao: AccountDao,
    categoryDao: CategoryDao,
): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
