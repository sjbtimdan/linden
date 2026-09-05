package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import org.sjbtimdan.linden.model.CategoryType
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
})
