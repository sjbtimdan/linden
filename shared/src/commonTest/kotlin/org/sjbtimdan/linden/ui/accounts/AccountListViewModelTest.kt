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
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.ui.withAccountViewModel
import kotlin.math.roundToLong

@OptIn(ExperimentalTestApi::class)
class AccountListViewModelTest : StringSpec({
    "creating an account adds it to the list" {
        withAccountViewModel { viewModel ->
            viewModel.accounts.value.shouldBeEmpty()

            viewModel.createAccount("Main", Currency.CHF)

            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().account.name shouldBe "Main"
            viewModel.accounts.value.first().account.currency shouldBe Currency.CHF
        }
    }

    "creating an account stores its initial balance" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 25_000)

            viewModel.accounts.value.single().account.initialBalance shouldBe 25_000
        }
    }

    "updating an account reflects in the list" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val created = viewModel.accounts.value.first()

            viewModel.updateAccount(created.account.copy(name = "Savings", currency = Currency.USD))
            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().account.name shouldBe "Savings"
            viewModel.accounts.value.first().account.currency shouldBe Currency.USD
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

            viewModel.accounts.value.single().account.currency shouldBe Currency.CHF
        }
    }

    "updateAccount allows a currency change when the account has no entries" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val created = viewModel.accounts.value.first().account

            viewModel.updateAccount(created.copy(currency = Currency.EUR))

            viewModel.accounts.value.single().account.currency shouldBe Currency.EUR
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

            viewModel.accounts.value.single().account.name shouldBe "Main Account"
            viewModel.accounts.value.single().account.currency shouldBe Currency.CHF
        }
    }

    "direct database writes reflect in the list" {
        withAccountViewModel { dao, viewModel ->
            viewModel.accounts.value.shouldBeEmpty()

            dao.create("Wallet", Currency.EUR)

            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().account.name shouldBe "Wallet"
        }
    }

    "balance equals the initial balance when there are no entries" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF, initialBalance = 25_000)

            viewModel.accounts.value.single().balance shouldBe 25_000
        }
    }

    "balance reflects income and expense entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Salary", CategoryType.Income)
            categoryDao.create("Groceries", CategoryType.Expense)
            val salary = categoryDao.getAll().first().first { it.name == "Salary" }
            val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }

            entryDao.create(
                IncomeEntry(id = 0, category = salary, description = "Pay", account = main, amount = 50_000),
            )
            entryDao.create(
                ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450),
            )

            viewModel.accounts.value.single().balance shouldBe 10_000 + 50_000 - 450
        }
    }

    "balance is computed per account" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Savings", Currency.CHF)
            val accounts = accountDao.getAll().first()
            val main = accounts.first { it.name == "Main" }
            val savings = accounts.first { it.name == "Savings" }
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(
                ExpenseEntry(id = 0, category = groceries, description = "Coffee", account = main, amount = 450),
            )
            entryDao.create(
                ExpenseEntry(id = 0, category = groceries, description = "Lunch", account = savings, amount = 1_200),
            )

            viewModel.accounts.value.single { it.account.name == "Main" }.balance shouldBe 9_550
            viewModel.accounts.value.single { it.account.name == "Savings" }.balance shouldBe -1_200
        }
    }

    "total sums balances in the default currency" {
        withAccountViewModel { accountDao, _, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Savings", Currency.CHF, initialBalance = 5_000)

            viewModel.totalMinor.value shouldBe 15_000
        }
    }

    "total converts foreign balances to the default currency" {
        val rate = FxRate(baseCurrency = Currency.CHF, quoteCurrency = Currency.EUR, rate = 1.1, date = "2026-08-16")
        withAccountViewModel(defaultCurrency = Currency.CHF, rates = listOf(rate)) { accountDao, _, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Euros", Currency.EUR, initialBalance = 5_000)

            viewModel.totalMinor.value shouldBe 10_000 + (5_000.0 / 1.1).roundToLong()
        }
    }

    "total is null when a foreign balance has no rate" {
        withAccountViewModel { accountDao, _, _, viewModel ->
            accountDao.create("Euros", Currency.EUR, initialBalance = 5_000)

            viewModel.totalMinor.value shouldBe null
        }
    }

    "total uses the stored default currency" {
        val rate = FxRate(baseCurrency = Currency.EUR, quoteCurrency = Currency.CHF, rate = 0.9, date = "2026-08-16")
        withAccountViewModel(defaultCurrency = Currency.EUR, rates = listOf(rate)) { accountDao, _, _, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            accountDao.create("Euros", Currency.EUR, initialBalance = 5_000)

            viewModel.totalMinor.value shouldBe 5_000 + (10_000.0 / 0.9).roundToLong()
        }
    }
})
