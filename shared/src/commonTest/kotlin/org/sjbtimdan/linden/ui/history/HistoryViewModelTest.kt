package org.sjbtimdan.linden.ui.history

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
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
import org.sjbtimdan.linden.model.IncomeEntry
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
            val created = entryDao.getExpenses().first().first()
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
        withHistoryViewModel(today = { LocalDate(2026, 8, 15) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Before", main, 100, createdAt = Instant.parse("2026-07-31T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Inside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "After", main, 300, createdAt = Instant.parse("2026-09-01T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Month)
            viewModel.entries.value.map { it.description } shouldBe listOf("Inside")

            viewModel.goToNextPeriod()
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
        withHistoryViewModel(today = { LocalDate(2026, 8, 13) }) { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "SunBefore", main, 100, createdAt = Instant.parse("2026-08-09T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "MonInside", main, 200, createdAt = Instant.parse("2026-08-10T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "SunInside", main, 300, createdAt = Instant.parse("2026-08-16T12:00:00Z")))
            viewModel.createEntry(ExpenseEntry(0, groceries, "MonAfter", main, 400, createdAt = Instant.parse("2026-08-17T12:00:00Z")))

            viewModel.setPeriod(HistoryPeriod.Week)

            viewModel.entries.value.map { it.description } shouldBe listOf("SunInside", "MonInside")
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
