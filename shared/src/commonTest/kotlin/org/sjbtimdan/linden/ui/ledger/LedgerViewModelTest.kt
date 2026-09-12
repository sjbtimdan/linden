package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.onTestMain
import org.sjbtimdan.linden.ui.testAllEntries
import org.sjbtimdan.linden.ui.testRatesProvider
import org.sjbtimdan.linden.ui.withLedgerViewModel
import kotlin.time.Instant

@OptIn(ExperimentalTestApi::class)
class LedgerViewModelTest : StringSpec({
    "entries start empty" {
        withLedgerViewModel(clock = FakeClock()) { viewModel ->
            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "hasAnyEntries is false while no entry exists and becomes true with the first one" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            viewModel.hasAnyEntries.value shouldBe false

            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            entryDao.getAll().first().shouldHaveSize(1)
            viewModel.hasAnyEntries.first { it } shouldBe true
        }
    }

    "defaults to the current month period" {
        onTestMain {
            runComposeUiTest {
                val database = lindenDatabase()
                val entryDao = EntryDao(database.entryQueries)
                val settingsDao = SettingsDao(database.settingsQueries)
                val viewModel = LedgerViewModel(
                    entryDao,
                    AccountDao(database.accountQueries),
                    CategoryDao(database.categoryQueries),
                    settingsDao,
                    BudgetDao(database.budgetQueries),
                    testRatesProvider(settingsDao, FxRateDao(database.fxRateQueries)),
                    testAllEntries(entryDao),
                    clock = FakeClock(today = LocalDate(2026, 8, 15)),
                )
                viewModel.periodSelection.value.period shouldBe LedgerPeriod.Month
            }
        }
    }

    "categoryTotals carries the budget limit in a month period" {
        onTestMain {
            runComposeUiTest {
                val database = lindenDatabase()
                val accountDao = AccountDao(database.accountQueries)
                val categoryDao = CategoryDao(database.categoryQueries)
                val budgetDao = BudgetDao(database.budgetQueries)
                val settingsDao = SettingsDao(database.settingsQueries)
                val entryDao = EntryDao(database.entryQueries)
                val viewModel = LedgerViewModel(
                    entryDao,
                    accountDao,
                    categoryDao,
                    settingsDao,
                    budgetDao,
                    testRatesProvider(settingsDao, FxRateDao(database.fxRateQueries)),
                    testAllEntries(entryDao),
                    clock = FakeClock(today = LocalDate(2026, 8, 15)),
                )
                val (main, groceries) = seed(accountDao, categoryDao)
                viewModel.createEntry(
                    ExpenseEntry(
                        0,
                        groceries,
                        "Coffee",
                        main,
                        450,
                        createdAt = Instant.parse("2026-08-10T12:00:00Z"),
                    ),
                )
                budgetDao.upsert("Groceries", 1_000)

                val groceriesTotal = viewModel.categoryTotals.value.first { it.category?.name == "Groceries" }
                groceriesTotal.budget shouldBe 1_000
            }
        }
    }

    "categoryTotals drops the budget limit outside a month period" {
        onTestMain {
            runComposeUiTest {
                val database = lindenDatabase()
                val accountDao = AccountDao(database.accountQueries)
                val categoryDao = CategoryDao(database.categoryQueries)
                val budgetDao = BudgetDao(database.budgetQueries)
                val settingsDao = SettingsDao(database.settingsQueries)
                val entryDao = EntryDao(database.entryQueries)
                val viewModel = LedgerViewModel(
                    entryDao,
                    accountDao,
                    categoryDao,
                    settingsDao,
                    budgetDao,
                    testRatesProvider(settingsDao, FxRateDao(database.fxRateQueries)),
                    testAllEntries(entryDao),
                    clock = FakeClock(today = LocalDate(2026, 8, 15)),
                )
                val (main, groceries) = seed(accountDao, categoryDao)
                viewModel.createEntry(
                    ExpenseEntry(
                        0,
                        groceries,
                        "Coffee",
                        main,
                        450,
                        createdAt = Instant.parse("2026-08-10T12:00:00Z"),
                    ),
                )
                budgetDao.upsert("Groceries", 1_000)

                viewModel.categoryTotals.value.first().budget shouldBe 1_000

                viewModel.setPeriod(LedgerPeriod.All)
                viewModel.categoryTotals.value.first { it.category?.name == "Groceries" }.budget.shouldBeNull()
            }
        }
    }

    "search filters by description and account name" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.setSearchQuery("coffee")
            viewModel.entries.value.map { it.description } shouldBe listOf("Coffee")

            viewModel.setSearchQuery("main")
            viewModel.entries.value.shouldHaveSize(2)

            viewModel.setSearchQuery("zzz")
            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "search matches a transfer's to-account name" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(
                TransferEntry(
                    id = 0,
                    category = null,
                    description = "Move",
                    account = main,
                    amount = 500,
                    toAccount = savings,
                    toAmount = null,
                ),
            )

            viewModel.setSearchQuery("savings")
            viewModel.entries.value.map { it.description } shouldBe listOf("Move")

            viewModel.setSearchQuery("nope")
            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "amount filter greater-than keeps only larger amounts" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.setAmountFilter(AmountFilter(AmountOperator.GreaterThan, 500))

            viewModel.entries.value.map { it.description } shouldBe listOf("Lunch")
        }
    }

    "amount filter less-than keeps only smaller amounts" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.setAmountFilter(AmountFilter(AmountOperator.LessThan, 500))

            viewModel.entries.value.map { it.description } shouldBe listOf("Coffee")
        }
    }

    "amount filter approximately keeps amounts within 5%" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Low", main, 1_900))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Mid", main, 2_000))
            viewModel.createEntry(ExpenseEntry(0, groceries, "High", main, 2_100))
            viewModel.createEntry(ExpenseEntry(0, groceries, "TooLow", main, 1_899))
            viewModel.createEntry(ExpenseEntry(0, groceries, "TooHigh", main, 2_101))

            viewModel.setAmountFilter(AmountFilter(AmountOperator.Approximately, 2_000))

            viewModel.entries.value.map { it.description }.filterNotNull().sorted() shouldBe listOf(
                "High",
                "Low",
                "Mid",
            )
        }
    }

    "amount filter matches a transfer's to-amount" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(
                TransferEntry(
                    id = 0,
                    category = null,
                    description = "Move",
                    account = main,
                    amount = 500,
                    toAccount = savings,
                    toAmount = 480,
                ),
            )

            viewModel.setAmountFilter(AmountFilter(AmountOperator.GreaterThan, 470))

            viewModel.entries.value.map { it.description } shouldBe listOf("Move")
        }
    }

    "amount filter composes with text search" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 1_200))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.setSearchQuery("coffee")
            viewModel.setAmountFilter(AmountFilter(AmountOperator.GreaterThan, 500))

            viewModel.entries.value.map { it.description } shouldBe listOf("Coffee")
        }
    }

    "clearing the amount filter restores all amounts" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.setAmountFilter(AmountFilter(AmountOperator.GreaterThan, 500))
            viewModel.entries.value.shouldHaveSize(1)

            viewModel.clearAmountFilter()
            viewModel.entries.value.shouldHaveSize(2)
        }
    }

    "type filter keeps only matching entries" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            viewModel.setTypeFilter(EntryType.Income)

            viewModel.entries.value.shouldHaveSize(1)
            viewModel.entries.value.first().type shouldBe EntryType.Income

            viewModel.setTypeFilter(null)
            viewModel.entries.value.shouldHaveSize(2)
        }
    }

    "entries are always latest first by createdAt then id" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Oldest", main, 100, createdAt = at(1_000)),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Middle", main, 500, createdAt = at(2_000)),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Newest", main, 900, createdAt = at(3_000)),
            )

            viewModel.entries.value.map { it.description } shouldBe listOf("Newest", "Middle", "Oldest")
        }
    }

    "update reflects in the list" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            val updated = created.copy(description = "Tea", amount = 300)

            viewModel.updateEntry(updated)

            viewModel.entries.value.first() shouldBe updated
        }
    }

    "delete removes an entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Tea", main, 300))
            val created = viewModel.entries.value.first()

            viewModel.deleteEntry(created.id)

            viewModel.entries.value.shouldHaveSize(1)
            viewModel.entries.value.first().description shouldBe "Coffee"
        }
    }

    "direct database writes reflect in the list" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            entryDao.create(ExpenseEntry(0, groceries, "Direct", main, 100))

            viewModel.entries.value.shouldHaveSize(1)
        }
    }

    "month period shows only entries in the window and navigates" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 9, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Before", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Inside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "After", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")),
            )

            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.entries.value.map { it.description } shouldBe listOf("After")

            viewModel.goToNextPeriod()
            viewModel.entries.value.shouldBeEmpty()

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("After")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Inside")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Before")

            viewModel.setPeriod(LedgerPeriod.All)
            viewModel.entries.value.shouldHaveSize(3)
        }
    }

    "week period shows only entries in the calendar week" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 16)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "SunBefore", main, 100, createdAt = Instant.parse("2026-08-09T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "MonInside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "SunInside", main, 300, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "MonAfter", main, 400, createdAt = Instant.parse("2026-08-17T12:00:00Z")),
            )

            viewModel.setPeriod(LedgerPeriod.Week)

            viewModel.entries.value.map { it.description } shouldBe listOf("SunInside", "MonInside")
        }
    }

    "day period shows only entries on the anchor day and navigates" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "SameDay", main, 200, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Yesterday", main, 100, createdAt = Instant.parse("2026-08-14T12:00:00Z")),
            )

            viewModel.setPeriod(LedgerPeriod.Day)
            viewModel.entries.value.map { it.description } shouldBe listOf("SameDay")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Yesterday")

            viewModel.setPeriod(LedgerPeriod.All)
            viewModel.entries.value.shouldHaveSize(2)
        }
    }

    "future entries are excluded from the ledger" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            viewModel.entries.value.map { it.description } shouldBe listOf("Today")
        }
    }

    "showFuture includes future entries in the entries list" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            viewModel.setShowFuture(true)

            viewModel.entries.value.map { it.description } shouldBe listOf("Future", "Today")
        }
    }

    "showFuture includes future entries in account balances" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            viewModel.setShowFuture(true)

            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -300L))
        }
    }

    "showFuture includes future entries in category totals" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            viewModel.setShowFuture(true)

            viewModel.categoryTotals.value.first().total shouldBe -300L
        }
    }

    "upcomingCount counts future entries inside the window only" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Past", main, 100, createdAt = Instant.parse("2026-08-01T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(
                    0,
                    groceries,
                    "Later this month",
                    main,
                    100,
                    createdAt = Instant.parse("2026-08-20T12:00:00Z"),
                ),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Next month", main, 100, createdAt = Instant.parse("2026-09-02T12:00:00Z")),
            )

            // The harness starts on All: every future-dated entry counts.
            viewModel.upcomingCount.value shouldBe 2

            // The month window is anchored on 2026-08-15, so "Next month" falls outside it.
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.upcomingCount.value shouldBe 1

            // Last month has no entries at all, future or otherwise.
            viewModel.goToPreviousPeriod()
            viewModel.upcomingCount.value shouldBe 0
        }
    }

    "upcomingCount is zero when future entries sit outside a bounded window" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 100, createdAt = Instant.parse("2026-08-20T12:00:00Z")),
            )

            // The Day window of 2026-08-15 excludes the entry dated after it.
            viewModel.setPeriod(LedgerPeriod.Day)
            viewModel.upcomingCount.value shouldBe 0
        }
    }

    "a wholly future period shows its entries without the show-future toggle" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 9, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Scheduled", main, 200, createdAt = Instant.parse("2026-10-05T12:00:00Z")),
            )

            // The window after today's month is entirely in the future: its scheduled
            // entries are the point of the window, so none of them are hidden.
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.goToNextPeriod()

            viewModel.entries.value.map { it.description } shouldBe listOf("Scheduled")
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -200L))
            viewModel.categoryTotals.value.first().total shouldBe -200L
        }
    }

    "a wholly past period keeps its entries visible regardless of the toggle" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 9, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Past", main, 100, createdAt = Instant.parse("2026-08-05T12:00:00Z")),
            )

            // Nothing inside a past window is dated after today, so hiding future
            // entries can never remove a row there: the toggle would be inert.
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.goToPreviousPeriod()

            viewModel.entries.value.map { it.description } shouldBe listOf("Past")
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -100L))

            viewModel.setShowFuture(true)
            viewModel.entries.value.map { it.description } shouldBe listOf("Past")
        }
    }

    "year period shows only entries in the year" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 6, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Old", main, 100, createdAt = Instant.parse("2025-12-31T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Current", main, 200, createdAt = Instant.parse("2026-01-01T12:00:00Z")),
            )

            viewModel.setPeriod(LedgerPeriod.Year)

            viewModel.entries.value.map { it.description } shouldBe listOf("Current")
        }
    }

    "period with no entries shows an empty list" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Inside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")),
            )

            viewModel.setPeriod(LedgerPeriod.Year)
            viewModel.goToNextPeriod()

            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "total is the net of income and expenses" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            viewModel.totalMinor.value shouldBe 1_550L
        }
    }

    "total converts foreign currency entries via stored rates" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (_, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.totalMinor.value shouldBe -100L
        }
    }

    "total is null when a foreign rate is missing" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (_, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.totalMinor.value shouldBe null
        }
    }

    "total follows search and type filters" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            viewModel.totalMinor.value shouldBe -1_650L

            viewModel.setSearchQuery("coffee")
            viewModel.totalMinor.value shouldBe -450L

            viewModel.setSearchQuery("")
            viewModel.setTypeFilter(EntryType.Income)
            viewModel.totalMinor.value shouldBe 0L
        }
    }

    "total follows the default currency" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, settingsDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.totalMinor.value shouldBe -550L

            settingsDao.setDefaultCurrency(Currency.USD)

            viewModel.totalMinor.value shouldBe null
        }
    }

    "openEditDialog prefills the draft from the entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()

            viewModel.openEditDialog(created)

            viewModel.dialogState.value.let { draft ->
                draft.shouldNotBeNull()
                draft.editing shouldBe created
                draft.amountText shouldBe "4.50"
                draft.description shouldBe "Coffee"
            }
        }
    }

    "saveDialog updates the entry and closes the dialog" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            viewModel.openEditDialog(created)
            viewModel.onAmountChange("5.00")

            viewModel.saveDialog() shouldBe true

            viewModel.dialogState.value.shouldBeNull()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first().amount shouldBe 500
        }
    }

    "saveDialog keeps a transfer's category" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("General", CategoryType.Both)
            val accounts = accountDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val savings = accounts.first { it.name == "Savings" }
            val general = categoryDao.getAll().first().first()

            viewModel.createEntry(TransferEntry(0, general, "Move", main, 500, toAccount = savings, toAmount = null))
            val created = entryDao.getAll().first().filterIsInstance<TransferEntry>().first()
            created.category shouldBe general

            viewModel.openEditDialog(created)
            viewModel.onDescriptionChange("Moved")
            viewModel.saveDialog() shouldBe true

            val saved = entryDao.getAll().first().filterIsInstance<TransferEntry>().first()
            saved.description shouldBe "Moved"
            saved.category shouldBe general
        }
    }

    "deleteDialogEntry deletes the entry and closes the dialog" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            viewModel.openEditDialog(created)

            viewModel.deleteDialogEntry()

            viewModel.dialogState.value.shouldBeNull()
            entryDao.getAll().first().filterIsInstance<ExpenseEntry>().shouldBeEmpty()
        }
    }

    "dismissDialog closes the dialog" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            viewModel.openEditDialog(created)

            viewModel.dismissDialog()

            viewModel.dialogState.value.shouldBeNull()
        }
    }

    "changeDialogType flips an edited expense to income, keeping a both-category and the id" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            categoryDao.create("General", CategoryType.Both)
            val main = accountDao.getAll().first().first()
            val general = categoryDao.getAll().first().first()
            viewModel.createEntry(ExpenseEntry(0, general, "Coupon", main, 1_000))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()

            viewModel.openEditDialog(created)
            viewModel.changeDialogType(EntryType.Income)

            viewModel.dialogState.value.let { draft ->
                draft.shouldNotBeNull()
                draft.type shouldBe EntryType.Income
                draft.editing shouldBe created
                draft.categoryId shouldBe general.id
                draft.amountText shouldBe "10.00"
                draft.description shouldBe "Coupon"
            }

            viewModel.saveDialog() shouldBe true

            val rows = entryDao.getAll().first()
            rows.shouldHaveSize(1)
            val saved = rows.single()
            saved.id shouldBe created.id
            saved shouldBe IncomeEntry(created.id, general, "Coupon", main, 1_000)
        }
    }

    "changeDialogType clears a category the target type cannot use" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()

            viewModel.openEditDialog(created)
            viewModel.changeDialogType(EntryType.Income)

            viewModel.dialogState.value.let { draft ->
                draft.shouldNotBeNull()
                draft.type shouldBe EntryType.Income
                draft.categoryId.shouldBeNull()
                draft.description shouldBe "Coffee"
            }
        }
    }

    "changeDialogType is a no-op for transfers" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }
            viewModel.createEntry(TransferEntry(0, null, "Move", main, 500, toAccount = savings, toAmount = null))
            val created = entryDao.getAll().first().filterIsInstance<TransferEntry>().first()

            viewModel.openEditDialog(created)
            viewModel.changeDialogType(EntryType.Expense)

            viewModel.dialogState.value?.type shouldBe EntryType.Transfer
        }
    }

    "duplicateDialogEntry turns the edit into a new dated copy that saveDialog creates" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coupon", main, 1_000))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            val now = Instant.parse("2026-09-08T09:30:00Z")

            viewModel.openEditDialog(created)
            viewModel.duplicateDialogEntry(now = now, zone = TimeZone.UTC)

            viewModel.dialogState.value.let { draft ->
                draft.shouldNotBeNull()
                draft.editing.shouldBeNull()
                draft.type shouldBe EntryType.Expense
                draft.amountText shouldBe "10.00"
                draft.description shouldBe "Coupon"
                draft.categoryId shouldBe groceries.id
                draft.accountId shouldBe main.id
                draft.createdAt shouldBe now
            }

            viewModel.saveDialog() shouldBe true
            viewModel.dialogState.value.shouldBeNull()

            val rows = entryDao.getAll().first()
            rows.shouldHaveSize(2)
            val original = rows.first { it.id == created.id }
            original shouldBe created
            val copy = rows.first { it.id != created.id }
            copy shouldBe ExpenseEntry(copy.id, groceries, "Coupon", main, 1_000, createdAt = now)
        }
    }

    "view mode defaults to entries and toggles" {
        withLedgerViewModel(clock = FakeClock()) { viewModel ->
            viewModel.viewMode.value shouldBe LedgerViewMode.Entries

            viewModel.setViewMode(LedgerViewMode.Accounts)
            viewModel.viewMode.value shouldBe LedgerViewMode.Accounts

            viewModel.setViewMode(LedgerViewMode.Entries)
            viewModel.viewMode.value shouldBe LedgerViewMode.Entries

            viewModel.setViewMode(LedgerViewMode.Categories)
            viewModel.viewMode.value shouldBe LedgerViewMode.Categories

            viewModel.setViewMode(LedgerViewMode.Entries)
            viewModel.viewMode.value shouldBe LedgerViewMode.Entries
        }
    }

    "account balances follow the end of the selected period" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 9, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
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

            // Anchor is today (2026-09-15): the month window is Sep 2026, so the
            // balance includes every entry before and during September.
            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -600L))

            viewModel.goToPreviousPeriod()
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -300L))

            viewModel.goToPreviousPeriod()
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -100L))
        }
    }

    "account balances exclude future entries" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")),
            )

            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -100L))
        }
    }

    "account balances include the initial balance and react to direct writes" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 5_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Direct", main, 450))

            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, 4_550L))
        }
    }

    "account total converts foreign balances via stored rates" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Salary", CategoryType.Income)
            val main = accountDao.getAll().first().first { it.name == "Main" }
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            val salary = categoryDao.getAll().first().first()

            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 1_000))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", usd, 200))

            // 1'000 CHF + 200 USD / 2.0 = 1'100 CHF
            viewModel.accountTotalAtPeriodEnd.value shouldBe 1_100L
        }
    }

    "account total is null when a foreign rate is missing" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Salary", CategoryType.Income)
            val usd = accountDao.getAll().first().first()
            val salary = categoryDao.getAll().first().first()

            viewModel.createEntry(IncomeEntry(0, salary, "Pay", usd, 200))

            viewModel.accountTotalAtPeriodEnd.value shouldBe null
        }
    }

    "category totals show net per category" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 5_000))

            viewModel.categoryTotals.value shouldHaveSize (2)
            val cats = viewModel.categoryTotals.value.associateBy { it.category?.name }
            cats["Groceries"]!!.total shouldBe -1_650L
            cats["Groceries"]!!.count shouldBe 2
            cats["Salary"]!!.total shouldBe 5_000L
            cats["Salary"]!!.count shouldBe 1
        }
    }

    "category total is the sum of category totals" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.categoryTotal.value shouldBe 1_550L
        }
    }

    "category totals reconcile with the total chip and with income plus expenses" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 0.8, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }

            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", usd, 2))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", usd, 1))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 5_000))

            // Per sign: 2 USD -> 3 CHF and -1 USD -> -1 CHF, so Groceries nets 2 CHF.
            // A net-per-currency-group conversion would round (2 - 1) / 0.8 = 1.25 to 1
            // and make the category rows disagree with the total chip.
            viewModel.categoryTotal.value shouldBe 5_002L
            viewModel.categoryTotals.value.sumOf { it.total } shouldBe viewModel.categoryTotal.value
            val byName = viewModel.categoryTotals.value.associateBy { it.category?.name }
            byName["Groceries"]!!.total shouldBe 2L
            byName["Salary"]!!.total shouldBe 5_000L

            // The same reconciliation holds under the type filters: the "All" total of a
            // category equals its income total plus its expense total.
            viewModel.setTypeFilter(EntryType.Income)
            viewModel.categoryTotals.value.sumOf { it.total } shouldBe viewModel.categoryTotal.value
            viewModel.setTypeFilter(EntryType.Expense)
            viewModel.categoryTotals.value.sumOf { it.total } shouldBe viewModel.categoryTotal.value
        }
    }

    "category totals follow the period" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 9, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
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

            viewModel.setPeriod(LedgerPeriod.Month)
            viewModel.categoryTotals.value.shouldHaveSize(1)
            viewModel.categoryTotals.value.first().total shouldBe -300L

            viewModel.goToPreviousPeriod()
            viewModel.categoryTotals.value.first().total shouldBe -200L
        }
    }

    "category totals follow search filter" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.categoryTotals.value shouldHaveSize (2)

            viewModel.setSearchQuery("salary")
            viewModel.categoryTotals.value shouldHaveSize (1)
            viewModel.categoryTotals.value.first().category?.name shouldBe "Salary"
        }
    }

    "category totals follow type filter" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.setTypeFilter(EntryType.Income)
            viewModel.categoryTotals.value shouldHaveSize (1)
            viewModel.categoryTotals.value.first().category?.name shouldBe "Salary"

            viewModel.setTypeFilter(null)
            viewModel.categoryTotals.value shouldHaveSize (2)
        }
    }

    "category totals convert foreign currency entries via stored rates" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 2.0, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (_, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.categoryTotals.value shouldHaveSize (1)
            // 200 USD / 2.0 = 100 CHF
            viewModel.categoryTotals.value.first().total shouldBe -100L
        }
    }

    "category total is null when a foreign rate is missing" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (_, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.categoryTotal.value shouldBe null
        }
    }

    "openCategory filters the entries to the category and switches to the entries view" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 5_000))

            viewModel.setViewMode(LedgerViewMode.Categories)
            viewModel.openCategory(groceries.id)

            viewModel.viewMode.value shouldBe LedgerViewMode.Entries
            viewModel.categoryFilter.value shouldBe groceries.id
            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Lunch", "Coffee")
            viewModel.totalMinor.value shouldBe -1_650L
        }
    }

    "category filter excludes transfers from the drill-down" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(
                TransferEntry(0, groceries, "Move", main, 500, toAccount = savings, toAmount = null),
            )

            viewModel.openCategory(groceries.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Coffee")
            viewModel.totalMinor.value shouldBe -450L
        }
    }

    "openAccount filters the entries to the account and switches to the entries view" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Bonus", savings, 2_000))
            viewModel.createEntry(TransferEntry(0, groceries, "Move", main, 500, toAccount = savings, toAmount = null))

            viewModel.setViewMode(LedgerViewMode.Accounts)
            viewModel.openAccount(savings.id)

            viewModel.viewMode.value shouldBe LedgerViewMode.Entries
            viewModel.accountFilter.value shouldBe savings.id
            // The income on Savings and the transfer into it stay; Main's expense goes.
            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Move", "Bonus")
            viewModel.totalMinor.value shouldBe 2_500L
        }
    }

    "category filter follows the type filter" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.setTypeFilter(EntryType.Income)
            viewModel.openCategory(salary.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Pay")
        }
    }

    "clearCategoryFilter restores the full entries list" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.openCategory(groceries.id)
            viewModel.displayedEntries.value.shouldHaveSize(1)

            viewModel.clearCategoryFilter()

            viewModel.categoryFilter.value.shouldBeNull()
            viewModel.viewMode.value shouldBe LedgerViewMode.Entries
            viewModel.displayedEntries.value.shouldHaveSize(2)
            viewModel.totalMinor.value shouldBe 1_550L
        }
    }

    "switching the view mode away from entries clears the entry filters" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            viewModel.openCategory(groceries.id)
            viewModel.setAccountFilter(main.id)
            viewModel.categoryFilter.value shouldBe groceries.id
            viewModel.accountFilter.value shouldBe main.id

            viewModel.setViewMode(LedgerViewMode.Categories)

            viewModel.categoryFilter.value.shouldBeNull()
            viewModel.accountFilter.value.shouldBeNull()
        }
    }

    "setCategoryFilter narrows the entries without switching the view mode" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.setCategoryFilter(salary.id)

            viewModel.viewMode.value shouldBe LedgerViewMode.Entries
            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Pay")
        }
    }

    "account filter narrows the entries to the account" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.setAccountFilter(usd.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Foreign")

            viewModel.clearAccountFilter()
            viewModel.displayedEntries.value.shouldHaveSize(2)
        }
    }

    "account filter keeps transfers of the source account" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(
                TransferEntry(0, groceries, "Move", main, 500, toAccount = savings, toAmount = null),
            )

            viewModel.setAccountFilter(main.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Move", "Coffee")
        }
    }

    "account filter counts an outgoing transfer as negative" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(
                TransferEntry(0, groceries, "Move", main, 500, toAccount = savings, toAmount = null),
            )

            viewModel.setAccountFilter(main.id)

            viewModel.totalMinor.value shouldBe -950L
        }
    }

    "account filter keeps transfers into the account and counts them positive" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            accountDao.create("Savings", Currency.CHF)
            val savings = accountDao.getAll().first().first { it.name == "Savings" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(
                TransferEntry(0, groceries, "Move", savings, 500, toAccount = main, toAmount = null),
            )

            viewModel.setAccountFilter(main.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Move", "Coffee")
            viewModel.totalMinor.value shouldBe 50L
        }
    }

    "account filter totals an incoming foreign transfer at the received amount" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
            rates = listOf(FxRate(Currency.CHF, Currency.USD, 0.8, "2026-08-13")),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }

            viewModel.createEntry(
                TransferEntry(0, null, "Convert", usd, 800, toAccount = main, toAmount = 1_000),
            )

            viewModel.setAccountFilter(main.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Convert")
            viewModel.totalMinor.value shouldBe 1_000L
        }
    }

    "account filter without a rate for a foreign account yields a null total" {
        withLedgerViewModel(
            clock = FakeClock(),
            defaultCurrency = Currency.CHF,
        ) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, _) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }

            viewModel.createEntry(
                TransferEntry(0, null, "Convert", main, 100, toAccount = usd, toAmount = 80),
            )

            viewModel.setAccountFilter(usd.id)

            viewModel.totalMinor.value shouldBe null
        }
    }

    "category and account filters combine" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.setCategoryFilter(groceries.id)
            viewModel.setAccountFilter(main.id)

            viewModel.displayedEntries.value.map { it.description } shouldBe listOf("Coffee")
        }
    }

    "category totals ignore the category filter" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            categoryDao.create("Salary", CategoryType.Income)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, salary, "Pay", main, 2_000))

            viewModel.openCategory(groceries.id)
            viewModel.displayedEntries.value.shouldHaveSize(1)

            viewModel.categoryTotals.value.shouldHaveSize(2)
            viewModel.categoryTotal.value shouldBe 1_550L
        }
    }

    "adjustBalance to a higher target creates an income entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()

            viewModel.adjustBalance(main, targetBalance = 12_500, category = groceries)

            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(1)
            val entry = entries.first()
            entry shouldBe IncomeEntry(
                id = entry.id,
                category = groceries,
                description = null,
                account = entry.account,
                amount = 2_500,
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )
        }
    }

    "adjustBalance to a lower target creates an expense entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()

            viewModel.adjustBalance(main, targetBalance = 9_000, category = groceries)

            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(1)
            val entry = entries.first()
            entry shouldBe ExpenseEntry(
                id = entry.id,
                category = groceries,
                description = null,
                account = entry.account,
                amount = 1_000,
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )
        }
    }

    "usedCategories is empty when the account has no entries" {
        withLedgerViewModel(clock = FakeClock()) { accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            val main = accountDao.getAll().first().first()

            viewModel.usedCategories(main.id).shouldBeEmpty()
        }
    }

    "usedCategories returns the account's most-used categories" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            categoryDao.create("Salary", CategoryType.Income)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }

            entryDao.create(ExpenseEntry(0, groceries, "a", main, 100, at(1_700_000_000_000), TimeZone.UTC))
            entryDao.create(ExpenseEntry(0, groceries, "b", main, 200, at(1_700_000_000_000), TimeZone.UTC))
            entryDao.create(IncomeEntry(0, salary, "c", main, 300, at(1_700_000_000_000), TimeZone.UTC))

            val categories = viewModel.usedCategories(main.id)

            categories.map { it.id } shouldBe listOf(groceries.id, salary.id)
        }
    }

    "adjustBalance with an unchanged balance creates no entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()

            viewModel.adjustBalance(main, targetBalance = 10_000, category = groceries)

            entryDao.getAll().first().shouldBeEmpty()
        }
    }

    "hidden accounts are excluded from balances, totals and adjust maps, but their entries stay in the ledger" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 5_000)
            accountDao.create("Old", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val old = accounts.first { it.name == "Old" }
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Visible", main, 450, at(1_700_000_000_000), TimeZone.UTC))
            entryDao.create(ExpenseEntry(0, groceries, "Archived", old, 900, at(1_700_000_000_000), TimeZone.UTC))
            accountDao.setHidden(old.id, true)

            // The Accounts tab shows only visible accounts...
            viewModel.visibleAccounts.value.map { it.name } shouldBe listOf("Main")
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, 4_550L))
            // ...so the period-end total excludes the hidden balance too.
            viewModel.accountTotalAtPeriodEnd.value shouldBe 4_550L
            // Current balances (Adjust Balance) have no key for the hidden account.
            viewModel.currentAccountBalances.value shouldBe mapOf(main.id to 4_550L)

            // History is kept: the hidden account's entries still appear in the
            // entries list and still count toward the category totals.
            viewModel.entries.value.map { it.description } shouldBe listOf("Archived", "Visible")
            viewModel.categoryTotals.value.single().let { total ->
                total.category shouldBe groceries
                total.total shouldBe -1_350L
            }
        }
    }

    "entries of hidden accounts stay editable from the ledger" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            accountDao.setHidden(main.id, true)

            // The entry is still listed (history is kept), so it can be opened,
            // edited and saved without switching accounts.
            entryDao.getAll().first().single().let { entry ->
                viewModel.openEditDialog(entry)
            }
            viewModel.onDescriptionChange("Edited coffee")
            viewModel.saveDialog() shouldBe true

            entryDao.getAll().first().single().description shouldBe "Edited coffee"
        }
    }

    "currentAccountBalances excludes future-dated entries" {
        withLedgerViewModel(
            clock = FakeClock(today = LocalDate(2026, 8, 15)),
        ) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Savings", Currency.CHF)
            categoryDao.create("Groceries", CategoryType.Expense)
            val accounts = accountDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val savings = accounts.first { it.name == "Savings" }
            val groceries = categoryDao.getAll().first().first()

            // A past expense (2026-08-01) and a future transfer out of Main (2026-09-01).
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, at(1_784_000_000_000), TimeZone.UTC))
            entryDao.create(
                TransferEntry(
                    0, null, null, main, 10_000, toAccount = savings, toAmount = null,
                    createdAt = at(1_787_000_000_000), createdZone = TimeZone.UTC,
                ),
            )

            viewModel.currentAccountBalances.value shouldBe mapOf(
                main.id to 9_550L,
                savings.id to 0L,
            )
        }
    }

    "adjusting again creates a separate entry" {
        withLedgerViewModel(clock = FakeClock()) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()
            val now = at(1_700_000_000_000)

            viewModel.adjustBalance(main, targetBalance = 12_500, category = groceries, now = now)
            viewModel.adjustBalance(main, targetBalance = 11_000, category = groceries, now = now)

            val entries = entryDao.getAll().first()
            entries.shouldHaveSize(2)
            entries.map { it.amount } shouldBe listOf(1_500L, 2_500L)
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

private fun at(millis: Long) = Instant.fromEpochMilliseconds(millis)
