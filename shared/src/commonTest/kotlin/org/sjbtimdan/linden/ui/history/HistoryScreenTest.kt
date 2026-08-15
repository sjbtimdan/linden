package org.sjbtimdan.linden.ui.history

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.entry.SortOrder
import org.sjbtimdan.linden.ui.withHistoryViewModel

@OptIn(ExperimentalTestApi::class)
class HistoryScreenTest : StringSpec({
    "displays empty state" {
        withHistoryViewModel { viewModel ->
            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "search narrows the list" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Search").performTextInput("lunch")

            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "type filter chip narrows the list" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Refund").assertIsDisplayed()

            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "no entries match message when nothing matches" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Search").performTextInput("zzz")

            onNodeWithText("No entries match.").assertIsDisplayed()
        }
    }

    "editing an entry shows current values and saves changes" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Edit Expense").assertIsDisplayed()

            onNodeWithText("Amount").performTextClearance()
            onNodeWithText("Amount").performTextInput("5.00")
            onNodeWithText("Save").performClick()

            onNodeWithText("− 5.00 CHF").assertIsDisplayed()
            onNodeWithText("− 4.50 CHF").assertDoesNotExist()
        }
    }

    "deleting an entry from the edit dialog removes it" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Delete").performClick()

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "day headers appear when entries span multiple days" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(
                    0, groceries, "Coffee", main, 450,
                    createdAt = Instant.fromEpochMilliseconds(1_000_000_000_000),
                )
            )
            viewModel.createEntry(
                ExpenseEntry(
                    0, groceries, "Lunch", main, 1_200,
                    createdAt = Instant.fromEpochMilliseconds(1_000_086_400_000),
                )
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("10 Sep 2001").assertIsDisplayed()
            onNodeWithText("9 Sep 2001").assertIsDisplayed()
        }
    }

    "no day headers when sorted by amount" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(
                    0, groceries, "Coffee", main, 450,
                    createdAt = Instant.fromEpochMilliseconds(1_000_000_000_000),
                )
            )
            viewModel.createEntry(
                ExpenseEntry(
                    0, groceries, "Lunch", main, 1_200,
                    createdAt = Instant.fromEpochMilliseconds(1_000_086_400_000),
                )
            )
            viewModel.setSortOrder(SortOrder.AmountHighToLow)

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("9 Sep 2001").assertDoesNotExist()
            onNodeWithText("10 Sep 2001").assertDoesNotExist()
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
