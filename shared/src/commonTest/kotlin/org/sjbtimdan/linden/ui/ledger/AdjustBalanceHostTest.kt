package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class AdjustBalanceHostTest : StringSpec({
    "a higher target with a chosen category saves an income adjustment" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()

            var dismissed = false
            setContent {
                var state by remember {
                    mutableStateOf(
                        AdjustBalanceDialogState(
                            account = AccountWithBalance(main, 10_000),
                            currentBalance = 10_000,
                            targetBalanceText = "100.00",
                        ),
                    )
                }
                AdjustBalanceHost(
                    viewModel = viewModel,
                    state = state,
                    hideAmounts = false,
                    onStateChange = { state = it },
                    onDismiss = { dismissed = true },
                )
            }

            waitForIdle()
            onAllNodes(hasSetTextAction())[0].performTextClearance()
            onAllNodes(hasSetTextAction())[0].performTextInput("125.00")
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Adjust").performClick()
            waitForIdle()

            dismissed shouldBe true
            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(1)
            entries.first().amount shouldBe 2_500
        }
    }

    "the confirm is disabled until a category is chosen" {
        withLedgerViewModel(clock = FakeClock()) { _, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()

            setContent {
                var state by remember {
                    mutableStateOf(
                        AdjustBalanceDialogState(
                            account = AccountWithBalance(main, 10_000),
                            currentBalance = 10_000,
                            targetBalanceText = "100.00",
                        ),
                    )
                }
                AdjustBalanceHost(
                    viewModel = viewModel,
                    state = state,
                    hideAmounts = false,
                    onStateChange = { state = it },
                    onDismiss = {},
                )
            }

            waitForIdle()
            onAllNodes(hasSetTextAction())[0].performTextClearance()
            onAllNodes(hasSetTextAction())[0].performTextInput("125.00")

            onNodeWithText("Adjust").assertIsNotEnabled()

            onNodeWithText("Groceries").performClick()

            onNodeWithText("Adjust").assertIsEnabled()
        }
    }

    "dismissing reports without writing" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            val main = accountDao.getAll().first().first()

            var dismissed = false
            setContent {
                AdjustBalanceHost(
                    viewModel = viewModel,
                    state = AdjustBalanceDialogState(
                        account = AccountWithBalance(main, 10_000),
                        currentBalance = 10_000,
                        targetBalanceText = "100.00",
                    ),
                    hideAmounts = false,
                    onStateChange = {},
                    onDismiss = { dismissed = true },
                )
            }

            waitForIdle()
            onNodeWithText("Cancel").performClick()

            dismissed shouldBe true
            entryDao.getAll().first().shouldBeEmpty()
        }
    }

    "hideAmounts masks the current balance" {
        withLedgerViewModel(clock = FakeClock()) { _, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            val main = accountDao.getAll().first().first()

            setContent {
                AdjustBalanceHost(
                    viewModel = viewModel,
                    state = AdjustBalanceDialogState(
                        account = AccountWithBalance(main, 10_000),
                        currentBalance = 10_000,
                        targetBalanceText = "100.00",
                    ),
                    hideAmounts = true,
                    onStateChange = {},
                    onDismiss = {},
                )
            }

            onNodeWithText("Current balance: •••••• CHF").assertIsDisplayed()
        }
    }
})
