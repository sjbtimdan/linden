package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
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
import kotlin.time.Clock
import kotlin.time.Duration.Companion.days

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

    "hero card shows the total balance across accounts" {
        withLedgerViewModel { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 12_345)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Total balance").assertIsDisplayed()
            onNodeWithText("123.45").assertIsDisplayed()
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

            enterAmount("12.50")
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Description (optional)").performTextInput("Coffee")
            // typing the description collapses the form and hides the type tabs;
            // tapping outside drops focus and brings the buttons back
            onNodeWithText("Expense").assertDoesNotExist()
            tapOutside()
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            // amount is cleared for the next entry
            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            // description is prefilled from the saved entry
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldHaveSize(1)
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
            enterAmount("12.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            tapOutside()
            onNodeWithText("Add").assertIsEnabled()

            onNodeWithText("Clear").performClick()

            onNode(hasSetTextAction() and hasText("12.50")).assertDoesNotExist()
            onNode(hasSetTextAction() and hasText("Lunch")).assertDoesNotExist()
            // not reset to the prefill from the last entry either
            onNode(hasSetTextAction() and hasText("Coffee")).assertDoesNotExist()
            onNodeWithText("Groceries").assertDoesNotExist()
            onNodeWithText("Main").assertDoesNotExist()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldHaveSize(1)
        }
    }

    "back arrow while editing expands the form and keeps the draft" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Lunch")

            // typing the description collapses the form and hides the buttons
            onNodeWithText("Add").assertDoesNotExist()

            // invoke the click action directly; the skiko mouse-click path misses
            // the button when the form is collapsed, so a gesture-level tap here
            // would be testing the input injection rather than the back logic
            onNodeWithContentDescription("Back")
                .performSemanticsAction(SemanticsActions.OnClick)

            // the form expands with the draft preserved — Clear is the full reset
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Lunch")).assertIsDisplayed()
            onNodeWithText("Add").assertIsDisplayed()
            entryDao.getAll().first().shouldHaveSize(0)
        }
    }

    "add is enabled once the form is valid" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Add").assertIsNotEnabled()
            enterAmount("12.50")
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

            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            // the type tabs are hidden while a field is focused; drop focus first
            tapOutside()
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
                TransferEntry(
                    0,
                    null,
                    "Move money",
                    accounts.first(),
                    10_000,
                    toAccount = accounts.last(),
                    toAmount = 9_500,
                ),
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
            enterAmount("100", label = "Amount (sent)")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
            // focusing the received amount opens its own calculator
            onNodeWithText("Amount (received)").performClick()
            waitForIdle()
            onNodeWithText("9").performClick()
            onNodeWithText("5").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            entryDao.getAll().first().filterIsInstance<TransferEntry>().shouldHaveSize(1)
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
            enterAmount("100", label = "Amount (sent)")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").assertDoesNotExist()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "type tabs hide while the amount calculator is open and return on commit" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Amount").performClick()
            waitForIdle()

            // only the calculator keypad remains: no tabs, no form, no actions
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithText("Expense").assertDoesNotExist()
            onNodeWithText("Add").assertDoesNotExist()

            onNodeWithText("1").performClick()
            onNodeWithText("0").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()

            onNodeWithText("Expense").assertIsDisplayed()
            onNodeWithText("Add").assertIsDisplayed()
        }
    }

    "back arrow closes the amount calculator and keeps the draft" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            enterAmount("12.50")

            // reopen the calculator; the back arrow must close it again
            onNodeWithText("Amount").performClick()
            waitForIdle()
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithText("Add").assertDoesNotExist()

            onNodeWithContentDescription("Back")
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            // calculator gone, form expanded, committed value untouched
            onNodeWithText("Enter").assertDoesNotExist()
            onNodeWithText("Amount").assertIsDisplayed()
            onNodeWithText("Add").assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
        }
    }

    "received amount opens its own calculator and collapses the form" {
        withLedgerViewModel { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            enterAmount("100", label = "Amount (sent)")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").performClick()
            waitForIdle()

            // the received amount gets its own keypad; the form and tabs collapse
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithText("Transfer").assertDoesNotExist()
            onNodeWithText("Add").assertDoesNotExist()

            onNodeWithText("9").performClick()
            onNodeWithText("5").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()

            // the committed value comes back formatted by the calculator
            onNode(hasSetTextAction() and hasText("95.00")).assertIsDisplayed()
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            entryDao.getAll().first().filterIsInstance<TransferEntry>().shouldHaveSize(1)
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
            // Focusing To account collapses the form, so the From field is gone too;
            // Main must appear nowhere, i.e. it is never offered as a to-account option.
            onNodeWithText("Main").assertDoesNotExist()
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
            enterAmount("4.50")
            // clear the prefilled description so suggestions can be verified independently;
            // clearing focuses the field, which collapses the rest of the form
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()

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
            enterAmount("4.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            waitForText("Coffee")

            onNodeWithText("Coffee").performClick()

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            // the suggestion row is dismissed, leaving the field and the
            // quick-entry chip (which also shows "Coffee")
            onAllNodesWithText("Coffee").assertCountEquals(2)
        }
    }

    "tapping outside the description field clears focus and hides suggestions" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            waitForText("Coffee")

            onNodeWithText("Description (optional)").assertIsFocused()

            // focusing the description collapses the rest of the form and hides the
            // type tabs; tapping outside drops focus and brings the fields back
            onNodeWithText("Expense").assertDoesNotExist()
            tapOutside()

            onNodeWithText("Description (optional)").assertIsNotFocused()
            // no suggestion was applied to the field; the persistent quick-entry
            // chip still shows the description
            onNode(hasSetTextAction() and hasText("Coffee")).assertDoesNotExist()
            onNodeWithText("Quick entry").assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("4.50")).assertIsDisplayed()
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
            enterAmount("4.50")
            onNode(hasSetTextAction() and hasText("Cocoa")).performTextClearance()
            waitForText("Coffee")

            onNodeWithText("Description (optional)").performTextInput("cof")

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Cocoa").assertDoesNotExist()
        }
    }

    "shows predicted account and category chips from history" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Leisure", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val categories = categoryDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val savings = accounts.first { it.name == "Savings" }
            val groceries = categories.first { it.name == "Groceries" }
            val leisure = categories.first { it.name == "Leisure" }
            val now = Clock.System.now()
            // The pairings prediction must rediscover from history.
            viewModel.createEntry(ExpenseEntry(0, leisure, "Cinema", savings, 2_000, createdAt = now.minus(5.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            // Clear the prefilled form so the account/category fields start empty.
            onNodeWithText("Clear").performClick()
            enterAmount("4.50")

            // Category: the predicted chip leads the full list, highlighted.
            onNodeWithText("Category").performClick()
            onNode(hasText("Groceries") and hasContentDescription("Recommended")).assertIsDisplayed()
            onNodeWithText("Leisure").assertIsDisplayed()
            onNode(hasText("Leisure") and hasContentDescription("Recommended")).assertDoesNotExist()

            // Account: picking the category narrows the prediction to its pairing.
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Account").performClick()
            onNode(hasText("Main") and hasContentDescription("Recommended")).assertIsDisplayed()
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }

    "prefilled account and category fall back to the full list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Leisure", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val categories = categoryDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val groceries = categories.first { it.name == "Groceries" }
            val now = Clock.System.now()
            // The latest entry prefills the form with account=Main, category=Groceries.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")

            // The only predicted category is the already-selected one, so it is
            // excluded from the predictions and the full list is shown instead.
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Leisure").assertIsDisplayed()
            onNode(hasText("Groceries") and hasContentDescription("Recommended")).assertDoesNotExist()
        }
    }

    "shows quick entry chips for the current type" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Quick entry")
            onNodeWithText("Quick entry").assertIsDisplayed()
            // the chip duplicates the prefilled description field
            onAllNodesWithText("Coffee").assertCountEquals(2)
        }
    }

    "selecting a quick entry fills the form" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = Clock.System.now()
            // The latest entry prefills the form; the older one only exists as a chip.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Train", main, 450, createdAt = now))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Quick entry")
            // prefill comes from the latest entry, so "Coffee" is only the chip
            onNode(hasSetTextAction() and hasText("Train")).assertIsDisplayed()
            onNodeWithText("Coffee").performClick()
            waitForIdle()

            onNode(hasSetTextAction() and hasText("4.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "quick entry chips hide for a type without matching history" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Clock.System.now()))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            waitForText("Quick entry")
            onNodeWithText("Income").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Quick entry").fetchSemanticsNodes().isEmpty()
            }
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.waitForText(text: String) {
    waitUntil(timeoutMillis = 5_000) {
        onAllNodesWithText(text).fetchSemanticsNodes().isNotEmpty()
    }
}

/**
 * Taps empty space to drop focus. While a field is focused the form collapses,
 * so the bottom-left area of the screen is just dead space; the tap reaches the
 * screen's tap-to-dismiss gesture, which is what real users do to close the
 * keyboard and bring the full form back.
 */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.tapOutside() {
    onRoot().performTouchInput {
        click(Offset(centerX * 0.2f, bottom - 40f))
    }
}

/** Enters an amount via the calculator keypad and commits it with Enter. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.enterAmount(text: String, label: String = "Amount") {
    onNodeWithText(label).performClick()
    waitForIdle()
    text.forEach { char ->
        onNodeWithText(char.toString()).performClick()
    }
    onNodeWithText("Enter").performClick()
    waitForIdle()
}

private suspend fun seed(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
