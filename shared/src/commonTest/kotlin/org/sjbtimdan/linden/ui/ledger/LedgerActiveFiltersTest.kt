package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain

private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)

@OptIn(ExperimentalTestApi::class)
class LedgerActiveFiltersTest : StringSpec({
    "renders every active filter chip and clears the matching filter" {
        onTestMain {
            runComposeUiTest {
                var cleared = ""
                setContent {
                    LedgerActiveFilters(
                        viewMode = LedgerViewMode.Entries,
                        searchQuery = "coffee",
                        typeFilter = EntryType.Expense,
                        categoryFilter = groceries.id,
                        accountFilter = main.id,
                        amountFilter = AmountFilter(AmountOperator.GreaterThan, 500),
                        categories = listOf(groceries),
                        accounts = listOf(main),
                        onClearSearch = { cleared = "search" },
                        onClearType = { cleared = "type" },
                        onClearCategory = { cleared = "category" },
                        onClearAccount = { cleared = "account" },
                        onClearAmount = { cleared = "amount" },
                        onClearAll = { cleared = "all" },
                    )
                }

                onNodeWithTag("activeSearchFilterChip").assertIsDisplayed()
                onNodeWithText("coffee").assertIsDisplayed()
                onNodeWithTag("activeTypeFilterChip").assertIsDisplayed()
                onNodeWithText("Expense").assertIsDisplayed()
                onNodeWithTag("categoryFilterChip").assertIsDisplayed()
                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithTag("accountFilterChip").assertIsDisplayed()
                onNodeWithText("Main").assertIsDisplayed()
                onNodeWithTag("activeAmountFilterChip").assertIsDisplayed()
                onNodeWithText("> 5").assertIsDisplayed()

                onNodeWithTag("categoryFilterChip").performClick()
                cleared shouldBe "category"

                onNodeWithTag("clearAllFilters").performClick()
                cleared shouldBe "all"
            }
        }
    }

    "uses fallback labels and renders nothing outside the entries view" {
        onTestMain {
            runComposeUiTest {
                setContent {
                    LedgerActiveFilters(
                        viewMode = LedgerViewMode.Entries,
                        searchQuery = "",
                        typeFilter = null,
                        categoryFilter = 99,
                        accountFilter = 99,
                        amountFilter = null,
                        categories = emptyList(),
                        accounts = emptyList(),
                        onClearSearch = {},
                        onClearType = {},
                        onClearCategory = {},
                        onClearAccount = {},
                        onClearAmount = {},
                        onClearAll = {},
                    )
                }

                onNodeWithText("Uncategorized").assertIsDisplayed()
                onNodeWithText("Unknown account").assertIsDisplayed()
            }

            runComposeUiTest {
                setContent {
                    LedgerActiveFilters(
                        viewMode = LedgerViewMode.Accounts,
                        searchQuery = "coffee",
                        typeFilter = EntryType.Expense,
                        categoryFilter = 1,
                        accountFilter = 1,
                        amountFilter = AmountFilter(AmountOperator.GreaterThan, 500),
                        categories = emptyList(),
                        accounts = emptyList(),
                        onClearSearch = {},
                        onClearType = {},
                        onClearCategory = {},
                        onClearAccount = {},
                        onClearAmount = {},
                        onClearAll = {},
                    )
                }

                onNodeWithTag("activeSearchFilterChip").assertDoesNotExist()
                onNodeWithTag("clearAllFilters").assertDoesNotExist()
            }
        }
    }
})
