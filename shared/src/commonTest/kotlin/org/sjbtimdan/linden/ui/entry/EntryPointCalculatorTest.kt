package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.time.TEST_NOW
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointCalculatorTest : StringSpec({
    "type tabs hide while the amount calculator is open and return on commit" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Amount").performClick()
            waitForIdle()

            // Only the calculator keypad remains: no tabs, no form, no actions.
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithText("Expense").assertDoesNotExist()
            onNodeWithTag("saveEntry").assertDoesNotExist()

            onNodeWithText("1").performClick()
            onNodeWithText("0").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()

            onNodeWithText("Expense").assertIsDisplayed()
            onNodeWithText("Add").assertIsDisplayed()
        }
    }

    "back arrow closes the amount calculator and keeps the draft" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            enterAmount("12.50")

            // Reopen the calculator so the back arrow must close it again.
            onNodeWithText("Amount").performClick()
            waitForIdle()
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithTag("saveEntry").assertDoesNotExist()

            onNodeWithContentDescription("Back")
                .performSemanticsAction(SemanticsActions.OnClick)
            waitForIdle()

            // Back closed the keypad and kept the committed value.
            onNodeWithText("Enter").assertDoesNotExist()
            onNodeWithText("Amount").assertIsDisplayed()
            onNodeWithText("Add").assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
        }
    }

    "received amount opens its own calculator and collapses the form" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            enterAmount("100", label = "Amount (sent)")
            onNodeWithText("From account").performClick()
            onNodeWithText("Main").performClick()
            onNodeWithText("To account").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Amount (received)").performClick()
            waitForIdle()

            // The received amount opens its own keypad; the form and tabs collapse.
            onNodeWithText("Enter").assertIsDisplayed()
            onNodeWithText("Transfer").assertDoesNotExist()
            onNodeWithTag("saveEntry").assertDoesNotExist()

            onNodeWithText("9").performClick()
            onNodeWithText("5").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()

            // The committed value comes back formatted by the calculator.
            onNode(hasSetTextAction() and hasText("95.00")).assertIsDisplayed()
            onNodeWithText("Add").performClick()

            onNodeWithText("Added").assertIsDisplayed()
            entryDao.getAll().first().filterIsInstance<TransferEntry>().shouldHaveSize(1)
        }
    }

    "calculator header falls back to the field purpose without an account" {
        withEntryPoint(clock = FakeClock()) { viewModel ->
            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Amount").performClick()
            waitForIdle()

            onNodeWithTag("calculatorContextLabel").assertTextEquals("Amount")
        }
    }

    "calculator header shows the amount purpose and the account context" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = TEST_NOW))

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            waitForText("Coffee")
            onNodeWithText("Amount").performClick()
            waitForIdle()

            onNodeWithTag("calculatorContextLabel").assertTextEquals("Amount · Main · CHF")
        }
    }

    "calculator header for a transfer sent amount shows both accounts" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, _, viewModel ->
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
            onNodeWithText("Amount (sent)").performClick()
            waitForIdle()

            onNodeWithTag("calculatorContextLabel")
                .assertTextEquals("Amount (sent) · Main · CHF → Savings · €")
        }
    }

    "calculator header for the received amount shows both accounts" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, _, viewModel ->
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
            onNodeWithText("Amount (received)").performClick()
            waitForIdle()

            onNodeWithTag("calculatorContextLabel")
                .assertTextEquals("Amount (received) · Main · CHF → Savings · €")
        }
    }
})
