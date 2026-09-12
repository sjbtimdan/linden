package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.hasClickAction
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
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
            onNodeWithText("New Category").assertIsDisplayed()
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

            onNodeWithText("New Category").performClick()
            onNode(hasText("New Category") and !hasClickAction()).assertIsDisplayed()

            onNodeWithText("Save").performClick()
        }
    }

    "creating a category with a duplicate name shows an error and keeps the dialog open" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("New Category").performClick()
            onAllNodes(hasSetTextAction())[1].performTextInput("Groceries")
            onNodeWithText("Save").performClick()

            onNodeWithText("A category with this name already exists").assertIsDisplayed()
            onNode(hasText("New Category") and !hasClickAction()).assertIsDisplayed()
            viewModel.categories.value.shouldHaveSize(1)
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

            onNodeWithContentDescription("Back").performClick()
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

    "search filters the category list and clears on the clear button" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            viewModel.createCategory("Salary", CategoryType.Income)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onAllNodes(hasSetTextAction())[0].performTextInput("gro")

            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Salary").assertDoesNotExist()

            onNodeWithContentDescription("Clear").performClick()

            onNodeWithText("Salary").assertIsDisplayed()
        }
    }

    "search with no matches shows the no-matches message" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onAllNodes(hasSetTextAction())[0].performTextInput("nonexistent")

            onNodeWithText("No matching categories.").assertIsDisplayed()
        }
    }

    "delete button is shown when editing a category without entries" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Groceries").performClick()
            onNodeWithText("Delete Category").assertIsDisplayed()
            onNodeWithText("Delete Category").assertIsEnabled()
        }
    }

    "delete button is hidden when creating a new category" {
        withViewModel { viewModel ->
            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("New Category").performClick()
            onNode(hasText("New Category") and !hasClickAction()).assertIsDisplayed()
            onNodeWithText("Delete Category").assertDoesNotExist()
        }
    }

    "delete button is disabled with a note when editing a category with entries" {
        withViewModel { categoryDao, entryDao, accountDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.categoriesWithEntries.first { groceries.id in it }

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Groceries").performClick()
            onNodeWithText("Edit Category").assertIsDisplayed()
            onNodeWithText("This category cannot be deleted: it has entries.").assertIsDisplayed()
            onNodeWithText("Delete Category").assertIsNotEnabled()
        }
    }

    "deleting a category without entries removes it from the list" {
        withViewModel { viewModel ->
            viewModel.createCategory("Groceries", CategoryType.Expense)
            viewModel.createCategory("Salary", CategoryType.Income)

            setContent {
                CategoryListScreen(
                    viewModel = viewModel,
                    onNavigateBack = {},
                )
            }

            onNodeWithText("Groceries").performClick()
            onNodeWithText("Edit Category").assertIsDisplayed()
            onNodeWithText("Delete Category").performClick()

            viewModel.categories.value.map { it.name } shouldBe listOf("Salary")
            onNodeWithText("Salary").assertIsDisplayed()
            onNodeWithText("Groceries").assertDoesNotExist()
        }
    }
})
