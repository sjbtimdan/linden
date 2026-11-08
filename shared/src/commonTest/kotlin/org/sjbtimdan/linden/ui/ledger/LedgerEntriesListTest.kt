package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.onTestMain

private val listAccount = Account(1, "Main", Currency.CHF)
private val listCategory = Category(1, "Groceries", CategoryType.Expense)

private fun coffeeEntry(id: Long, amount: Long) = ExpenseEntry(
    id = id,
    category = listCategory,
    description = "Coffee",
    account = listAccount,
    amount = amount,
)

private val listItems = listOf(
    DayHeaderItem("day-2026-01-20", "Jan 20, 2026"),
    EntryListItem(coffeeEntry(1, 450)),
    EntryListItem(coffeeEntry(2, 1_200)),
)

@OptIn(ExperimentalTestApi::class)
class LedgerEntriesListTest : StringSpec({
    "renders day headers and their entries" {
        onTestMain {
            runComposeUiTest {
                setListContent(listItems)

                onNodeWithText("Jan 20, 2026").assertIsDisplayed()
                onAllNodesWithText("Coffee").assertCountEquals(2)
            }
        }
    }

    "clicking an entry reports it" {
        onTestMain {
            runComposeUiTest {
                var clicked: Entry? = null
                setListContent(listItems, onEntryClick = { clicked = it })

                onAllNodesWithText("Coffee")[0].performClick()

                clicked?.id shouldBe 1L
            }
        }
    }

    "shows the amounts" {
        onTestMain {
            runComposeUiTest {
                setListContent(listItems)

                onNodeWithText("− 4.50 CHF").assertIsDisplayed()
                onNodeWithText("− 12.00 CHF").assertIsDisplayed()
            }
        }
    }

    "masks the amounts when asked" {
        onTestMain {
            runComposeUiTest {
                setListContent(listItems, hideAmounts = true)

                onNodeWithText("− 4.50 CHF").assertDoesNotExist()
                onAllNodesWithText("••••••").assertCountEquals(2)
            }
        }
    }
})

@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.setListContent(
    items: List<LedgerListItem>,
    hideAmounts: Boolean = false,
    onEntryClick: (Entry) -> Unit = {},
) {
    setContent {
        LedgerEntriesList(
            items = items,
            hideAmounts = hideAmounts,
            zone = TimeZone.UTC,
            onEntryClick = onEntryClick,
        )
    }
}
