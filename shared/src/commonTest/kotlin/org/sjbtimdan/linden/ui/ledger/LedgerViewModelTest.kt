package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
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
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerViewModelTest : StringSpec({
    "entries start empty" {
        withLedgerViewModel { viewModel ->
            viewModel.entries.value.shouldBeEmpty()
        }
    }

    "creating an entry adds it to the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Coffee", main, 450)
            )

            viewModel.entries.value.shouldHaveSize(1)
            val entry = viewModel.entries.value.first()
            entry shouldBe ExpenseEntry(entry.id, groceries, "Coffee", main, 450)
        }
    }

    "type filter keeps only matching entries" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
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

    "search filters by description and account name" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
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

    "sort orders apply correctly" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Small", main, 100, createdAt = at(1_000)),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Large", main, 900, createdAt = at(2_000)),
            )
            viewModel.createEntry(
                ExpenseEntry(0, groceries, "Medium", main, 500, createdAt = at(3_000)),
            )

            // Newest first (default): Medium, Large, Small by createdAt
            viewModel.entries.value.map { it.amount } shouldBe listOf(500L, 900L, 100L)

            viewModel.setSortOrder(SortOrder.OldestFirst)
            viewModel.entries.value.map { it.amount } shouldBe listOf(100L, 900L, 500L)

            viewModel.setSortOrder(SortOrder.AmountHighToLow)
            viewModel.entries.value.map { it.amount } shouldBe listOf(900L, 500L, 100L)

            viewModel.setSortOrder(SortOrder.AmountLowToHigh)
            viewModel.entries.value.map { it.amount } shouldBe listOf(100L, 500L, 900L)
        }
    }

    "update reflects in the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            val created = entryDao.getExpenses().first().first()
            val updated = created.copy(description = "Tea", amount = 300)

            viewModel.updateEntry(updated)

            viewModel.entries.value.first() shouldBe updated
        }
    }

    "delete removes an entry" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Tea", main, 300))
            val created = viewModel.entries.value.first()

            viewModel.deleteEntry(created.id)

            viewModel.entries.value.shouldHaveSize(1)
            viewModel.entries.value.first().description shouldBe "Coffee"
        }
    }

    "new entry state prefills from the most recent entry of the same type" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.createEntry(ExpenseEntry(0, groceries, "Lunch", main, 1_200))

            val state = viewModel.newEntryState(EntryType.Expense)

            state.editing shouldBe null
            state.type shouldBe EntryType.Expense
            state.amountText shouldBe ""
            state.categoryId shouldBe groceries.id
            state.accountId shouldBe main.id
            state.description shouldBe "Lunch"
        }
    }

    "new entry state for a type with no entries is empty" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)
            viewModel.createEntry(ExpenseEntry(0, groceries, "Coffee", main, 450))

            val state = viewModel.newEntryState(EntryType.Income)

            state.editing shouldBe null
            state.amountText shouldBe ""
            state.categoryId shouldBe null
            state.accountId shouldBe null
            state.toAccountId shouldBe null
            state.description shouldBe ""
        }
    }

    "new transfer state prefills accounts from the most recent transfer" {
        withLedgerViewModel { entryDao, accountDao, _, viewModel ->
            accountDao.create("Main", Currency.CHF)
            accountDao.create("Savings", Currency.EUR)
            val accounts = accountDao.getAll().first()
            val main = accounts.first()
            val savings = accounts.last()

            viewModel.createEntry(TransferEntry(0, null, "Move money", main, 10_000, toAccount = savings, toAmount = 9_500))

            val state = viewModel.newEntryState(EntryType.Transfer)

            state.type shouldBe EntryType.Transfer
            state.amountText shouldBe ""
            state.toAmountText shouldBe ""
            state.accountId shouldBe main.id
            state.toAccountId shouldBe savings.id
            state.description shouldBe "Move money"
        }
    }

    "direct database writes reflect in the list" {
        withLedgerViewModel { entryDao, accountDao, categoryDao, viewModel ->
            val (main, groceries) = seed(accountDao, categoryDao)

            entryDao.create(ExpenseEntry(0, groceries, "Direct", main, 100))

            viewModel.entries.value.shouldHaveSize(1)
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
