package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerScreenTest : StringSpec({
    "shows the expense form with add disabled initially" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Amount").assertIsDisplayed()
            onNodeWithText("Date & time").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "expense is the default selected type" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Expense").assertIsSelected()
            onNodeWithText("Income").assertIsNotSelected()
            onNodeWithText("Transfer").assertIsNotSelected()
        }
    }

    "creating an expense saves it and resets the form" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Description (optional)").performTextInput("Coffee")
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            // amount is cleared for the next entry
            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            // description is prefilled from the saved entry
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            entryDao.getExpenses().first().shouldHaveSize(1)
        }
    }

    "clear resets the form without adding an entry" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            // A prior entry prefills the form, so Clear must empty it rather
            // than restoring the prefill.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            onNodeWithText("Amount").performTextInput("12.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            onNodeWithText("Add").assertIsEnabled()

            onNodeWithText("Clear").performClick()

            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("Lunch")).assertDoesNotExist()
            // not reset to the prefill from the last entry either
            onNode(hasSetTextAction() and hasText("Coffee")).assertDoesNotExist()
            onNodeWithText("Groceries").assertDoesNotExist()
            onNodeWithText("Main").assertDoesNotExist()
            entryDao.getExpenses().first().shouldHaveSize(1)
        }
    }

    "add is enabled once the form is valid" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add").assertIsNotEnabled()
            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Add").assertIsNotEnabled()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "switching type keeps amount and description" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Amount").performTextInput("12.50")
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            onNodeWithText("Income").performClick()

            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Lunch")).assertIsDisplayed()
        }
    }

    "switching type swaps the type-specific fields" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Category").assertIsDisplayed()
            onNodeWithText("From account").assertDoesNotExist()

            onNodeWithText("Transfer").performClick()

            onNodeWithText("From account").assertIsDisplayed()
            onNodeWithText("Category").assertDoesNotExist()
        }
    }

    "switching to transfer prefills accounts from the last transfer" {
        withLedgerViewModel { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)
            val accounts = accountDao.getAll().first()
            viewModel.createEntry(
                TransferEntry(0, null, "Move money", accounts.first(), 10_000, toAccount = accounts.last(), toAmount = 9_500),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            waitForText("Main")
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }

    "cross-currency transfer requires the received amount" {
        withLedgerViewModel { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            onNodeWithText("Amount (sent)").performTextInput("100")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
            onNodeWithText("Amount (received)").performTextInput("95")
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            entryDao.getTransfers().first().shouldHaveSize(1)
        }
    }

    "same-currency transfer hides the received amount field" {
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            onNodeWithText("Amount (sent)").performTextInput("100")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").assertDoesNotExist()
            onNodeWithText("Add").assertIsEnabled()
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

            onNodeWithText("Transfer").performClick()

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

            onNodeWithText("Transfer").performClick()

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

            onNodeWithText("Transfer").performClick()

            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()

            onNodeWithText("To account").performClick()
            onAllNodesWithText("Main").assertCountEquals(1)
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }

    "expense form prefills from the last expense" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Main").assertIsDisplayed()
            // amount stays blank, so add is not enabled yet
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "shows description suggestions based on category, account and amount" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            // clear the prefilled description so suggestions can be verified independently
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Amount").performTextInput("4.50")
            onNodeWithText("Description (optional)").performClick()

            waitForText("Coffee")
        }
    }

    "selecting a description suggestion fills the field" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Amount").performTextInput("4.50")
            onNodeWithText("Description (optional)").performClick()
            waitForText("Coffee")

            onNodeWithText("Coffee").performClick()

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
        }
    }

    "typing filters the description suggestions" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Cocoa", main, 450, createdAt = now.minus(1.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Cocoa")
            onNode(hasSetTextAction() and hasText("Cocoa")).performTextClearance()
            onNodeWithText("Amount").performTextInput("4.50")
            onNodeWithText("Description (optional)").performClick()
            waitForText("Coffee")

            onNodeWithText("Description (optional)").performTextInput("cof")

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Cocoa").assertDoesNotExist()
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitForText(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

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
