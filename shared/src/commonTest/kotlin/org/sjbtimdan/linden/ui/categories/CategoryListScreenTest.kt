package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.waitUntilExactlyOneExists
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withViewModel

@OptIn(ExperimentalTestApi::class)
class CategoryListScreenTest : StringSpec({
    "displays empty state when no categories" {
        withViewModel { viewModel ->
            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("No categories yet.").assertIsDisplayed()
            onNodeWithText("+ New Category").assertIsDisplayed()
        }
    }

    "creating a category via dialog shows it in the list" {
        withViewModel { viewModel ->
            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("+ New Category").performClick()
            onNodeWithText("New Category").assertIsDisplayed()

            // The OutlinedTextField fills in via the node's text input semantics
            onNodeWithText("Save").performClick()
        }
    }

    "back button triggers navigation" {
        withViewModel { viewModel ->
            var navigatedBack = false

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = { navigatedBack = true },
                )
            }

            onNodeWithText("< Settings").performClick()
            navigatedBack shouldBe true
        }
    }

    "opening edit dialog shows current values" {
        withViewModel { viewModel ->
            viewModel.createCategory("Transport", CategoryType.Expense)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Transport").performClick()
            onNodeWithText("Edit Category").assertIsDisplayed()
        }
    }

    "displays the balance with its currency on each category" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Main", Currency.CHF)
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            val main = accountDao.getAll().first().first()

            entryDao.create(
                ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450),
            )
            entryDao.create(
                IncomeEntry(id = 0, category = salary, description = "Pay", account = main, amount = 50_000),
            )

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            waitUntilExactlyOneExists(hasText("500.00 CHF", substring = true))
            waitUntilExactlyOneExists(hasText("-4.50 CHF", substring = true))
        }
    }

    "displays the total in the default currency at the top" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            accountDao.create("Main", Currency.CHF)
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            val main = accountDao.getAll().first().first()

            entryDao.create(
                ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450),
            )
            entryDao.create(
                IncomeEntry(id = 0, category = salary, description = "Pay", account = main, amount = 50_000),
            )

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            waitUntilExactlyOneExists(hasText("495.50 CHF", substring = true))
            onNodeWithText("Total").assertIsDisplayed()
        }
    }
})
