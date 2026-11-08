package org.sjbtimdan.linden.ui.ledger

import androidx.compose.material3.Text
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertIsDisplayed
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
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.onTestMain
import kotlin.time.Instant

private val main = Account(1, "Main", Currency.CHF)
private val groceries = Category(1, "Groceries", CategoryType.Expense)
private val coffee = ExpenseEntry(
    id = 0,
    category = groceries,
    description = "Coffee",
    account = main,
    amount = 450,
    createdAt = Instant.parse("2026-08-10T12:00:00Z"),
    createdZone = TimeZone.UTC,
)

@OptIn(ExperimentalTestApi::class)
class LedgerEntriesViewTest : StringSpec({
    "shows the guided empty state with the pace header" {
        onTestMain {
            runComposeUiTest {
                var added = false
                setContent {
                    LedgerEntriesView(
                        entries = emptyList(),
                        zone = TimeZone.UTC,
                        hideAmounts = false,
                        searchQuery = "",
                        typeFilter = null,
                        categoryFilter = null,
                        accountFilter = null,
                        amountFilter = null,
                        periodIsAll = false,
                        showFuture = false,
                        upcomingCount = 0,
                        hasAnyEntries = false,
                        paceHeader = { Text("Pace") },
                        onEntryClick = {},
                        onAddFirstEntry = { added = true },
                        onRevealUpcoming = {},
                    )
                }

                onNodeWithText("Pace").assertIsDisplayed()
                onNodeWithText("No entries yet.").assertIsDisplayed()
                onNodeWithText("Add your first entry").performClick()

                added shouldBe true
            }
        }
    }

    "explains hidden future entries and offers to reveal them" {
        onTestMain {
            runComposeUiTest {
                var revealed = false
                setContent {
                    LedgerEntriesView(
                        entries = emptyList(),
                        zone = TimeZone.UTC,
                        hideAmounts = false,
                        searchQuery = "",
                        typeFilter = null,
                        categoryFilter = null,
                        accountFilter = null,
                        amountFilter = null,
                        periodIsAll = false,
                        showFuture = false,
                        upcomingCount = 2,
                        hasAnyEntries = true,
                        paceHeader = null,
                        onEntryClick = {},
                        onAddFirstEntry = {},
                        onRevealUpcoming = { revealed = true },
                    )
                }

                onNodeWithText("Entries after today are hidden.").assertIsDisplayed()
                onNodeWithText("Show entries after today").performClick()

                revealed shouldBe true
            }
        }
    }

    "renders the list with the pace header and forwards row clicks" {
        onTestMain {
            runComposeUiTest {
                var clicked = false
                setContent {
                    LedgerEntriesView(
                        entries = listOf(coffee),
                        zone = TimeZone.UTC,
                        hideAmounts = false,
                        searchQuery = "",
                        typeFilter = null,
                        categoryFilter = null,
                        accountFilter = null,
                        amountFilter = null,
                        periodIsAll = false,
                        showFuture = false,
                        upcomingCount = 0,
                        hasAnyEntries = true,
                        paceHeader = { Text("Pace") },
                        onEntryClick = { clicked = true },
                        onAddFirstEntry = {},
                        onRevealUpcoming = {},
                    )
                }

                onNodeWithText("Pace").assertIsDisplayed()
                onNodeWithText("Aug 10, 2026").assertIsDisplayed()
                onNodeWithText("Coffee").performClick()

                clicked shouldBe true
            }
        }
    }
})
