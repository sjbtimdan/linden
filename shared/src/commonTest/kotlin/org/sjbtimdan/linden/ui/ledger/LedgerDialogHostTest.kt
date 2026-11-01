package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.entry.EntryDraft
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerDialogHostTest : StringSpec({
    "keeps hidden accounts referenced by the draft in the account picker" {
        withLedgerViewModel(clock = FakeClock()) { viewModel ->
            val visible = Account(1, "Visible", Currency.CHF)
            val hiddenReferenced = Account(2, "Hidden Referenced", Currency.CHF, hidden = true)
            val hiddenUnreferenced = Account(3, "Hidden Unreferenced", Currency.CHF, hidden = true)
            val groceries = Category(1, "Groceries", CategoryType.Expense)
            val draft = EntryDraft.forNew(clock = FakeClock()).copy(
                amountText = "10.00",
                categoryId = groceries.id,
                accountId = hiddenReferenced.id,
            )

            setContent {
                LedgerDialogHost(
                    viewModel = viewModel,
                    accounts = listOf(visible, hiddenReferenced, hiddenUnreferenced),
                    categories = listOf(groceries),
                    dialogState = draft,
                    saving = false,
                    adjustState = null,
                    hideAmounts = false,
                    onAdjustStateChange = {},
                )
            }

            onNodeWithText("New Expense").assertIsDisplayed()
            onNodeWithText("Hidden Referenced").assertIsDisplayed()

            onNodeWithText("Account").performScrollTo().performClick()
            waitForIdle()

            onNodeWithText("Visible").assertIsDisplayed()
            onNodeWithText("Hidden Referenced").assertIsDisplayed()
            onNodeWithText("Hidden Unreferenced").assertDoesNotExist()
        }
    }
})
