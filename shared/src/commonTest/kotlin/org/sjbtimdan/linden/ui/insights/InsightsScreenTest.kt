package org.sjbtimdan.linden.ui.insights

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withInsightsViewModel
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class InsightsScreenTest : StringSpec({

    "shows the current month, both series totals and the change against last month" {
        withInsightsViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, entryDao, viewModel ->
            val main = seedAccount(accountDao)
            val groceries = seedCategory(categoryDao, "Groceries")
            val salary = seedCategory(categoryDao, "Salary", CategoryType.Income)
            expense(entryDao, groceries, main, 450, "2026-08-10T12:00:00Z")
            income(entryDao, salary, main, 5_000, "2026-08-01T09:00:00Z")
            expense(entryDao, groceries, main, 100, "2026-07-05T12:00:00Z")

            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("Aug 2026").assertIsDisplayed()
            // Header legend rows and the breakdown section headers share the copy.
            onAllNodesWithText("Income").assertCountEquals(2)
            onAllNodesWithText("Expenses").assertCountEquals(2)
            onNodeWithText("Net").assertIsDisplayed()
            // Each amount appears in the header row and in its breakdown card.
            onAllNodesWithText("50.00 CHF").assertCountEquals(2)
            onAllNodesWithText("4.50 CHF").assertCountEquals(2)
            onNodeWithText("+ 45.50 CHF").assertIsDisplayed()
            onAllNodesWithText("vs Jul 2026: + 3.50 CHF").assertCountEquals(2)
            onNodeWithText("12-month average: 0.46 CHF").assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Salary").assertIsDisplayed()
            onNodeWithTag(MONTHLY_TREND_CHART_TAG).assertIsDisplayed()
        }
    }

    "tapping an older month's bar shows its own totals and title" {
        withInsightsViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, entryDao, viewModel ->
            val main = seedAccount(accountDao)
            val groceries = seedCategory(categoryDao, "Groceries")
            expense(entryDao, groceries, main, 450, "2026-08-10T12:00:00Z")
            expense(entryDao, groceries, main, 100, "2026-07-05T12:00:00Z")

            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateBack = {})
            }

            // September 2025 is the first of the twelve window months,
            // so July 2026 is column ten and August 2026 column eleven.
            onNodeWithTag(TREND_BAR_TAG_PREFIX + 10).performClick()

            onNodeWithText("Jul 2026").assertIsDisplayed()
            onAllNodesWithText("1.00 CHF").assertCountEquals(2)
            onNodeWithText("− 1.00 CHF").assertIsDisplayed()
            onNodeWithText("vs Jun 2026: + 1.00 CHF").assertIsDisplayed()
            onNodeWithText("Groceries").assertIsDisplayed()
        }
    }

    "the forward arrow is disabled at the current month and pages after going back" {
        withInsightsViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, entryDao, viewModel ->
            val main = seedAccount(accountDao)
            val groceries = seedCategory(categoryDao)
            expense(entryDao, groceries, main, 450, "2026-08-10T12:00:00Z")

            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("Aug 2026").assertIsDisplayed()
            onNodeWithTag("insightsNext").assertIsNotEnabled()

            onNodeWithTag("insightsPrevious").performClick()

            onNodeWithText("Aug 2025").assertIsDisplayed()
            onAllNodesWithText("0.00 CHF").assertCountEquals(3)

            onNodeWithTag("insightsNext").performClick()

            onNodeWithText("Aug 2026").assertIsDisplayed()
            onAllNodesWithText("4.50 CHF").assertCountEquals(2)
        }
    }

    "hiding totals masks the amounts and the change caption" {
        withInsightsViewModel(
            today = { LocalDate(2026, 8, 15) },
            hideEntryTotal = true,
        ) { accountDao, categoryDao, entryDao, viewModel ->
            val main = seedAccount(accountDao)
            val groceries = seedCategory(categoryDao)
            expense(entryDao, groceries, main, 450, "2026-08-10T12:00:00Z")
            income(entryDao, groceries, main, 5_000, "2026-08-01T09:00:00Z")

            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("Income").assertIsDisplayed()
            onNodeWithText("Expenses").assertIsDisplayed()
            onNodeWithText("Net").assertIsDisplayed()
            onAllNodesWithText("••••••").assertCountEquals(3)
            onNodeWithText("4.50 CHF").assertDoesNotExist()
            onNodeWithText("50.00 CHF").assertDoesNotExist()
            onNodeWithText("vs ", substring = true).assertDoesNotExist()
            onNodeWithText("average", substring = true).assertDoesNotExist()
        }
    }

    "shows a no-entries hint instead of the chart when the database is empty" {
        withInsightsViewModel(today = { LocalDate(2026, 8, 15) }) { _, _, _, viewModel ->
            setContent {
                InsightsScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("Add an entry to start seeing monthly trends").assertIsDisplayed()
            onNodeWithTag(MONTHLY_TREND_CHART_TAG).assertDoesNotExist()
            onNodeWithText("Aug 2026").assertDoesNotExist()
        }
    }
})

private suspend fun seedAccount(accountDao: AccountDao): Account {
    accountDao.create("Main", Currency.CHF)
    return accountDao.getAll().first().single()
}

private suspend fun seedCategory(
    categoryDao: CategoryDao,
    name: String = "Groceries",
    type: CategoryType = CategoryType.Expense,
): Category {
    categoryDao.create(name, type)
    return categoryDao.getAll().first().first { it.name == name }
}

private suspend fun expense(entryDao: EntryDao, category: Category, account: Account, amount: Long, at: String) {
    entryDao.create(
        ExpenseEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = amount,
            createdAt = Instant.parse(at),
        ),
    )
}

private suspend fun income(entryDao: EntryDao, category: Category, account: Account, amount: Long, at: String) {
    entryDao.create(
        IncomeEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = amount,
            createdAt = Instant.parse(at),
        ),
    )
}
