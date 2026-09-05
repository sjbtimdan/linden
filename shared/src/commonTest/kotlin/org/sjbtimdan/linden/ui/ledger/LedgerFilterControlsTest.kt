package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.onTestMain

private val groceries = Category(1, "Groceries", CategoryType.Expense, null)
private val salary = Category(2, "Salary", CategoryType.Income, null)
private val mainAccount = Account(1, "Main", Currency.CHF)

@OptIn(ExperimentalTestApi::class)
class LedgerFilterControlsTest : StringSpec({
    "type dropdown lists every type and reports the selection" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = null
                setControlsContent(onTypeFilterChange = { selected = it })

                onNodeWithTag("typeFilterDropdown").performClick()

                onNodeWithText("Expense").assertIsDisplayed()
                onNodeWithText("Income").assertIsDisplayed()
                onNodeWithText("Transfer").assertIsDisplayed()

                onNodeWithText("Transfer").performClick()

                selected shouldBe EntryType.Transfer
            }
        }
    }

    "shows the active type on the chip" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(typeFilter = EntryType.Income)

                onNodeWithText("Income").assertIsDisplayed()
            }
        }
    }

    "entries view offers the category, account and amount filters" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(showEntryFilters = true)

                onNodeWithTag("categoryFilterDropdown").assertIsDisplayed()
                onNodeWithTag("accountFilterDropdown").assertIsDisplayed()
                onNodeWithTag("amountFilterChip").assertIsDisplayed()
            }
        }
    }

    "category and account dropdowns report their selections" {
        onTestMain {
            runComposeUiTest {
                var categoryId: Long? = null
                var accountId: Long? = null
                setControlsContent(
                    onCategoryFilterChange = { categoryId = it },
                    onAccountFilterChange = { accountId = it },
                )

                onNodeWithTag("categoryFilterDropdown").performClick()
                onNodeWithText("Groceries").performClick()

                onNodeWithTag("accountFilterDropdown").performClick()
                onNodeWithText("Main").performClick()

                categoryId shouldBe groceries.id
                accountId shouldBe mainAccount.id
            }
        }
    }

    "shows the active category and account on their chips" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(categoryFilter = salary.id, accountFilter = mainAccount.id)

                onNodeWithText("Salary").assertIsDisplayed()
                onNodeWithText("Main").assertIsDisplayed()
            }
        }
    }

    "categories-only view hides the entry filters" {
        onTestMain {
            runComposeUiTest {
                setControlsContent(showEntryFilters = false)

                onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
                onNodeWithTag("categoryFilterDropdown").assertDoesNotExist()
                onNodeWithTag("accountFilterDropdown").assertDoesNotExist()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }

    "picking the All option reports a cleared type filter" {
        onTestMain {
            runComposeUiTest {
                var selected: EntryType? = EntryType.Income
                setControlsContent(
                    typeFilter = EntryType.Income,
                    onTypeFilterChange = { selected = it },
                )

                onNodeWithTag("typeFilterDropdown").performClick()
                onNodeWithText("Types: All").performClick()

                selected shouldBe null
            }
        }
    }

    "applying an amount from the popover reports the filter" {
        onTestMain {
            runComposeUiTest {
                var applied: AmountFilter? = null
                setControlsContent(onAmountFilterChange = { applied = it })

                onNodeWithTag("amountFilterChip").performClick()
                onNodeWithText("Filter by amount").assertIsDisplayed()
                onNodeWithText("<").performClick()
                onNodeWithTag("amountFilterValue").performTextInput("100")
                onNodeWithText("Apply").performClick()

                applied shouldBe AmountFilter(AmountOperator.LessThan, 10_000)
            }
        }
    }

    "shows the active amount filter and reports Clear" {
        onTestMain {
            runComposeUiTest {
                var cleared = false
                setControlsContent(
                    amountFilter = AmountFilter(AmountOperator.GreaterThan, 500),
                    onClearAmountFilter = { cleared = true },
                )

                onNodeWithText("> 5").assertIsDisplayed()

                onNodeWithTag("amountFilterChip").performClick()
                onNodeWithTag("clearAmountFilterButton").performClick()

                cleared shouldBe true
            }
        }
    }
})

/** Renders the inline filter row with the shared fixture data; every callback defaults to a no-op. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setControlsContent(
    showEntryFilters: Boolean = true,
    typeFilter: EntryType? = null,
    categoryFilter: Long? = null,
    accountFilter: Long? = null,
    amountFilter: AmountFilter? = null,
    onTypeFilterChange: (EntryType?) -> Unit = {},
    onCategoryFilterChange: (Long?) -> Unit = {},
    onAccountFilterChange: (Long?) -> Unit = {},
    onAmountFilterChange: (AmountFilter?) -> Unit = {},
    onClearAmountFilter: () -> Unit = {},
) {
    setContent {
        LedgerFilterControls(
            showEntryFilters = showEntryFilters,
            typeFilter = typeFilter,
            onTypeFilterChange = onTypeFilterChange,
            categories = listOf(groceries, salary),
            categoryFilter = categoryFilter,
            onCategoryFilterChange = onCategoryFilterChange,
            accounts = listOf(mainAccount),
            accountFilter = accountFilter,
            onAccountFilterChange = onAccountFilterChange,
            amountFilter = amountFilter,
            onAmountFilterChange = onAmountFilterChange,
            onClearAmountFilter = onClearAmountFilter,
        )
    }
}
