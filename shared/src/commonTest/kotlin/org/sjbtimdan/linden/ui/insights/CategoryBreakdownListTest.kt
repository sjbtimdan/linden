package org.sjbtimdan.linden.ui.insights

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.entry.DateLanguage
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class CategoryBreakdownListTest : StringSpec({

    "renders the expense section ranked by amount with icons and totals" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = 4_500),
                                row("Dining", 2, amountMinor = 900, icon = CategoryIcon.Restaurant),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("Expenses").assertIsDisplayed()
                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("45.00 CHF").assertIsDisplayed()
                onNodeWithText("Dining").assertIsDisplayed()
                onNodeWithText("9.00 CHF").assertIsDisplayed()
                onNodeWithText("Income").assertDoesNotExist()
            }
        }
    }

    "renders the income section under its own header" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = emptyList(),
                            incomes = listOf(row("Salary", 3, amountMinor = 5_000, icon = CategoryIcon.Savings)),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("Income").assertIsDisplayed()
                onNodeWithText("Salary").assertIsDisplayed()
                onNodeWithText("50.00 CHF").assertIsDisplayed()
                onNodeWithText("Expenses").assertDoesNotExist()
            }
        }
    }

    "renders nothing for an empty breakdown" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(emptyList(), emptyList()),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("Expenses").assertDoesNotExist()
                onNodeWithText("Income").assertDoesNotExist()
            }
        }
    }

    "shows budget progress instead of a delta for budgeted rows" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = 450, previousMinor = 300, budgetMinor = 800),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = "Jul 2026",
                    )
                }

                onNodeWithText("4.50 of 8.00").assertIsDisplayed()
                onNodeWithText("vs ", substring = true).assertDoesNotExist()
            }
        }
    }

    "shows the change against the previous month for rows without a budget" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = 1_000, previousMinor = 300),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = "Jul 2026",
                    )
                }

                onNodeWithText("10.00 CHF").assertIsDisplayed()
                onNodeWithText("vs Jul 2026: + 7.00 CHF").assertIsDisplayed()
            }
        }
    }

    "shows a negative change when the category cost more last month" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = 300, previousMinor = 1_000),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = "Jul 2026",
                    )
                }

                onNodeWithText("vs Jul 2026: − 7.00 CHF").assertIsDisplayed()
            }
        }
    }

    "hides the delta when there is no previous month to compare against" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = 1_000, previousMinor = 300),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("10.00 CHF").assertIsDisplayed()
                onNodeWithText("vs ", substring = true).assertDoesNotExist()
            }
        }
    }

    "shows an incomplete row without an amount or budget bar" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Groceries", 1, amountMinor = null, budgetMinor = 800),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("0.00 CHF").assertDoesNotExist()
                onNodeWithText(" of ", substring = true).assertDoesNotExist()
            }
        }
    }

    "falls back to the category initial when it has no icon" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    CategoryBreakdownList(
                        breakdown = MonthBreakdown(
                            expenses = listOf(
                                row("Zoo", 4, amountMinor = 100, icon = null),
                            ),
                            incomes = emptyList(),
                        ),
                        currency = testCurrency,
                        language = DateLanguage.English,
                        previousMonthLabel = null,
                    )
                }

                onNodeWithText("Zoo").assertIsDisplayed()
                onNodeWithText("Z").assertIsDisplayed()
            }
        }
    }
})

/** Currency used by the tests; amounts render with the CHF symbol. */
private val testCurrency = Currency.CHF

private fun row(
    name: String,
    id: Long,
    amountMinor: Long?,
    previousMinor: Long? = null,
    budgetMinor: Long? = null,
    icon: CategoryIcon? = CategoryIcon.ShoppingCart,
) = CategoryBreakdownRow(
    category = Category(id, name, CategoryType.Expense, icon),
    amountMinor = amountMinor,
    previousMinor = previousMinor,
    budgetMinor = budgetMinor,
    count = 1,
)
