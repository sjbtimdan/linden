package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.remember
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class UndoDeleteSnackbarTest : StringSpec({
    "shows the undo offer after a delete and restores the entry when tapped" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            viewModel.openEditDialog(created)

            setContent {
                val hostState = remember { SnackbarHostState() }
                Box {
                    SnackbarHost(hostState = hostState)
                    viewModel.UndoDeleteSnackbar(hostState)
                }
            }

            viewModel.deleteDialogEntry()
            waitForIdle()

            onNodeWithText("Entry deleted").assertIsDisplayed()
            onNodeWithText("Undo").assertIsDisplayed()

            onNodeWithText("Undo").performClick()
            waitForIdle()

            viewModel.lastDeleted.value.shouldBeNull()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldHaveSize(1)
            onNodeWithText("Entry deleted").assertDoesNotExist()
        }
    }
})
