package org.sjbtimdan.linden.ui.ledger

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onNodeWithContentDescription
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
import org.sjbtimdan.linden.ui.onTestMain

private val panelCategories = listOf(Category(1, "Groceries", CategoryType.Expense))
private val panelAccounts = listOf(Account(2, "Savings", Currency.CHF))

@OptIn(ExperimentalTestApi::class)
class LedgerSearchPanelTest : StringSpec({
    "typing a matching name in the entries view offers a category suggestion" {
        onTestMain {
            runComposeUiTest {
                var selected: Long? = null
                setSearchPanelContent(
                    categories = panelCategories,
                    accounts = panelAccounts,
                    onCategorySelected = { selected = it },
                )

                onNodeWithTag("searchField").performTextInput("gro")
                onNodeWithText("Groceries").assertIsDisplayed()
                onNodeWithText("Groceries").performClick()

                selected shouldBe 1L
                // Picking a suggestion clears the free text so the two cannot combine.
                onNodeWithText("Groceries").assertDoesNotExist()
            }
        }
    }

    "an account suggestion reports the account" {
        onTestMain {
            runComposeUiTest {
                var selected: Long? = null
                setSearchPanelContent(
                    categories = panelCategories,
                    accounts = panelAccounts,
                    onAccountSelected = { selected = it },
                )

                onNodeWithTag("searchField").performTextInput("sav")
                onNodeWithText("Savings").performClick()

                selected shouldBe 2L
            }
        }
    }

    "an already-active filter is not suggested again" {
        onTestMain {
            runComposeUiTest {
                setSearchPanelContent(
                    categories = panelCategories,
                    accounts = panelAccounts,
                    activeCategoryId = 1L,
                )

                onNodeWithTag("searchField").performTextInput("gro")

                onNodeWithText("Groceries").assertDoesNotExist()
            }
        }
    }

    "the accounts view does not offer suggestions" {
        onTestMain {
            runComposeUiTest {
                setSearchPanelContent(
                    viewMode = LedgerViewMode.Accounts,
                    categories = panelCategories,
                    accounts = panelAccounts,
                )

                onNodeWithTag("searchField").performTextInput("gro")

                onNodeWithText("Groceries").assertDoesNotExist()
            }
        }
    }

    "the clear icon empties the query" {
        onTestMain {
            runComposeUiTest {
                var clearedTo: String? = null
                setSearchPanelContent(query = "coffee", onQueryChange = { clearedTo = it })

                onNodeWithContentDescription("Clear").performClick()

                clearedTo shouldBe ""
            }
        }
    }

    "there is no clear icon while the query is empty" {
        onTestMain {
            runComposeUiTest {
                setSearchPanelContent()

                onNodeWithContentDescription("Clear").assertDoesNotExist()
            }
        }
    }

    "the entries view labels the field for entry search" {
        onTestMain {
            runComposeUiTest {
                setSearchPanelContent(viewMode = LedgerViewMode.Entries)

                onNodeWithText("Search entries").assertIsDisplayed()
            }
        }
    }

    "the accounts view labels the field by what it filters" {
        onTestMain {
            runComposeUiTest {
                setSearchPanelContent(viewMode = LedgerViewMode.Accounts)

                onNodeWithText("Filter accounts").assertIsDisplayed()
                onNodeWithText("Search entries").assertDoesNotExist()
            }
        }
    }
})

/** Renders the stateless search panel; the fixture hoists the query so suggestions react to typing. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setSearchPanelContent(
    query: String = "",
    viewMode: LedgerViewMode = LedgerViewMode.Entries,
    categories: List<Category> = emptyList(),
    accounts: List<Account> = emptyList(),
    activeCategoryId: Long? = null,
    activeAccountId: Long? = null,
    onQueryChange: (String) -> Unit = {},
    onCategorySelected: (Long) -> Unit = {},
    onAccountSelected: (Long) -> Unit = {},
) {
    setContent {
        var text by remember { mutableStateOf(query) }
        LedgerSearchPanel(
            query = text,
            onQueryChange = {
                text = it
                onQueryChange(it)
            },
            viewMode = viewMode,
            categories = categories,
            accounts = accounts,
            activeCategoryId = activeCategoryId,
            activeAccountId = activeAccountId,
            onCategorySelected = onCategorySelected,
            onAccountSelected = onAccountSelected,
            focusRequester = remember { FocusRequester() },
        )
    }
}
