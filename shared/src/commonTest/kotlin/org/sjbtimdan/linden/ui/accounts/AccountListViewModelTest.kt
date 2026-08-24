package org.sjbtimdan.linden.ui.accounts

import androidx.compose.ui.test.ExperimentalTestApi
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.ui.withAccountViewModel

@OptIn(ExperimentalTestApi::class)
class AccountListViewModelTest : StringSpec({
    "creating an account adds it to the list" {
        withAccountViewModel { viewModel ->
            viewModel.accounts.value.shouldBeEmpty()

            viewModel.createAccount("Main", Currency.CHF)

            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().name shouldBe "Main"
            viewModel.accounts.value.first().currency shouldBe Currency.CHF
        }
    }

    "creating an account stores its initial balance" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 25_000)

            viewModel.accounts.value.single().initialBalance shouldBe 25_000
        }
    }

    "updating an account reflects in the list" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val created = viewModel.accounts.value.first()

            viewModel.updateAccount(created.copy(name = "Savings", currency = Currency.USD))
            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().name shouldBe "Savings"
            viewModel.accounts.value.first().currency shouldBe Currency.USD
        }
    }

    "updateAccount ignores a currency change when the account has entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.accountsWithEntries.first { main.id in it }

            viewModel.updateAccount(main.copy(currency = Currency.EUR))

            viewModel.accounts.value.single().currency shouldBe Currency.CHF
        }
    }

    "updateAccount allows a currency change when the account has no entries" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val created = viewModel.accounts.value.first()

            viewModel.updateAccount(created.copy(currency = Currency.EUR))

            viewModel.accounts.value.single().currency shouldBe Currency.EUR
        }
    }

    "updateAccount allows renaming an account that has entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.accountsWithEntries.first { main.id in it }

            viewModel.updateAccount(main.copy(name = "Main Account"))

            viewModel.accounts.value.single().name shouldBe "Main Account"
            viewModel.accounts.value.single().currency shouldBe Currency.CHF
        }
    }

    "direct database writes reflect in the list" {
        withAccountViewModel { dao, viewModel ->
            viewModel.accounts.value.shouldBeEmpty()

            dao.create("Wallet", Currency.EUR)

            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().name shouldBe "Wallet"
        }
    }
})
