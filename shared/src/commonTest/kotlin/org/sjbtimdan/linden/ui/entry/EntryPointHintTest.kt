package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointHintTest : StringSpec({
    "add is enabled once the form is valid" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
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

    "the save hint stays hidden until the user edits, then names the missing fields in form order" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            // Opening the screen is not an error.
            onNodeWithText("Enter an amount").assertDoesNotExist()
            onNodeWithText("Add").assertIsNotEnabled()

            // Typing the amount flags the next box down: Category above Account.
            enterAmount("12.50")
            onNodeWithText("Choose a category").assertIsDisplayed()

            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Choose an account").assertIsDisplayed()

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Enter an amount").assertDoesNotExist()
            onNodeWithText("Choose an account").assertDoesNotExist()
            onNodeWithText("Choose a category").assertDoesNotExist()
            onNodeWithText("Add").assertIsEnabled()
        }
    }

    "the save hint does not reappear right after a successful save" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("Category").performClick()
            onNodeWithText("Groceries").performClick()
            enterAmount("12.50")
            onNodeWithText("Add").performClick()
            waitForIdle()

            // The saved entry resets the amount, but that is not an error: the
            // receipt confirms the add and the hint waits for the next edit.
            onNodeWithText("Added").assertIsDisplayed()
            onNodeWithText("Enter an amount").assertDoesNotExist()
            onNodeWithText("Add").assertIsNotEnabled()
            entryDao.getAll().first().shouldHaveSize(1)
        }
    }

    "the save hint asks for the received amount on a cross-currency transfer" {
        withEntryPoint(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            // Picking accounts is an edit, but the amount was never entered:
            // the hint flags the sent amount first, then the received one.
            onNodeWithText("Enter an amount").assertIsDisplayed()
            enterAmount("100", label = "Amount (sent)")

            onNodeWithText("Enter the received amount").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
        }
    }

    "the hint offers to create an account when none exist" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Add an account to continue").assertIsDisplayed()
            onNodeWithTag("missingRequirementAction").performClick()

            onNodeWithText("New Account").assertIsDisplayed()
            accountDao.getAll().first().shouldHaveSize(0)
        }
    }

    "the hint offers to create a category when none match the entry type" {
        withEntryPoint(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("No categories yet — create one").assertIsDisplayed()
            onNodeWithTag("missingRequirementAction").performClick()

            onNodeWithText("New Category").assertIsDisplayed()
        }
    }
})
