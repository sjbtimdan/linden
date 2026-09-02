package org.sjbtimdan.linden.ui.budget

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.ui.withBudgetViewModel

@OptIn(ExperimentalTestApi::class)
class BudgetScreenTest : StringSpec({
    "new budget dialog lists expense and both categories but not income-only ones" {
        withBudgetViewModel { categoryDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            categoryDao.create("Gifts", CategoryType.Both)

            setContent {
                BudgetScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("+ New Budget").performClick()
            onNodeWithContentDescription("Choose category").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Gifts").assertIsDisplayed()
            onNodeWithText("Salary").assertDoesNotExist()
        }
    }

    "creating a budget for an expense category adds it to the list" {
        withBudgetViewModel { categoryDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)

            setContent {
                BudgetScreen(viewModel = viewModel, onNavigateBack = {})
            }

            onNodeWithText("+ New Budget").performClick()
            onNodeWithContentDescription("Choose category").performClick()
            onNodeWithText("Groceries").performClick()
            onNode(hasSetTextAction()).performTextInput("800.00")
            onNodeWithText("Save").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("800.00").assertIsDisplayed()
            onNodeWithText("Save").assertDoesNotExist()
            onNodeWithText("Salary").assertDoesNotExist()
        }
    }
})
