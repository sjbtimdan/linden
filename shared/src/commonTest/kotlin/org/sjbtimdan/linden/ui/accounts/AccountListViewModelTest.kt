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
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.ui.withAccountViewModel
import kotlin.time.Instant

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

    "createAccount refuses a duplicate name" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)

            val result = viewModel.createAccount("Main", Currency.USD)

            result shouldBe false
            viewModel.accounts.value.shouldHaveSize(1)
        }
    }

    "createAccount refuses a case-variant duplicate name" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)

            val result = viewModel.createAccount("MAIN", Currency.USD)

            result shouldBe false
            viewModel.accounts.value.shouldHaveSize(1)
        }
    }

    "createAccount trims the name before storing" {
        withAccountViewModel { viewModel ->
            val result = viewModel.createAccount("  Main  ", Currency.CHF)

            result shouldBe true
            viewModel.accounts.value.single().name shouldBe "Main"
        }
    }

    "updateAccount refuses renaming to a taken name" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            viewModel.createAccount("Savings", Currency.USD)
            val main = viewModel.accounts.value.first { it.name == "Main" }

            val result = viewModel.updateAccount(main.copy(name = "Savings"))

            result shouldBe false
            viewModel.accounts.value.map { it.name } shouldBe listOf("Main", "Savings")
        }
    }

    "updateAccount allows keeping its own name" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val main = viewModel.accounts.value.first()

            val result = viewModel.updateAccount(main.copy(name = "Main"))

            result shouldBe true
            viewModel.accounts.value.single().name shouldBe "Main"
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
        withAccountViewModel { dao, _, _, viewModel ->
            viewModel.accounts.value.shouldBeEmpty()

            dao.create("Wallet", Currency.EUR)

            viewModel.accounts.value.shouldHaveSize(1)
            viewModel.accounts.value.first().name shouldBe "Wallet"
        }
    }

    "search filters accounts by name, case-insensitively" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            viewModel.createAccount("Savings", Currency.USD)

            viewModel.setSearchQuery("MAIN")

            viewModel.accounts.value.map { it.name } shouldBe listOf("Main")
        }
    }

    "search matches substrings and clearing it restores all accounts" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            viewModel.createAccount("Savings", Currency.USD)

            viewModel.setSearchQuery("ving")
            viewModel.accounts.value.map { it.name } shouldBe listOf("Savings")

            viewModel.setSearchQuery("")
            viewModel.accounts.value.map { it.name } shouldBe listOf("Main", "Savings")
        }
    }

    "search with no matches yields an empty list" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)

            viewModel.setSearchQuery("nonexistent")

            viewModel.accounts.value.shouldBeEmpty()
        }
    }

    "deleteAccount removes an account with no entries" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            viewModel.createAccount("Savings", Currency.USD)
            val main = viewModel.accounts.value.first { it.name == "Main" }

            viewModel.deleteAccount(main.id)

            viewModel.accounts.value.map { it.name } shouldBe listOf("Savings")
        }
    }

    "deleteAccount ignores an account that still has entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()
            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            viewModel.accountsWithEntries.first { main.id in it }

            viewModel.deleteAccount(main.id)

            viewModel.accounts.value.map { it.name } shouldBe listOf("Main")
        }
    }

    "deleteAccount ignores an account that is a transfer target" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Source", Currency.CHF)
            accountDao.create("Target", Currency.CHF)
            val source = accountDao.getAll().first().first { it.name == "Source" }
            val target = accountDao.getAll().first().first { it.name == "Target" }
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()
            entryDao.create(
                TransferEntry(
                    id = 0,
                    category = groceries,
                    description = "Move",
                    account = source,
                    amount = 450,
                    toAccount = target,
                    toAmount = null,
                ),
            )
            viewModel.accountsWithEntries.first { target.id in it }

            viewModel.deleteAccount(target.id)

            viewModel.accounts.value.map { it.name } shouldBe listOf("Source", "Target")
        }
    }

    "setHidden flips the flag through the database" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Main", Currency.CHF)
            val main = viewModel.accounts.value.single()

            viewModel.setHidden(main.id, true)
            viewModel.accounts.value.single().hidden shouldBe true

            viewModel.setHidden(main.id, false)
            viewModel.accounts.value.single().hidden shouldBe false
        }
    }

    "allTimeBalances adds the initial balance and counts future-dated entries" {
        withAccountViewModel { accountDao, entryDao, categoryDao, viewModel ->
            accountDao.create("Main", Currency.CHF, initialBalance = 10_000)
            val main = accountDao.getAll().first().first()
            categoryDao.create("Groceries", CategoryType.Expense)
            val groceries = categoryDao.getAll().first().first()

            entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
            // Dated in 2030: the hide warning must count it even though today's
            // balance does not.
            entryDao.create(
                ExpenseEntry(
                    0,
                    groceries,
                    "Future",
                    main,
                    500,
                    createdAt = Instant.parse("2030-01-01T00:00:00Z"),
                ),
            )

            viewModel.allTimeBalances.value shouldBe mapOf(main.id to 9_050L)
        }
    }

    "allTimeBalances includes accounts without entries at their initial balance" {
        withAccountViewModel { viewModel ->
            viewModel.createAccount("Empty", Currency.CHF, initialBalance = 3_000)

            viewModel.allTimeBalances.value shouldBe mapOf(viewModel.accounts.value.single().id to 3_000L)
        }
    }
})
