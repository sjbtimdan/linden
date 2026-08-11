package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
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
            createdAt = Instant.fromEpochMilliseconds(1_000),
            createdZone = TimeZone.of("Europe/Berlin"),
        )
        val income = IncomeEntry(
            id = 0,
            category = salary,
            description = "Salary",
            account = main,
            amount = 50_000,
            currency = Currency.CHF,
            createdAt = Instant.fromEpochMilliseconds(2_000),
            createdZone = TimeZone.of("Asia/Tokyo"),
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
            createdAt = Instant.fromEpochMilliseconds(3_000),
            createdZone = TimeZone.of("America/New_York"),
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

    "update modifies an entry in place" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        categoryDao.create("Salary", CategoryType.Income)
        val main = accountDao.getAll().first().first()
        val categories = categoryDao.getAll().first()
        val groceries = categories.first()
        val salary = categories.last()

        entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF))
        val created = entryDao.getAll().first().first()
        val updatedEntry = IncomeEntry(
            id = created.id,
            category = salary,
            description = "Bonus",
            account = main,
            amount = 5_000,
            currency = Currency.CHF,
            createdAt = Instant.fromEpochMilliseconds(4_000),
            createdZone = TimeZone.of("Asia/Tokyo"),
        )

        entryDao.update(updatedEntry)

        entryDao.getAll().first().first() shouldBe updatedEntry
    }

    "entries with a null description round-trip" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val main = accountDao.getAll().first().first()
        val groceries = categoryDao.getAll().first().first()

        entryDao.create(ExpenseEntry(0, groceries, null, main, 100, Currency.CHF))

        entryDao.getAll().first().first().description shouldBe null
    }

    "update modifies a transfer in place" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.EUR)
        val accounts = accountDao.getAll().first()
        val main = accounts.first()
        val savings = accounts.last()

        entryDao.create(TransferEntry(0, null, null, main, 10_000, Currency.CHF, toAccount = savings, toAmount = 9_500, toCurrency = Currency.EUR))
        val created = entryDao.getAll().first().first()

        val updatedEntry = TransferEntry(
            id = created.id,
            category = null,
            description = "Top up",
            account = main,
            amount = 20_000,
            currency = Currency.CHF,
            toAccount = savings,
            toAmount = 19_000,
            toCurrency = Currency.EUR,
        )
        entryDao.update(updatedEntry)

        entryDao.getAll().first().first() shouldBe updatedEntry
    }

    "same-currency transfers store a null toAmount" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.CHF)
        val accounts = accountDao.getAll().first()
        val main = accounts.first()
        val savings = accounts.last()

        entryDao.create(TransferEntry(0, null, null, main, 10_000, Currency.CHF, toAccount = savings, toAmount = 9_500, toCurrency = Currency.CHF))

        val created = entryDao.getAll().first().first() as TransferEntry
        created.toAmount shouldBe null
    }

    "typed read helpers return only their own entries" {
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

        val expense = ExpenseEntry(0, groceries, "Coffee", main, 450, Currency.CHF)
        val income = IncomeEntry(0, salary, "Salary", main, 50_000, Currency.CHF)
        val transfer = TransferEntry(0, null, null, main, 10_000, Currency.CHF, toAccount = savings, toAmount = 9_500, toCurrency = Currency.EUR)

        entryDao.create(expense)
        entryDao.create(income)
        entryDao.create(transfer)

        // getAll() is newest first: transfer, income, expense
        val entries = entryDao.getAll().first()
        entryDao.getExpenses().first() shouldBe listOf(expense.copy(id = entries[2].id))
        entryDao.getIncomes().first() shouldBe listOf(income.copy(id = entries[1].id))
        entryDao.getTransfers().first() shouldBe listOf(transfer.copy(id = entries[0].id))
    }
})
