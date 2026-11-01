package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import org.sjbtimdan.linden.ui.onTestMain

@OptIn(ExperimentalTestApi::class)
class LedgerFilterPanelTest : StringSpec({
    "entries view shows search, type and amount filters" {
        onTestMain {
            runComposeUiTest {
                showPanel(viewMode = LedgerViewMode.Entries, expanded = true)

                onNodeWithTag("searchField").assertIsDisplayed()
                onNodeWithTag("typeFilter-All").assertIsDisplayed()
                onNodeWithTag("typeFilter-Expense").assertIsDisplayed()
                onNodeWithTag("typeFilter-Income").assertIsDisplayed()
                onNodeWithTag("typeFilter-Transfer").assertIsDisplayed()
                onNodeWithTag("amountFilterChip").assertIsDisplayed()
            }
        }
    }

    "categories view omits the Transfer and amount filters" {
        onTestMain {
            runComposeUiTest {
                showPanel(viewMode = LedgerViewMode.Categories, expanded = true)

                onNodeWithTag("typeFilter-All").assertIsDisplayed()
                onNodeWithTag("typeFilter-Transfer").assertDoesNotExist()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }

    "accounts view keeps only the search field" {
        onTestMain {
            runComposeUiTest {
                showPanel(viewMode = LedgerViewMode.Accounts, expanded = true)

                onNodeWithTag("searchField").assertIsDisplayed()
                onNodeWithTag("typeFilter-All").assertDoesNotExist()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }

    "collapsed panel hides the search and filter controls" {
        onTestMain {
            runComposeUiTest {
                showPanel(viewMode = LedgerViewMode.Entries, expanded = false)

                onNodeWithTag("searchField").assertDoesNotExist()
                onNodeWithTag("typeFilter-All").assertDoesNotExist()
                onNodeWithTag("amountFilterChip").assertDoesNotExist()
            }
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.showPanel(viewMode: LedgerViewMode, expanded: Boolean) {
    setContent {
        LedgerFilterPanel(
            expanded = expanded,
            onToggle = {},
            onSearchClick = {},
            viewMode = viewMode,
            searchQuery = "",
            onSearchQueryChange = {},
            categories = emptyList(),
            accounts = emptyList(),
            activeCategoryId = null,
            activeAccountId = null,
            onCategorySelected = {},
            onAccountSelected = {},
            focusRequester = FocusRequester(),
            typeFilter = null,
            onTypeFilterChange = {},
            amountFilter = null,
            onAmountFilterChange = {},
            onClearAmountFilter = {},
        )
    }
}
