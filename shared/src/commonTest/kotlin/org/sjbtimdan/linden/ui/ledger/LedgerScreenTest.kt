package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
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
    "displays empty state and add buttons" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
            onNodeWithText("Add Expense").assertIsDisplayed()
            onNodeWithText("Add Income").assertIsDisplayed()
            onNodeWithText("Add Transfer").assertIsDisplayed()
        }
    }

    "creating an expense via the dialog shows it in the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()
            onNodeWithText("New Expense").assertIsDisplayed()

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
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()
            onNodeWithText("Save").assertIsNotEnabled()
            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    "new entry dialog shows a date and time section" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()

            onNodeWithText("Date & time").assertIsDisplayed()
            onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    "add buttons preselect the entry type in the dialog" {
        withLedgerViewModel {accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Income").performClick()
            onNodeWithText("New Income").assertIsDisplayed()
        }
    }

    "editing an entry shows current values and saves changes" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Edit Expense").assertIsDisplayed()

            onNodeWithText("Amount").performTextClearance()
            onNodeWithText("Amount").performTextInput("5.00")
            onNodeWithText("Save").performClick()

            onNodeWithText("− 5.00").assertIsDisplayed()
            onNodeWithText("− 4.50").assertDoesNotExist()
        }
    }

    "deleting an entry from the edit dialog removes it" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Delete").performClick()

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "type filter chip narrows the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

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
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Transfer").performClick()

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

    "same-currency transfer hides the received amount field" {
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Transfer").performClick()

            onNodeWithText("Amount (sent)").performTextInput("100")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").assertDoesNotExist()
            onNodeWithText("Save").assertIsEnabled()

            onNodeWithText("Save").performClick()

            onNodeWithText("100.00").assertIsDisplayed()
            onNodeWithText("Main → Savings").assertIsDisplayed()
        }
    }

    "search narrows the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

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

            onNodeWithText("Add Expense").performClick()

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

            onNodeWithText("Add Transfer").performClick()

            onAllNodesWithText("Please enter account").assertCountEquals(2)
            onNodeWithText("Please enter category").assertDoesNotExist()
        }
    }

    "shows add-second-account link for transfer when only one account exists" {
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            var settingsNavigations = 0

            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToSettings = { settingsNavigations++ },
                )
            }

            onNodeWithText("Add Transfer").performClick()

            onNodeWithText("Please add a second account").assertIsDisplayed()
            onNodeWithText("Please enter account").assertDoesNotExist()

            onNodeWithText("Please add a second account").performClick()
            settingsNavigations shouldBe 1
        }
    }

    "to account dropdown excludes the from account" {
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Transfer").performClick()

            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()

            onNodeWithText("To account").performClick()
            onAllNodesWithText("Main").assertCountEquals(1)
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }
    "new expense dialog prefills from the last expense" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()

            // prefilled description field plus the ledger row behind the dialog
            onAllNodesWithText("Coffee").assertCountEquals(2)
            // prefilled category dropdown (row title shows the description, not the category)
            onAllNodesWithText("Groceries").assertCountEquals(1)
            // prefilled account dropdown plus the ledger row subtitle
            onAllNodesWithText("Main").assertCountEquals(2)
            // amount stays blank, so save is not enabled yet
            onNodeWithText("Save").assertIsNotEnabled()
        }
    }

    "shows description suggestions based on category, account and amount" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()
            // category and account are prefilled from the last expense; clear the
            // prefilled description so suggestions can be verified independently
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Amount").performTextInput("4.50")
            onNodeWithText("Description (optional)").performClick()

            // suggestion plus the ledger row behind the dialog
            onAllNodesWithText("Coffee").assertCountEquals(2)
        }
    }

    "selecting a description suggestion fills the field" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add Expense").performClick()
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Amount").performTextInput("4.50")
            onNodeWithText("Description (optional)").performClick()
            onAllNodesWithText("Coffee")[1].performClick()

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
        }
    }
})

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
