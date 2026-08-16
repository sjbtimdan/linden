package org.sjbtimdan.linden.ui.history

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.LocalDate
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.accounts.AccountWithBalance
import org.sjbtimdan.linden.ui.withHistoryViewModel

@OptIn(ExperimentalTestApi::class)
class HistoryViewModelTest : StringSpec({
    "entries start empty" {
        withHistoryViewModel { viewModel ->
            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "search filters by description and account name" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
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

    "type filter keeps only matching entries" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            val updated = created.copy(description = "Tea", amount = 300)

            viewModel.updateEntry(updated)

            viewModel.entries.value.first() shouldBe updated
        }
    }

    "delete removes an entry" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            entryDao.create(ExpenseEntry(0, groceries, "Direct", main, 100))

            viewModel.entries.value.shouldHaveSize(1)
        }
    }

    "month period shows only entries in the window and navigates" {
        withHistoryViewModel(today = { LocalDate(2026, 9, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Before", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Inside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "After", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Month)
            viewModel.entries.value.map { it.description } shouldBe listOf("After")

            viewModel.goToNextPeriod()
            viewModel.entries.value.shouldBeEmpty()

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("After")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Inside")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Before")

            viewModel.setPeriod(HistoryPeriod.All)
            viewModel.entries.value.shouldHaveSize(3)
        }
    }

    "week period shows only entries in the calendar week" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 16) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "SunBefore", main, 100, createdAt = Instant.parse("2026-08-09T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "MonInside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "SunInside", main, 300, createdAt = Instant.parse("2026-08-16T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "MonAfter", main, 400, createdAt = Instant.parse("2026-08-17T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Week)

            viewModel.entries.value.map { it.description } shouldBe listOf("SunInside", "MonInside")
        }
    }

    "day period shows only entries on the anchor day and navigates" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "SameDay", main, 200, createdAt = Instant.parse("2026-08-15T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Yesterday", main, 100, createdAt = Instant.parse("2026-08-14T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Day)
            viewModel.entries.value.map { it.description } shouldBe listOf("SameDay")

            viewModel.goToPreviousPeriod()
            viewModel.entries.value.map { it.description } shouldBe listOf("Yesterday")

            viewModel.setPeriod(HistoryPeriod.All)
            viewModel.entries.value.shouldHaveSize(2)
        }
    }

    "future entries are excluded from the history" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")))

            viewModel.entries.value.map { it.description } shouldBe listOf("Today")
        }
    }

    "year period shows only entries in the year" {
        withHistoryViewModel(today = { LocalDate(2026, 6, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Old", main, 100, createdAt = Instant.parse("2025-12-31T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Current", main, 200, createdAt = Instant.parse("2026-01-01T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Year)

            viewModel.entries.value.map { it.description } shouldBe listOf("Current")
        }
    }

    "period with no entries shows an empty list" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Inside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Year)
            viewModel.goToNextPeriod()

            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "total is the net of income and expenses" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(IncomeEntry(0, groceries, "Refund", main, 2_000))

            viewModel.totalMinor.value shouldBe 1_550L
        }
    }

    "total converts foreign currency entries via stored rates" {
        withHistoryViewModel(
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (_, groceries) = seed(accountDao, categoryDao)
            accountDao.create("USD", Currency.USD)
            val usd = accountDao.getAll().first().first { it.name == "USD" }
            viewModel.createEntry(ExpenseEntry(0, groceries, "Foreign", usd, 200))

            viewModel.totalMinor.value shouldBe null
        }
    }

    "total follows search and type filters" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel(
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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

    "deleteDialogEntry deletes the entry and closes the dialog" {
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getAll().first().filterIsInstance<ExpenseEntry>().first()
            viewModel.openEditDialog(created)

            viewModel.dismissDialog()

            viewModel.dialogState.value.shouldBeNull()
        }
    }

    "view mode defaults to entries and toggles" {
        withHistoryViewModel { viewModel ->
            viewModel.viewMode.value shouldBe HistoryViewMode.Entries

            viewModel.setViewMode(HistoryViewMode.Accounts)
            viewModel.viewMode.value shouldBe HistoryViewMode.Accounts

            viewModel.setViewMode(HistoryViewMode.Entries)
            viewModel.viewMode.value shouldBe HistoryViewMode.Entries
        }
    }

    "account balances follow the end of the selected period" {
        withHistoryViewModel(today = { LocalDate(2026, 9, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Jul", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Aug", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Sep", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")))

            // Anchor is today (2026-09-15): the month window is Sep 2026, so the
            // balance includes every entry before and during September.
            viewModel.setPeriod(HistoryPeriod.Month)
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -600L))

            viewModel.goToPreviousPeriod()
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -300L))

            viewModel.goToPreviousPeriod()
            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -100L))
        }
    }

    "account balances exclude future entries" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Today", main, 100, createdAt = Instant.parse("2026-08-15T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Future", main, 200, createdAt = Instant.parse("2026-08-16T12:00:00Z")))

            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, -100L))
        }
    }

    "account balances include the initial balance and react to direct writes" {
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 5_000)
            categoryDao.create("Groceries", CategoryType.Expense)
            val main = accountDao.getAll().first().first()
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Direct", main, 450))

            viewModel.accountBalancesAtPeriodEnd.value shouldBe listOf(AccountWithBalance(main, 4_550L))
        }
    }

    "account total converts foreign balances via stored rates" {
        withHistoryViewModel(
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
        withHistoryViewModel { entryDao, accountDao, categoryDao, viewModel ->
            accountDao.create("USD", Currency.USD)
            categoryDao.create("Salary", CategoryType.Income)
            val usd = accountDao.getAll().first().first()
            val salary = categoryDao.getAll().first().first()

            viewModel.createEntry(IncomeEntry(0, salary, "Pay", usd, 200))

            viewModel.accountTotalAtPeriodEnd.value shouldBe null
        }
    }
})

private suspend fun seed(
    accountDao: AccountDao,
    categoryDao: CategoryDao,
): Pair<Account, Category> {
    accountDao.create("Main", Currency.CHF)
    categoryDao.create("Groceries", CategoryType.Expense)
    val main = accountDao.getAll().first().first()
    val groceries = categoryDao.getAll().first().first()
    return main to groceries
}

private fun at(millis: Long) = Instant.fromEpochMilliseconds(millis)
