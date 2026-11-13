package org.sjbtimdan.linden.ui.entry

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.seed
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.enterAmount
import org.sjbtimdan.linden.ui.withEntryPoint

@OptIn(ExperimentalTestApi::class)
class EntryPointTransferTest : StringSpec({
    "switching type keeps amount and description" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            enterAmount("12.50")
            onNodeWithText("Description (optional)").performTextInput("Lunch")
            // Drop focus first: the tabs are hidden while a field is focused.
            tapOutside()
            onNodeWithText("Income").performClick()

            onNode(hasSetTextAction() and hasText("12.50")).assertIsDisplayed()
            onNode(hasSetTextAction() and hasText("Lunch")).assertIsDisplayed()
        }
    }

    "switching type swaps the type-specific fields" {
        withEntryPoint(clock = FakeClock()) { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)

            setContent {
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Category").assertIsDisplayed()
            onNodeWithText("From account").assertDoesNotExist()

            onNodeWithText("Transfer").performClick()

            onNodeWithText("From account").assertIsDisplayed()
            onNodeWithText("Category").assertDoesNotExist()
        }
    }

    "switching to transfer prefills accounts from the last transfer" {
        withEntryPoint(clock = FakeClock()) { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)
            val accounts = accountDao.getAll().first()
            entryDao.create(
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
                EntryPoint(viewModel = viewModel)
            }

            onNodeWithText("Transfer").performClick()
            waitForText("Main")
            onNodeWithText("Savings").assertIsDisplayed()
        }
    }

    "cross-currency transfer requires the received amount" {
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

            onNodeWithText("Amount (received)").assertIsDisplayed()
            onNodeWithText("Add").assertIsNotEnabled()
            // The received amount opens its own keypad.
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
        withEntryPoint(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)

            setContent {
                EntryPoint(viewModel = viewModel)
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
})
