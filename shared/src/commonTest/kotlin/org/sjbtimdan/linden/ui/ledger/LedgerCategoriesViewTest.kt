package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

private val groceries = CategoryWithTotal(
    category = Category(1, "Groceries", CategoryType.Expense),
    total = 500,
    count = 2,
)
private val salary = CategoryWithTotal(
    category = Category(2, "Salary", CategoryType.Income),
    total = 1_000,
    count = 1,
)

@OptIn(ExperimentalTestApi::class)
class LedgerCategoriesViewTest : StringSpec({
    "shows guided, no-spending, future-hidden and no-match empty states" {
        onTestMain {
            runComposeUiTest {
                var action = false
                setContent {
                    LedgerCategoriesView(
                        categories = emptyList(),
                        totals = emptyList(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        showFuture = false,
                        upcomingCount = 0,
                        onEmptyAction = { action = true },
                        onCategoryClick = {},
                    )
                }

                onNodeWithText("No categories yet.").assertIsDisplayed()
                onNodeWithText("Add categories").performClick()
                action shouldBe true
            }

            runComposeUiTest {
                setContent {
                    LedgerCategoriesView(
                        categories = listOf(groceries.category!!),
                        totals = emptyList(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        showFuture = false,
                        upcomingCount = 0,
                        onEmptyAction = {},
                        onCategoryClick = {},
                    )
                }

                onNodeWithText("No spending yet.").assertIsDisplayed()
            }

            runComposeUiTest {
                setContent {
                    LedgerCategoriesView(
                        categories = listOf(groceries.category!!),
                        totals = emptyList(),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "",
                        showFuture = false,
                        upcomingCount = 2,
                        onEmptyAction = {},
                        onCategoryClick = {},
                    )
                }

                onNodeWithText("Spending after today is hidden.").assertIsDisplayed()
            }

            runComposeUiTest {
                setContent {
                    LedgerCategoriesView(
                        categories = listOf(groceries.category!!, salary.category!!),
                        totals = listOf(groceries, salary),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "zzz",
                        showFuture = false,
                        upcomingCount = 0,
                        onEmptyAction = {},
                        onCategoryClick = {},
                    )
                }

                onNodeWithText("No categories match.").assertIsDisplayed()
            }
        }
    }

    "filters rows by name and forwards the clicked category" {
        onTestMain {
            runComposeUiTest {
                var clicked: CategoryWithTotal? = null
                setContent {
                    LedgerCategoriesView(
                        categories = listOf(groceries.category!!, salary.category!!),
                        totals = listOf(groceries, salary),
                        defaultCurrency = Currency.CHF,
                        hideAmounts = false,
                        searchQuery = "Gro",
                        showFuture = false,
                        upcomingCount = 0,
                        onEmptyAction = {},
                        onCategoryClick = { clicked = it },
                    )
                }

                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("Salary").assertDoesNotExist()
                onNodeWithText("Groceries").performClick()

                clicked?.category?.name shouldBe "Groceries"
            }
        }
    }
})
