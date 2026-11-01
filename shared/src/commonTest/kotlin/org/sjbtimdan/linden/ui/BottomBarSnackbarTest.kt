package org.sjbtimdan.linden.ui

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.db.SqlPreparedStatement
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.data.createTestSqlDriver
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.accounts.AccountListScreen
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.budget.BudgetScreen
import org.sjbtimdan.linden.ui.budget.BudgetViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListScreen
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel

@OptIn(ExperimentalTestApi::class)
class BottomBarSnackbarTest : StringSpec({
    val message = "Your change couldn't be saved. Please try again."

    "an accounts write error stays visible above the bottom bar" {
        onTestMain {
            runComposeUiTest {
                val database = failingWriteDatabase()
                val entryDao = EntryDao(database.entryQueries)
                val viewModel = AccountListViewModel(
                    accountDao = AccountDao(database.accountQueries),
                    entryDao = entryDao,
                    settingsDao = SettingsDao(database.settingsQueries),
                    allEntries = testAllEntries(entryDao),
                )

                setContent {
                    CompositionLocalProvider(LocalBottomBarContentPadding provides 120.dp) {
                        AccountListScreen(viewModel = viewModel, onNavigateBack = {})
                    }
                }

                viewModel.createAccount("Main", Currency.CHF)
                waitForIdle()

                onNodeWithText(message).assertIsDisplayed()
            }
        }
    }

    "a category write error stays visible above the bottom bar" {
        onTestMain {
            runComposeUiTest {
                val database = failingWriteDatabase()
                val viewModel = CategoryListViewModel(
                    categoryDao = CategoryDao(database.categoryQueries),
                    entryDao = EntryDao(database.entryQueries),
                )

                setContent {
                    CompositionLocalProvider(LocalBottomBarContentPadding provides 120.dp) {
                        CategoryListScreen(viewModel = viewModel, onNavigateBack = {})
                    }
                }

                viewModel.createCategory("Food", CategoryType.Expense)
                waitForIdle()

                onNodeWithText(message).assertIsDisplayed()
            }
        }
    }

    "a budget write error stays visible above the bottom bar" {
        onTestMain {
            runComposeUiTest {
                val database = failingWriteDatabase()
                val viewModel = BudgetViewModel(
                    budgetDao = BudgetDao(database.budgetQueries),
                    categoryDao = CategoryDao(database.categoryQueries),
                    settingsDao = SettingsDao(database.settingsQueries),
                )

                setContent {
                    CompositionLocalProvider(LocalBottomBarContentPadding provides 120.dp) {
                        BudgetScreen(viewModel = viewModel, onNavigateBack = {})
                    }
                }

                viewModel.saveBudget("Food", 1_000)
                waitForIdle()

                onNodeWithText(message).assertIsDisplayed()
            }
        }
    }
})

/** A database whose reads hit an in-memory DB but whose writes fail, to exercise error snackbars. */
private suspend fun failingWriteDatabase(): LindenDatabase {
    val driver = createTestSqlDriver()
    createLindenDatabase(driver)
    return LindenDatabase(
        object : SqlDriver by driver {
            override fun execute(
                identifier: Int?,
                sql: String,
                parameters: Int,
                binders: (SqlPreparedStatement.() -> Unit)?,
            ): QueryResult<Long> = error("write failed")
        },
    )
}
