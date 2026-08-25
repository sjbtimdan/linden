package org.sjbtimdan.linden.ui.history

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasScrollAction
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withHistoryViewModel
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class HistoryScreenTest : StringSpec({
    "displays empty state" {
        withHistoryViewModel { viewModel ->
            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "search narrows the list" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Search").performTextInput("lunch")

            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "type filter narrows the list" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Refund").assertIsDisplayed()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "no entries match message when nothing matches" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Search").performTextInput("zzz")

            onNodeWithText("No entries match.").assertIsDisplayed()
        }
    }

    "editing an entry shows current values and saves changes" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Edit Expense").assertIsDisplayed()

            onNodeWithText("Amount").performClick()
            waitForIdle()
            // The calculator opens prefilled with the current amount; clear and retype.
            onNodeWithText("C").performClick()
            onNodeWithText("5").performClick()
            onNodeWithText(".").performClick()
            onNodeWithText("0").performClick()
            onNodeWithText("0").performClick()
            onNodeWithText("Enter").performClick()
            waitForIdle()
            onNodeWithText("Save").performClick()

            // The edited row and the period total both show the new amount.
            onAllNodesWithText("− 5.00 CHF").assertCountEquals(2)
            onNodeWithText("− 4.50 CHF").assertDoesNotExist()
        }
    }

    "deleting an entry from the edit dialog removes it" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Delete").performClick()

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "day headers appear when entries span multiple days" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(
                    0,
                    groceries,
                    "Coffee",
                    main,
                    450,
                    createdAt = Instant.fromEpochMilliseconds(1_000_000_000_000),
                ),
            )
            viewModel.createEntry(
                ExpenseEntry(
                    0,
                    groceries,
                    "Lunch",
                    main,
                    1_200,
                    createdAt = Instant.fromEpochMilliseconds(1_000_086_400_000),
                ),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("10 Sep 2001").assertIsDisplayed()
            onNodeWithText("9 Sep 2001").assertIsDisplayed()
        }
    }

    "day header sticks flush to the top of the list while scrolling" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            for (day in 8..12) {
                repeat(4) { hour ->
                    viewModel.createEntry(
                        ExpenseEntry(
                            0,
                            groceries,
                            "E$day-$hour",
                            main,
                            100,
                            createdAt = Instant.parse("2001-09-${day.toString().padStart(2, '0')}T12:00:00Z"),
                        ),
                    )
                }
            }

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            repeat(3) { onNode(hasScrollAction()).performTouchInput { swipeUp() } }
            waitForIdle()

            val listTop = onNode(hasScrollAction()).getUnclippedBoundsInRoot().top
            var pinnedTop: Dp? = null
            for (day in 8..12) {
                runCatching {
                    val top = onNodeWithText("$day Sep 2001").getUnclippedBoundsInRoot().top
                    if (pinnedTop == null || top < pinnedTop!!) pinnedTop = top
                }
            }
            // The pinned date must sit flush at the top of the list, not over the first entry.
            pinnedTop shouldBe listTop
        }
    }

    "selecting a period narrows the list" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Jul", main, 200, createdAt = Instant.parse("2026-07-01T12:00:00Z")),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("In Aug").assertIsDisplayed()
            onNodeWithText("In Jul").assertIsDisplayed()

            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            onNodeWithText("Aug 2026").assertIsDisplayed()
            onNodeWithText("In Aug").assertIsDisplayed()
            onNodeWithText("In Jul").assertDoesNotExist()
        }
    }

    "arrows move between periods" {
        withHistoryViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Sep", main, 200, createdAt = Instant.parse("2026-09-10T12:00:00Z")),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            onNodeWithText("Sep 2026").assertIsDisplayed()
            onNodeWithText("In Sep").assertIsDisplayed()
            onNodeWithText("In Aug").assertDoesNotExist()

            onNodeWithContentDescription("Previous period").performClick()

            onNodeWithText("Aug 2026").assertIsDisplayed()
            onNodeWithText("In Aug").assertIsDisplayed()
            onNodeWithText("In Sep").assertDoesNotExist()

            onNodeWithContentDescription("Next period").performClick()

            onNodeWithText("Sep 2026").assertIsDisplayed()
            onNodeWithText("In Sep").assertIsDisplayed()
            onNodeWithText("In Aug").assertDoesNotExist()
        }
    }

    "period with no entries shows no entries match" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Year").performClick()
            onNodeWithContentDescription("Previous period").performClick()

            onNodeWithText("2025").assertIsDisplayed()
            onNodeWithText("No entries match.").assertIsDisplayed()
        }
    }

    "shows the net total of the period next to the navigator" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("+ 15.50 CHF").assertIsDisplayed()
        }
    }

    "shows the total converted to the default currency" {
        withHistoryViewModel(
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-08-13")),
        ) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            // 450 CHF + 200 USD / 2.0 = 550 CHF
            onNodeWithText("− 5.50 CHF").assertIsDisplayed()
        }
    }

    "shows a dash when the total cannot be computed" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Groceries", CategoryType.Expense)
            val usd = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("–").assertIsDisplayed()
        }
    }

    "view dropdown switches to the accounts view and back" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Refund").assertIsDisplayed()

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Accounts").performClick()

            // Accounts view: the balance row replaces the entries, but the search
            // field stays; the type filter is disabled because it does not apply.
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertIsDisplayed()
            onNodeWithText("+ 20.00 CHF").assertIsDisplayed()
            onNodeWithText("Refund").assertDoesNotExist()
            onNodeWithText("Search").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertIsNotEnabled()

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Entries").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertDoesNotExist()
        }
    }

    "type filter is disabled in the accounts view and preserved across switches" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()

            // The type filter does not apply to balances, so its dropdown is disabled.
            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Accounts").performClick()

            onNodeWithTag("typeFilterDropdown").assertIsNotEnabled()

            // Switching back keeps the filter.
            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Entries").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "search filters accounts by name in the accounts view" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(IncomeEntry(0, groceries, "Pay", main, 2_000))
            viewModel.createEntry(IncomeEntry(0, groceries, "Pay", usd, 200))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Accounts").performClick()
            onNodeWithText("Search").performTextInput("usd")

            onNodeWithText("2.00 $").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertDoesNotExist()
        }
    }

    "accounts view shows balances at the end of the selected period" {
        withHistoryViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Jul", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Aug", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Sep", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Accounts").performClick()
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            // Anchor is today (2026-09-15): September's window includes all three entries.
            onNodeWithText("-6.00 CHF").assertIsDisplayed()

            onNodeWithContentDescription("Previous period").performClick()

            // August's window stops at the end of the month: July and August only.
            onNodeWithText("-3.00 CHF").assertIsDisplayed()
            onNodeWithText("-6.00 CHF").assertDoesNotExist()
        }
    }

    "accounts view shows the empty state when there are no accounts" {
        withHistoryViewModel { viewModel ->
            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Accounts").performClick()

            onNodeWithText("No accounts yet.").assertIsDisplayed()
        }
    }

    "view dropdown switches to the categories view and back" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithText("Search").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertIsDisplayed()

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Entries").performClick()

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("1 entry").assertDoesNotExist()
        }
    }

    "categories view shows the empty state when there are no entries" {
        withHistoryViewModel { viewModel ->
            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()

            onNodeWithText("No categories yet.").assertIsDisplayed()
        }
    }

    "search filters categories by name in the categories view" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithText("Search").performTextInput("salary")

            onNodeWithText("Salary").assertIsDisplayed()
            onNodeWithText("Groceries").assertDoesNotExist()
        }
    }

    "categories view shows totals at the end of the selected period" {
        withHistoryViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Jul", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Aug", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Sep", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")),
            )

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            // September window: only Sep entry
            onAllNodesWithText("− 3.00 CHF").assertCountEquals(2)

            onNodeWithContentDescription("Previous period").performClick()

            // August window: only Aug entry (Jul 31 is before Aug 1)
            onAllNodesWithText("− 2.00 CHF").assertCountEquals(2)
            onNodeWithText("− 3.00 CHF").assertDoesNotExist()
        }
    }

    "tapping a category drills into its entries with a filter chip" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithText("Groceries").performClick()

            // The entries view is narrowed to Groceries and the chip is visible.
            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Pay").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertIsDisplayed()
            onNodeWithText("− 16.50 CHF").assertIsDisplayed()
        }
    }

    "removing the category filter restores the full entries list" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeDropdown").performClick()
            onNodeWithText("Categories").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Pay").assertDoesNotExist()

            onNodeWithTag("categoryFilterChip").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithTag("categoryFilterChip").assertDoesNotExist()
        }
    }

    "category dropdown filters the entries and shows the chip" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("categoryFilterDropdown").performClick()
            onNodeWithText("Salary").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertIsDisplayed()
        }
    }

    "account dropdown filters the entries and shows the chip" {
        withHistoryViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                HistoryScreen(viewModel = viewModel)
            }

            onNodeWithTag("accountFilterDropdown").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("No entries match this filter.").assertIsDisplayed()
            onNodeWithTag("accountFilterChip").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }
})

private suspend fun seed(accountDao: AccountDao, categoryDao: CategoryDao): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}
