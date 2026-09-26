package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

private val groceries = CategoryWithTotal(
    category = Category(1, "Groceries", CategoryType.Expense),
    total = -450,
    count = 1,
    budget = 1_000,
)

@OptIn(ExperimentalTestApi::class)
class CategoryTotalsListTest : StringSpec({
    "shows the category total and its budget progress while amounts are visible" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryTotalsList(
                        categories = listOf(groceries),
                        currency = Currency.CHF,
                        modifier = Modifier.fillMaxWidth(),
                        emptyMessage = "Nothing here",
                        onCategoryClick = {},
                    )
                }

                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertIsDisplayed()
                onNodeWithText("4.50 of 10.00").assertIsDisplayed()
            }
        }
    }

    "masks the total and hides the budget progress when amounts are hidden" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryTotalsList(
                        categories = listOf(groceries),
                        currency = Currency.CHF,
                        modifier = Modifier.fillMaxWidth(),
                        hideAmounts = true,
                        emptyMessage = "Nothing here",
                        onCategoryClick = {},
                    )
                }

                // The category stays identifiable; its spending never shows, and the
                // budget bar would leak it through the spent/limit label.
                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("− 4.50 CHF").assertDoesNotExist()
                onNodeWithText("4.50 of 10.00").assertDoesNotExist()
                onNodeWithText("••••••").assertIsDisplayed()
            }
        }
    }
})
