package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

class EntryDaoTest : StringSpec({
    "income, expense, and transfer entries round-trip" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.EUR)
        categoryDao.create("Groceries", CategoryType.Expense)
        categoryDao.create("Salary", CategoryType.Income)

        val accounts = accountDao.getAll().first()
        val main = accounts.first()
        val savings = accounts.last()
        val categories = categoryDao.getAll().first()
        val groceries = categories.first()
        val salary = categories.last()

        val expense = ExpenseEntry(
            id = 0,
            category = groceries,
            description = "Coffee",
            account = main,
            amount = 450,
            currency = Currency.CHF,
        )
        val income = IncomeEntry(
            id = 0,
            category = salary,
            description = "Salary",
            account = main,
            amount = 50_000,
            currency = Currency.CHF,
        )
        val transfer = TransferEntry(
            id = 0,
            category = null,
            description = null,
            account = main,
            amount = 10_000,
            currency = Currency.CHF,
            toAccount = savings,
            toAmount = 9_500,
            toCurrency = Currency.EUR,
        )

        entryDao.create(expense)
        entryDao.create(income)
        entryDao.create(transfer)

        val entries = entryDao.getAll().first()
        entries shouldBe listOf(
            transfer.copy(id = entries[0].id),
            income.copy(id = entries[1].id),
            expense.copy(id = entries[2].id),
        )
    }

    "entries are ordered newest first" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val main = accountDao.getAll().first().first()
        val groceries = categoryDao.getAll().first().first()

        entryDao.create(ExpenseEntry(0, groceries, "First", main, 100, Currency.CHF))
        entryDao.create(ExpenseEntry(0, groceries, "Second", main, 200, Currency.CHF))
        entryDao.create(ExpenseEntry(0, groceries, "Third", main, 300, Currency.CHF))

        entryDao.getAll().first().map { it.description } shouldBe listOf("Third", "Second", "First")
    }

    "delete removes an entry" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val main = accountDao.getAll().first().first()
        val groceries = categoryDao.getAll().first().first()

        entryDao.create(ExpenseEntry(0, groceries, "First", main, 100, Currency.CHF))
        entryDao.create(ExpenseEntry(0, groceries, "Second", main, 200, Currency.CHF))

        val created = entryDao.getAll().first()
        val second = created.first()
        val first = created.last()
        entryDao.delete(second.id)

        entryDao.getAll().first() shouldBe listOf(first)
    }
})
