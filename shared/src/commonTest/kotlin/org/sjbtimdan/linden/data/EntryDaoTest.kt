package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Instant

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
            createdAt = Instant.fromEpochMilliseconds(1_000),
            createdZone = TimeZone.of("Europe/Berlin"),
        )
        val income = IncomeEntry(
            id = 0,
            category = salary,
            description = "Salary",
            account = main,
            amount = 50_000,
            createdAt = Instant.fromEpochMilliseconds(2_000),
            createdZone = TimeZone.of("Asia/Tokyo"),
        )
        val transfer = TransferEntry(
            id = 0,
            category = null,
            description = null,
            account = main,
            amount = 10_000,
            toAccount = savings,
            toAmount = 9_500,
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

        entryDao.create(ExpenseEntry(0, groceries, "First", main, 100))
        entryDao.create(ExpenseEntry(0, groceries, "Second", main, 200))
        entryDao.create(ExpenseEntry(0, groceries, "Third", main, 300))

        entryDao.getAll().first().map { it.description } shouldBe listOf("Third", "Second", "First")
    }

    "latest returns the most recent entry of the requested type by date" {
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

        // mirrored from a backup import: newest entries were inserted first,
        // so a newer date can have a lower id than an older one
        entryDao.create(
            ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Instant.fromEpochMilliseconds(1_000)),
        )
        entryDao.create(
            IncomeEntry(0, salary, "Salary", main, 50_000, createdAt = Instant.fromEpochMilliseconds(2_000)),
        )
        entryDao.create(
            ExpenseEntry(0, groceries, "Lunch", main, 1_200, createdAt = Instant.fromEpochMilliseconds(3_000)),
        )
        entryDao.create(ExpenseEntry(0, groceries, "Old", main, 500, createdAt = Instant.fromEpochMilliseconds(500)))

        entryDao.latest(EntryType.Expense)!!.description shouldBe "Lunch"
        entryDao.latest(EntryType.Income)!!.description shouldBe "Salary"
        entryDao.latest(EntryType.Transfer) shouldBe null
    }

    "getSince returns only entries at or after the boundary, newest first" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val main = accountDao.getAll().first().first()
        val groceries = categoryDao.getAll().first().first()

        entryDao.create(ExpenseEntry(0, groceries, "Old", main, 100, createdAt = Instant.fromEpochMilliseconds(1_000)))
        entryDao.create(ExpenseEntry(0, groceries, "Mid", main, 200, createdAt = Instant.fromEpochMilliseconds(2_000)))
        entryDao.create(ExpenseEntry(0, groceries, "New", main, 300, createdAt = Instant.fromEpochMilliseconds(3_000)))

        entryDao.getSince(2_000).first().map { it.description } shouldBe listOf("New", "Mid")
        entryDao.getSince(3_000).first().map { it.description } shouldBe listOf("New")
        entryDao.getSince(4_000).first() shouldBe emptyList()
    }

    "getUpTo returns only entries at or before the boundary, newest first" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val main = accountDao.getAll().first().first()
        val groceries = categoryDao.getAll().first().first()

        entryDao.create(ExpenseEntry(0, groceries, "Old", main, 100, createdAt = Instant.fromEpochMilliseconds(1_000)))
        entryDao.create(ExpenseEntry(0, groceries, "Mid", main, 200, createdAt = Instant.fromEpochMilliseconds(2_000)))
        entryDao.create(ExpenseEntry(0, groceries, "New", main, 300, createdAt = Instant.fromEpochMilliseconds(3_000)))

        entryDao.getUpTo(2_000).first().map { it.description } shouldBe listOf("Mid", "Old")
        entryDao.getUpTo(1_000).first().map { it.description } shouldBe listOf("Old")
        entryDao.getUpTo(0).first() shouldBe emptyList()
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

        entryDao.create(ExpenseEntry(0, groceries, "First", main, 100))
        entryDao.create(ExpenseEntry(0, groceries, "Second", main, 200))

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

        entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
        val created = entryDao.getAll().first().first()
        val updatedEntry = IncomeEntry(
            id = created.id,
            category = salary,
            description = "Bonus",
            account = main,
            amount = 5_000,
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

        entryDao.create(ExpenseEntry(0, groceries, null, main, 100))

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

        entryDao.create(TransferEntry(0, null, null, main, 10_000, toAccount = savings, toAmount = 9_500))
        val created = entryDao.getAll().first().first()

        val updatedEntry = TransferEntry(
            id = created.id,
            category = null,
            description = "Top up",
            account = main,
            amount = 20_000,
            toAccount = savings,
            toAmount = 19_000,
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

        entryDao.create(TransferEntry(0, null, null, main, 10_000, toAccount = savings, toAmount = 9_500))

        val created = entryDao.getAll().first().first() as TransferEntry
        created.toAmount shouldBe null
    }

    "transfers with a category round-trip and keep it on update" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.EUR)
        categoryDao.create("General", CategoryType.Both)
        val accounts = accountDao.getAll().first()
        val main = accounts.first()
        val savings = accounts.last()
        val general = categoryDao.getAll().first().first()

        entryDao.create(TransferEntry(0, general, "Move", main, 10_000, toAccount = savings, toAmount = 9_500))
        val created = entryDao.getAll().first().first() as TransferEntry
        created.category shouldBe general

        val updated = created.copy(amount = 20_000, toAmount = 19_000)
        entryDao.update(updated)

        val saved = entryDao.getAll().first().first() as TransferEntry
        saved.amount shouldBe 20_000
        saved.toAmount shouldBe 19_000
        saved.category shouldBe general
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

        val expense = ExpenseEntry(0, groceries, "Coffee", main, 450)
        val income = IncomeEntry(0, salary, "Salary", main, 50_000)
        val transfer = TransferEntry(0, null, null, main, 10_000, toAccount = savings, toAmount = 9_500)

        entryDao.create(expense)
        entryDao.create(income)
        entryDao.create(transfer)

        // getAll() is newest first: transfer, income, expense
        val entries = entryDao.getAll().first()
        entries.filterIsInstance<ExpenseEntry>() shouldBe listOf(expense.copy(id = entries[2].id))
        entries.filterIsInstance<IncomeEntry>() shouldBe listOf(income.copy(id = entries[1].id))
        entries.filterIsInstance<TransferEntry>() shouldBe listOf(transfer.copy(id = entries[0].id))
    }

    "accountDeltasUpTo excludes entries created after the cutoff" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.CHF)
        categoryDao.create("Groceries", CategoryType.Expense)
        val accounts = accountDao.getAll().first()
        val main = accounts.first { it.name == "Main" }
        val savings = accounts.first { it.name == "Savings" }
        val groceries = categoryDao.getAll().first().first()

        // A past expense and a future transfer out of Main.
        entryDao.create(
            ExpenseEntry(0, groceries, "Coffee", main, 450, createdAt = Instant.fromEpochMilliseconds(1_000)),
        )
        entryDao.create(
            TransferEntry(
                0,
                null,
                null,
                main,
                10_000,
                toAccount = savings,
                toAmount = null,
                createdAt = Instant.fromEpochMilliseconds(9_999_999_999),
            ),
        )

        // Cutoff after the expense but before the future transfer.
        entryDao.accountDeltasUpTo(2_000).first() shouldBe mapOf(main.id to -450L)
    }

    "accountsWithEntries includes source accounts and transfer targets but not empty accounts" {
        val database = lindenDatabase()
        val entryDao = EntryDao(database.entryQueries)
        val accountDao = AccountDao(database.accountQueries)
        val categoryDao = CategoryDao(database.categoryQueries)

        accountDao.create("Main", Currency.CHF)
        accountDao.create("Savings", Currency.EUR)
        accountDao.create("Empty", Currency.CHF)
        val accounts = accountDao.getAll().first()
        val main = accounts.first { it.name == "Main" }
        val savings = accounts.first { it.name == "Savings" }
        categoryDao.create("Groceries", CategoryType.Expense)
        categoryDao.create("Salary", CategoryType.Income)
        val groceries = categoryDao.getAll().first().first { it.name == "Groceries" }
        val salary = categoryDao.getAll().first().first { it.name == "Salary" }

        // a net-zero account still counts as having entries
        entryDao.create(ExpenseEntry(0, groceries, "Coffee", main, 450))
        entryDao.create(IncomeEntry(0, salary, "Pay", main, 450))
        // a transfer target counts too
        entryDao.create(TransferEntry(0, null, null, main, 10_000, toAccount = savings, toAmount = 9_500))

        entryDao.accountsWithEntries().first() shouldBe setOf(main.id, savings.id)
    }
})
