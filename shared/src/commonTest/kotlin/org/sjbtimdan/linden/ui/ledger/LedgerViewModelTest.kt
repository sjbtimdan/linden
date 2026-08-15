package org.sjbtimdan.linden.ui.ledger

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withLedgerViewModel

@OptIn(ExperimentalTestApi::class)
class LedgerViewModelTest : StringSpec({
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
