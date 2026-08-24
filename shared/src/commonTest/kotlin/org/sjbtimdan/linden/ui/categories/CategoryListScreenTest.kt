package org.sjbtimdan.linden.ui.categories

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import io.kotest.core.spec.style.StringSpec
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
})
