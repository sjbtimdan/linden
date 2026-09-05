package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.assertTextContains
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.test.swipeUp
import androidx.compose.ui.unit.Dp
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.comparables.shouldBeLessThan
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
import org.sjbtimdan.linden.ui.withLedgerViewModel
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class LedgerScreenTest : StringSpec({
    "displays empty state" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "empty entries view offers adding the first entry" {
        withLedgerViewModel { viewModel ->
            var navigatedToEntry = false
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToEntry = { navigatedToEntry = true },
                )
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
            onNodeWithText("Add your first entry").assertIsDisplayed()

            onNodeWithText("Add your first entry").performClick()

            navigatedToEntry shouldBe true
        }
    }

    "accounts without entries still guide to the first entry" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            seed(accountDao, categoryDao)
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToEntry = { },
                )
            }

            onNodeWithText("No entries yet.").assertIsDisplayed()
            onNodeWithText("Add your first entry").assertIsDisplayed()
        }
    }

    "accounts view empty state offers creating an account" {
        withLedgerViewModel { viewModel ->
            var navigatedToAccounts = false
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToAccounts = { navigatedToAccounts = true },
                )
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithText("No accounts yet.").assertIsDisplayed()
            onNodeWithText("Create an account").assertIsDisplayed()

            onNodeWithText("Create an account").performClick()

            navigatedToAccounts shouldBe true
        }
    }

    "accounts view hides hidden accounts and explains when every account is hidden" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Old", Currency.CHF)
            accountDao.create("Also Old", Currency.CHF)
            val all = accountDao.getAll().first()
            val main = all.first { it.name == "Main" }
            val old = all.first { it.name == "Old" }
            val alsoOld = all.first { it.name == "Also Old" }
            accountDao.setHidden(old.id, true)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Also Old").assertIsDisplayed()
            onNodeWithText("Old").assertDoesNotExist()

            // Hiding the last visible account turns the empty state into an
            // explanation with a route back to the account manager.
            accountDao.setHidden(main.id, true)
            accountDao.setHidden(alsoOld.id, true)
            waitForIdle()

            onNodeWithText("All accounts are hidden.").assertIsDisplayed()
            onNodeWithText("Manage accounts").assertIsDisplayed()
            onNodeWithText("Main").assertDoesNotExist()
        }
    }

    "account filter dropdown offers only visible accounts" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Old", Currency.CHF)
            val old = accountDao.getAll().first().first { it.name == "Old" }
            accountDao.setHidden(old.id, true)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()
            onNodeWithTag("accountFilterDropdown").performClick()

            // The chip and the open menu both show the neutral label; only
            // visible accounts appear as options.
            onAllNodesWithText("Account: All").assertCountEquals(2)
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("Old").assertDoesNotExist()
        }
    }

    "categories view empty state offers adding categories" {
        withLedgerViewModel { viewModel ->
            var navigatedToCategories = false
            setContent {
                LedgerScreen(
                    viewModel = viewModel,
                    onNavigateToCategories = { navigatedToCategories = true },
                )
            }

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("No categories yet.").assertIsDisplayed()
            onNodeWithText("Add categories").assertIsDisplayed()

            onNodeWithText("Add categories").performClick()

            navigatedToCategories shouldBe true
        }
    }

    "defined categories without spending show a plain message" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            categoryDao.create("Groceries", CategoryType.Expense)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("No spending yet.").assertIsDisplayed()
            onNodeWithText("Add categories").assertDoesNotExist()
        }
    }

    "search narrows the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("searchField").performTextInput("lunch")

            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "show future toggle on the period navigator reveals future entries" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Today").assertIsDisplayed()
            onNodeWithText("Scheduled").assertDoesNotExist()

            onNodeWithTag("showFutureToggle").performClick()

            onNodeWithText("Scheduled").assertIsDisplayed()
            onNodeWithText("Today").assertIsDisplayed()
        }
    }

    "show-future notice explains what the toggle revealed and dismisses it" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("showFutureToggle").performClick()

            onNodeWithText("Scheduled").assertIsDisplayed()
            onNodeWithText("Showing 1 entry after today").assertIsDisplayed()

            onNodeWithTag("showFutureNotice").performClick()

            onNodeWithText("Scheduled").assertDoesNotExist()
            onNodeWithText("Showing 1 entry after today").assertDoesNotExist()
        }
    }

    "empty entries list explains when only future entries are hidden" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Entries after today are hidden.").assertIsDisplayed()

            onNodeWithText("Show entries after today").performClick()

            onNodeWithText("Scheduled").assertIsDisplayed()
            onNodeWithText("Showing 1 entry after today").assertIsDisplayed()
        }
    }

    "categories view names hidden future spending and explains totals once shown" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("Spending after today is hidden.").assertIsDisplayed()

            onNodeWithTag("showFutureToggle").performClick()

            onNodeWithText("Totals include entries after today").assertIsDisplayed()
            onNodeWithText("Spending after today is hidden.").assertDoesNotExist()
        }
    }

    "accounts view notice says balances include entries after today" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )
            viewModel.setShowFuture(true)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithText("Balances include entries after today").assertIsDisplayed()
        }
    }

    "type filter narrows the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Refund").assertIsDisplayed()

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "no entries match message when nothing matches" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("searchField").performTextInput("zzz")

            onNodeWithText("No entries match.").assertIsDisplayed()
            onNodeWithText("Add your first entry").assertDoesNotExist()
        }
    }

    "editing an entry shows current values and saves changes" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Edit Expense").assertIsDisplayed()

            onNodeWithText("Amount").performClick()
            waitForIdle()
            // Clear the prefilled current amount before typing the new one.
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
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").performClick()
            onNodeWithText("Delete").performClick()

            onNodeWithText("No entries yet.").assertIsDisplayed()
        }
    }

    "day headers appear when entries span multiple days" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
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
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("10 Sep 2001").assertIsDisplayed()
            onNodeWithText("9 Sep 2001").assertIsDisplayed()
        }
    }

    "day header sticks flush to the top of the list while scrolling" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
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
                LedgerScreen(viewModel = viewModel)
            }

            repeat(3) { onNodeWithTag("entryList").performTouchInput { swipeUp() } }
            waitForIdle()

            val listTop = onNodeWithTag("entryList").getUnclippedBoundsInRoot().top
            var pinnedTop: Dp? = null
            for (day in 8..12) {
                runCatching {
                    val top = onNodeWithText("$day Sep 2001").getUnclippedBoundsInRoot().top
                    if (pinnedTop == null || top < pinnedTop) pinnedTop = top
                }
            }
            // The pinned date must sit flush at the top of the list, not over the first entry.
            pinnedTop shouldBe listTop
        }
    }

    "selecting a period narrows the list" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Jul", main, 200, createdAt = Instant.parse("2026-07-01T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
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
        withLedgerViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Sep", main, 200, createdAt = Instant.parse("2026-09-10T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
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
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "In Aug", main, 100, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Year").performClick()
            onNodeWithContentDescription("Previous period").performClick()

            onNodeWithText("2025").assertIsDisplayed()
            onNodeWithText("No entries match.").assertIsDisplayed()
        }
    }

    "month view omits per-row timestamps under day headers, the day view shows them" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Morning", main, 100, createdAt = Instant.parse("2026-08-15T08:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Lunch", main, 200, createdAt = Instant.parse("2026-08-15T12:30:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Dinner", main, 300, createdAt = Instant.parse("2026-08-15T19:00:00Z")),
            )

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            // Month: the day header carries the date, so the rows stay timestamp-free.
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            onNodeWithText("15 Aug 2026").assertIsDisplayed()
            onNodeWithText("15 Aug 2026, 08:00").assertDoesNotExist()

            // Day: the timestamp is the only thing telling the rows apart.
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Day").performClick()

            onNodeWithText("15 Aug 2026, 08:00").assertIsDisplayed()
            onNodeWithText("15 Aug 2026, 12:30").assertIsDisplayed()
            onNodeWithText("15 Aug 2026, 19:00").assertIsDisplayed()
        }
    }

    "shows the net total of the period next to the navigator" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("+ 15.50 CHF").assertIsDisplayed()
        }
    }

    "shows the total converted to the default currency" {
        withLedgerViewModel(
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
                LedgerScreen(viewModel = viewModel)
            }

            // 450 CHF + 200 USD / 2.0 = 550 CHF
            onNodeWithText("− 5.50 CHF").assertIsDisplayed()
        }
    }

    "shows a dash when the total cannot be computed" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Groceries", CategoryType.Expense)
            val usd = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("–").assertIsDisplayed()
        }
    }

    "hides the top total when Hide Totals is enabled" {
        withLedgerViewModel(
            defaultCurrency = Currency.CHF,
            rates = emptyList(),
        ) { entryDao, accountDao, categoryDao, settingsDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            // Both the entry row and the top total show the amount.
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
            onAllNodesWithText("***").assertCountEquals(0)

            settingsDao.setHideEntryTotal(true)
            waitForIdle()

            // Hiding masks the total with a *** placeholder; only the row amount stays.
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(1)
            onNodeWithText("***").assertIsDisplayed()
        }
    }

    "mode tabs switch to the accounts view and back" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Refund").assertIsDisplayed()

            onNodeWithTag("viewModeTab-Accounts").performClick()

            // Accounts view: balance rows replace entries; tabs and period stay, filters hide.
            onNodeWithText("Main").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertIsDisplayed()
            onNodeWithText("+ 20.00 CHF").assertIsDisplayed()
            onNodeWithText("Refund").assertDoesNotExist()
            onNodeWithTag("viewModeTab-Entries").assertIsDisplayed()
            onNodeWithTag("periodLabel").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertDoesNotExist()

            // Expanding the panel in the accounts view only reveals the search field.
            expandFilters()

            onNodeWithTag("searchField").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertDoesNotExist()

            onNodeWithTag("viewModeTab-Entries").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertDoesNotExist()
        }
    }

    "type filter is hidden in the accounts view and preserved across switches" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("activeTypeFilterChip").assertIsDisplayed()

            // The type filter does not apply to balances, so it is hidden in the accounts view.
            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithTag("typeFilterDropdown").assertDoesNotExist()
            onNodeWithTag("activeTypeFilterChip").assertDoesNotExist()

            onNodeWithTag("viewModeTab-Entries").performClick()

            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "search filters accounts by name in the accounts view" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            val groceries = categoryDao.getAll().first().first()
            viewModel.createEntry(IncomeEntry(0, groceries, "Pay", main, 2_000))
            viewModel.createEntry(IncomeEntry(0, groceries, "Pay", usd, 200))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()
            expandFilters()

            onNodeWithTag("searchField").performTextInput("usd")

            onNodeWithText("2.00 $").assertIsDisplayed()
            onNodeWithText("20.00 CHF").assertDoesNotExist()
        }
    }

    "accounts view shows balances at the end of the selected period" {
        withLedgerViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
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
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()
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
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithText("No accounts yet.").assertIsDisplayed()
        }
    }

    "mode tabs switch to the categories view and back" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithText("Coffee").assertIsDisplayed()

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
            onNodeWithText("Coffee").assertDoesNotExist()

            // In the categories view the panel shows search and the type filter inline only.
            expandFilters()

            onNodeWithTag("searchField").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
            onNodeWithTag("categoryFilterDropdown").assertDoesNotExist()

            onNodeWithTag("viewModeTab-Entries").performClick()

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("1 entry").assertDoesNotExist()
        }
    }

    "categories view shows the empty state when there are no entries" {
        withLedgerViewModel { viewModel ->
            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("No categories yet.").assertIsDisplayed()
        }
    }

    "search filters categories by name in the categories view" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()
            expandFilters()

            onNodeWithTag("searchField").performTextInput("salary")

            onNodeWithText("Salary").assertIsDisplayed()
            onNodeWithText("Groceries").assertDoesNotExist()
        }
    }

    "categories view shows totals at the end of the selected period" {
        withLedgerViewModel(today = { LocalDate(2026, 9, 15) }) { accountDao, categoryDao, viewModel ->
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
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()
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
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()
            onNodeWithText("Groceries").performClick()

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Pay").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertIsDisplayed()
            onNodeWithText("− 16.50 CHF").assertIsDisplayed()
        }
    }

    "removing the category filter restores the full entries list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Pay").assertDoesNotExist()

            onNodeWithTag("categoryFilterChip").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithTag("categoryFilterChip").assertDoesNotExist()
        }
    }

    "category dropdown filters the entries and shows the chip" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("categoryFilterDropdown").performClick()
            onNodeWithText("Salary").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertIsDisplayed()
        }
    }

    "account dropdown filters the entries and shows the chip" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("accountFilterDropdown").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("No entries match this filter.").assertIsDisplayed()
            onNodeWithTag("accountFilterChip").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
        }
    }

    "tapping an account drills into its entries with a filter chip" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Bonus", savings, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()
            onNodeWithText("Savings").performClick()

            onNodeWithText("Bonus").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("accountFilterChip").assertIsDisplayed()
        }
    }

    "view entries in the overflow menu drills into the account" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Bonus", savings, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()
            // Main is the first row; the overflow of the Savings row is the second.
            onAllNodesWithContentDescription("More options")[1].performClick()
            onNodeWithText("View entries").performClick()

            onNodeWithText("Bonus").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("accountFilterChip").assertIsDisplayed()
        }
    }

    "accounts view hamburger menu opens the Adjust Balance dialog" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onNodeWithText("Adjust Balance").assertIsDisplayed()
            onNodeWithText("Current balance: 100.00 CHF").assertIsDisplayed()
        }
    }

    "adjusting to a higher balance in the accounts view creates an income entry" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onAllNodes(hasSetTextAction())[0].performTextClearance()
            onAllNodes(hasSetTextAction())[0].performTextInput("125.00")
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Adjust").performClick()

            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(1)
            entries.first().amount shouldBe 2_500
        }
    }

    "adjusting to a lower balance in the accounts view creates an expense entry" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onAllNodes(hasSetTextAction())[0].performTextClearance()
            onAllNodes(hasSetTextAction())[0].performTextInput("90.00")
            onNodeWithText("Groceries").performClick()
            onNodeWithText("Adjust").performClick()

            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(1)
            entries.first().amount shouldBe 1_000
        }
    }

    "adjusting to the same balance in the accounts view is disabled" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onNodeWithText("Adjust").assertIsNotEnabled()
        }
    }

    "typing in the category field filters the category chips" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onNodeWithText("Groceries").assertIsDisplayed()
            onNodeWithText("Salary").assertIsDisplayed()

            onAllNodes(hasSetTextAction())[1].performTextInput("sal")

            onNodeWithText("Salary").assertIsDisplayed()
            onNodeWithText("Groceries").assertDoesNotExist()
        }
    }

    "clicking a category chip shows the category in the text field" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").performClick()

            onNodeWithText("Groceries").performClick()

            onAllNodes(hasSetTextAction())[1].assertTextContains("Groceries")
        }
    }

    "adjust balance shows an explanation when viewing a historical period" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            // Navigate to the previous, historical month.
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()
            onNodeWithContentDescription("Previous period").performClick()

            // The action stays visible but disabled, reason inline — no dead-end dialog.
            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").assertIsNotEnabled()
            onNodeWithText("Only available when the latest period is shown.").assertIsDisplayed()
            onNodeWithText("Adjust Balance can only be done when the period is the latest.").assertDoesNotExist()
        }
    }

    "adjust balance is shown for the current period" {
        withLedgerViewModel(today = { LocalDate(2026, 8, 15) }) { accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Accounts").performClick()

            // The current month includes today, so Adjust Balance stays available.
            onNodeWithTag("periodLabel").performClick()
            onNodeWithText("Month").performClick()

            onNodeWithContentDescription("More options").performClick()
            onNodeWithText("Adjust Balance").assertIsDisplayed()
        }
    }

    "filters are collapsed by default, keeping the tabs, period and total visible" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("searchField").assertDoesNotExist()
            onNodeWithTag("typeFilterDropdown").assertDoesNotExist()
            onNodeWithTag("amountFilterChip").assertDoesNotExist()
            onNodeWithTag("viewModeTab-Entries").assertIsDisplayed()
            onNodeWithTag("viewModeTab-Accounts").assertIsDisplayed()
            onNodeWithTag("viewModeTab-Categories").assertIsDisplayed()
            onNodeWithTag("periodLabel").assertIsDisplayed()
            // The expense row and the period total both show the amount.
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
        }
    }

    "expanding the filters shows search and the inline filter chips" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("searchField").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
            onNodeWithTag("categoryFilterDropdown").assertIsDisplayed()
            onNodeWithTag("accountFilterDropdown").assertIsDisplayed()
            onNodeWithTag("amountFilterChip").assertIsDisplayed()
            onNodeWithTag("periodLabel").assertIsDisplayed()
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
        }
    }

    "collapsing the filters hides search and chips but keeps the tabs, period and total" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("searchField").assertIsDisplayed()

            onNodeWithTag("filtersHeader").performClick()
            waitForIdle()

            onNodeWithTag("searchField").assertDoesNotExist()
            onNodeWithTag("typeFilterDropdown").assertDoesNotExist()
            onNodeWithTag("amountFilterChip").assertDoesNotExist()
            onNodeWithTag("viewModeTab-Entries").assertIsDisplayed()
            onNodeWithTag("periodLabel").assertIsDisplayed()
            onNodeWithText("Coffee").assertIsDisplayed()
            onAllNodesWithText("− 4.50 CHF").assertCountEquals(2)
        }
    }

    "collapsing the filters reclaims vertical space for the list" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            val expandedTop = onNodeWithTag("entryList").getUnclippedBoundsInRoot().top

            onNodeWithTag("filtersHeader").performClick()
            waitForIdle()

            val collapsedTop = onNodeWithTag("entryList").getUnclippedBoundsInRoot().top
            collapsedTop shouldBeLessThan expandedTop
        }
    }

    "search icon in the collapsed header opens the panel and focuses the search field" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("searchField").assertDoesNotExist()

            onNodeWithContentDescription("Search").performClick()
            waitForIdle()

            onNodeWithTag("searchField").assertIsDisplayed()
        }
    }

    "collapsed filters still summarize active filters with removable chips" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithTag("filtersHeader").performClick()
            waitForIdle()

            onNodeWithTag("searchField").assertDoesNotExist()
            onNodeWithTag("activeTypeFilterChip").assertIsDisplayed()
            onNodeWithText("Refund").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()

            onNodeWithTag("activeTypeFilterChip").performClick()

            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithTag("activeTypeFilterChip").assertDoesNotExist()
        }
    }

    "clear all removes every active filter from the summary row" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            onNodeWithTag("categoryFilterDropdown").performClick()
            onNodeWithText("Salary").performClick()

            // Both filters apply: only the income entry in Salary is left.
            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onNodeWithTag("activeTypeFilterChip").assertIsDisplayed()
            onNodeWithTag("categoryFilterChip").assertIsDisplayed()

            onNodeWithTag("clearAllFilters").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithTag("activeTypeFilterChip").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertDoesNotExist()
        }
    }

    "choosing All in the inline dropdowns clears the applied filters" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()
            onNodeWithTag("categoryFilterDropdown").performClick()
            onNodeWithText("Salary").performClick()

            // Both filters apply: only the income entry in Salary is left.
            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()

            // Each dropdown resets its own filter to All — no dialog.
            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Types: All").performClick()
            onNodeWithTag("categoryFilterDropdown").performClick()
            onNodeWithText("Category: All").performClick()

            onNodeWithText("Pay").assertIsDisplayed()
            onNodeWithText("Coffee").assertIsDisplayed()
            onNodeWithTag("activeTypeFilterChip").assertDoesNotExist()
            onNodeWithTag("categoryFilterChip").assertDoesNotExist()
        }
    }

    "search label describes what it filters in each view" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithText("Search entries").assertIsDisplayed()

            onNodeWithTag("viewModeTab-Accounts").performClick()

            onNodeWithText("Filter accounts").assertIsDisplayed()

            onNodeWithTag("viewModeTab-Categories").performClick()

            onNodeWithText("Filter categories").assertIsDisplayed()

            onNodeWithTag("viewModeTab-Entries").performClick()

            onNodeWithText("Search entries").assertIsDisplayed()
        }
    }

    "the applied type filter reads back on the inline chip" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("typeFilterDropdown").performClick()
            onNodeWithText("Income").performClick()

            // The chip label and the summary row both report the narrowing; the
            // dropdown reopens showing Income as the checked option.
            onNodeWithTag("activeTypeFilterChip").assertIsDisplayed()
            onNodeWithTag("typeFilterDropdown").performClick()
            onNode(hasText("Income") and hasContentDescription("Selected")).assertIsDisplayed()
            onNode(hasText("Expense") and hasContentDescription("Selected")).assertDoesNotExist()
        }
    }

    "categories view shows only the type filter inline" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            onNodeWithTag("viewModeTab-Categories").performClick()
            expandFilters()

            onNodeWithTag("typeFilterDropdown").assertIsDisplayed()
            onNodeWithTag("categoryFilterDropdown").assertDoesNotExist()
            onNodeWithTag("accountFilterDropdown").assertDoesNotExist()
            onNodeWithTag("amountFilterChip").assertDoesNotExist()
        }
    }

    "amount filter can be applied from the inline chip" {
        withLedgerViewModel { accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            setContent {
                LedgerScreen(viewModel = viewModel)
            }

            expandFilters()

            onNodeWithTag("amountFilterChip").performClick()
            onNodeWithTag("amountFilterValue").performTextInput("10.00")
            onNodeWithText("Apply").performClick()

            // Only the entry over 10.00 remains; the inline chip and the summary
            // chip below the period bar both show the filter.
            onNodeWithText("Lunch").assertIsDisplayed()
            onNodeWithText("Coffee").assertDoesNotExist()
            onAllNodesWithText("> 10").assertCountEquals(2)
            onNodeWithTag("activeAmountFilterChip").assertIsDisplayed()
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

/** The search/filter panel starts collapsed; tests that use it open it first. */
@OptIn(ExperimentalTestApi::class)
private fun ComposeUiTest.expandFilters() {
    onNodeWithTag("filtersHeader").performClick()
    waitForIdle()
}
