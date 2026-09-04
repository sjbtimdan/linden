package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.ui.onTestMain

private fun insights(
    currentSpent: Long = 12_000,
    previousSpent: Long = 10_000,
    changePercent: Int? = 20,
    topCategories: List<CategoryShare> = emptyList(),
) = SpendingInsights(currentSpent, previousSpent, changePercent, topCategories)

private val groceries = Category(1, "Groceries", CategoryType.Expense, null)

@OptIn(ExperimentalTestApi::class)
class SpendingInsightsCardTest : StringSpec({
    "shows the month-to-date amount and the trend vs last month" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(insights = insights(), currency = Currency.CHF)
                }

                onNodeWithText("Spending insights").assertIsDisplayed()
                onNodeWithText("Spent this month").assertIsDisplayed()
                onNodeWithText("120.00").assertIsDisplayed()
                onNodeWithText("CHF").assertIsDisplayed()
                onNodeWithText("20% vs last month").assertIsDisplayed()
            }
        }
    }

    "shows the trend change as its absolute percent" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(
                        insights = insights(previousSpent = 12_000, changePercent = -20),
                        currency = Currency.CHF,
                    )
                }

                onNodeWithText("20% vs last month").assertIsDisplayed()
            }
        }
    }

    "hides the trend line when the previous month had no spending" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(insights = insights(changePercent = null), currency = Currency.CHF)
                }

                onNodeWithText("vs last month", substring = true).assertDoesNotExist()
                onNodeWithText("120.00").assertIsDisplayed()
            }
        }
    }

    "lists the top categories with their share" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(
                        insights = insights(
                            topCategories = listOf(
                                CategoryShare(groceries, 7_000, 58),
                                CategoryShare(null, 3_000, 25),
                            ),
                        ),
                        currency = Currency.CHF,
                    )
                }

                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("58%").assertIsDisplayed()
                onNodeWithText("Uncategorized").assertIsDisplayed()
                onNodeWithText("25%").assertIsDisplayed()
            }
        }
    }

    "renders without top categories" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(insights = insights(), currency = Currency.CHF)
                }

                onNodeWithText("Spent this month").assertIsDisplayed()
            }
        }
    }

    "collapsed card shows only the slim header and expands on tap" {
        onTestMain {
            runComposeUiTest {
                var toggled = 0
                setContent {
                    SpendingInsightsCard(
                        insights = insights(),
                        currency = Currency.CHF,
                        collapsed = true,
                        onToggleCollapsed = { toggled++ },
                    )
                }

                onNodeWithTag("spendingInsightsHeader").assertIsDisplayed()
                onNodeWithTag("spendingInsightsCard").assertDoesNotExist()
                onNodeWithText("Spent this month").assertDoesNotExist()

                onNodeWithTag("spendingInsightsHeader").performClick()

                toggled shouldBe 1
            }
        }
    }

    "collapse button hides the details and reports the toggle" {
        onTestMain {
            runComposeUiTest {
                var toggled = 0
                setContent {
                    SpendingInsightsCard(
                        insights = insights(),
                        currency = Currency.CHF,
                        collapsed = false,
                        onToggleCollapsed = { toggled++ },
                    )
                }

                onNodeWithTag("collapseInsights").assertIsDisplayed()
                onNodeWithTag("collapseInsights").performClick()

                toggled shouldBe 1
            }
        }
    }

    "has no collapse affordance without a toggle callback" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(insights = insights(), currency = Currency.CHF)
                }

                onNodeWithTag("collapseInsights").assertDoesNotExist()
            }
        }
    }

    "collapsed without a toggle callback falls back to the full card" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    SpendingInsightsCard(
                        insights = insights(),
                        currency = Currency.CHF,
                        collapsed = true,
                    )
                }

                // A collapse that could never be undone must not hide the details.
                onNodeWithTag("spendingInsightsCard").assertIsDisplayed()
                onNodeWithTag("spendingInsightsHeader").assertDoesNotExist()
                onNodeWithText("Spent this month").assertIsDisplayed()
            }
        }
    }
})
