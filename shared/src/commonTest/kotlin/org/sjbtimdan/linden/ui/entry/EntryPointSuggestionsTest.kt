package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.withEntryPoint
import kotlin.time.Duration.Companion.days

@OptIn(ExperimentalTestApi::class)
class EntryPointSuggestionsTest : StringSpec({
    "expense form prefills from the last expense" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Main").assertIsDisplayed()
            // The amount stays blank, so Add is not enabled yet.
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "shows description suggestions based on category, account and amount" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = TEST_NOW
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")
            // Clear the prefilled description (clearing focuses the field, showing suggestions).
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()

            waitForText("Coffee")
        }
    }

    "selecting a description suggestion fills the field" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = TEST_NOW
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(3.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            waitForText("Coffee")

            onNodeWithText("Coffee").performClick()

            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            // The suggestion row is gone; the field shows "Coffee" and the chip adds its amount.
            onNodeWithText("Coffee · 4.50").assertIsDisplayed()
        }
    }

    "tapping outside the description field clears focus and hides suggestions" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = TEST_NOW
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(3.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")
            onNode(hasSetTextAction() and hasText("Coffee")).performTextClearance()
            waitForText("Coffee")

            onNodeWithText("Description (optional)").assertIsFocused()

            // The description is focused, so the tabs are hidden until tapOutside drops focus.
            onNodeWithText("Expense").assertDoesNotExist()
            tapOutside()

            onNodeWithText("Description (optional)").assertIsNotFocused()
            // Nothing was applied — only the quick-entry chip still shows the entry.
            onNode(hasSetTextAction() and hasText("Coffee")).assertDoesNotExist()
            onNodeWithText("Quick entry").assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("4.50")).assertIsDisplayed()
        }
    }

    "typing filters the description suggestions" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val now = TEST_NOW
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Cocoa", main, 450, createdAt = now.minus(1.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
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
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
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
            val now = TEST_NOW
            // The pairing prediction must be rediscovered from history.
            viewModel.createEntry(ExpenseEntry(0, leisure, "Cinema", savings, 2_000, createdAt = now.minus(5.days)))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
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
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Leisure", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val categories = categoryDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val groceries = categories.first { it.name == "Groceries" }
            val now = TEST_NOW
            // The latest entry prefills the form with account=Main, category=Groceries.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = now.minus(2.days)))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            enterAmount("4.50")

            // The only predicted category is the already-selected one, so it is excluded
            // from the predictions and the full list is shown instead.
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Leisure").assertIsDisplayed()
            onNode(hasText("Groceries") and hasContentDescription("Recommended")).assertDoesNotExist()
        }
    }

    "shows quick entry chips for the current type" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val yesterday = TEST_NOW.minus(1.days)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Quick entry")
            onNodeWithText("Quick entry").assertIsDisplayed()
            // The field shows the prefilled description; the chip adds its amount.
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Coffee · 4.50").assertIsDisplayed()
        }
    }

    "selecting a quick entry fills the form" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val yesterday = TEST_NOW.minus(1.days)
            // Two occurrences of each description so the frequency filter passes.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Train", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Train", main, 450, createdAt = yesterday))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Quick entry")
            onNodeWithText("Coffee · 4.50").performClick()
            waitForIdle()

            onNode(hasSetTextAction() and hasText("4.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "selecting a quick entry keeps the amount the user entered" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val twoDaysAgo = TEST_NOW.minus(2.days)
            val yesterday = TEST_NOW.minus(1.days)
            // Two occurrences of each description so the frequency filter passes;
            // Train is the most recent, so the form is prefilled with it and
            // "Coffee" only labels the quick-entry chip.
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = twoDaysAgo))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = twoDaysAgo))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Train", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Train", main, 450, createdAt = yesterday))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Quick entry")
            enterAmount("9.99")
            onNodeWithText("Coffee · 4.50").performClick()
            waitForIdle()

            // The amount survives; the chip still fills the rest of the form.
            onNode(hasSetTextAction() and hasText("9.99")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Coffee")).assertIsDisplayed()
        }
    }

    "quick entry chips hide for a type without matching history" {
        withEntryPoint(clock = FakeClock(now = TEST_NOW)) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            val yesterday = TEST_NOW.minus(1.days)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = yesterday))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Quick entry")
            onNodeWithText("Income").performClick()
            waitUntil(timeoutMillis = 5_000) {
                onAllNodesWithText("Quick entry").fetchSemanticsNodes().isEmpty()
            }
        }
    }
})
