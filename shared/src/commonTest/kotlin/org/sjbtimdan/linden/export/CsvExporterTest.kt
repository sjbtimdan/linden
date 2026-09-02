package org.sjbtimdan.linden.export

import app.cash.sqldelight.async.coroutines.awaitAsOne
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import java.io.ByteArrayOutputStream
import kotlin.time.Instant

private val cash = Account(id = 1, name = "Cash", currency = Currency.CHF)
private val savings = Account(id = 2, name = "Savings", currency = Currency.EUR)
private val food = Category(id = 1, name = "Food", type = CategoryType.Expense, icon = CategoryIcon.Restaurant)
private val salary = Category(id = 2, name = "Salary", type = CategoryType.Income, icon = CategoryIcon.Savings)

class CsvExporterTest : StringSpec({

    "entriesToCsv writes a header and one row per entry" {
        val csv = entriesToCsv(
            listOf(
                ExpenseEntry(
                    id = 1,
                    category = food,
                    description = "Coffee",
                    account = cash,
                    amount = 450,
                    createdAt = Instant.fromEpochMilliseconds(1_700_000_000_000),
                    createdZone = TimeZone.UTC,
                ),
                IncomeEntry(
                    id = 2,
                    category = salary,
                    description = "Salary",
                    account = cash,
                    amount = 5_000,
                    createdAt = Instant.fromEpochMilliseconds(1_700_100_000_000),
                    createdZone = TimeZone.UTC,
                ),
            ),
        )

        val lines = csv.trimEnd().lines()
        lines[0] shouldBe "type,date,account,category,description,amount,currency,toAccount,toAmount,toCurrency"
        lines[1] shouldBe "Expense,2023-11-14T22:13:20Z,Cash,Food,Coffee,4.50,CHF,,,"
        lines[2] shouldBe "Income,2023-11-16T02:00:00Z,Cash,Salary,Salary,50.00,CHF,,,"
    }

    "entriesToCsv writes transfer rows with toAccount, toAmount and toCurrency" {
        val csv = entriesToCsv(
            listOf(
                TransferEntry(
                    id = 3,
                    category = null,
                    description = "Move",
                    account = cash,
                    amount = 1_000,
                    createdAt = Instant.fromEpochMilliseconds(1_700_200_000_000),
                    createdZone = TimeZone.UTC,
                    toAccount = savings,
                    toAmount = 90,
                ),
            ),
        )

        val lines = csv.trimEnd().lines()
        lines[1] shouldBe "Transfer,2023-11-17T05:46:40Z,Cash,,Move,10.00,CHF,Savings,0.90,EUR"
    }

    "entriesToCsv returns just the header for an empty list" {
        entriesToCsv(
            emptyList(),
        ) shouldBe "type,date,account,category,description,amount,currency,toAccount,toAmount,toCurrency\n"
    }

    "entriesToCsv leaves null description and category empty" {
        val csv = entriesToCsv(
            listOf(
                ExpenseEntry(
                    id = 4,
                    category = food,
                    description = null,
                    account = cash,
                    amount = 5,
                    createdAt = Instant.fromEpochMilliseconds(1_700_300_000_000),
                    createdZone = TimeZone.UTC,
                ),
            ),
        )

        val lines = csv.trimEnd().lines()
        lines[1] shouldBe "Expense,2023-11-18T09:33:20Z,Cash,Food,,0.05,CHF,,,"
    }

    "entriesToCsv quotes fields containing commas, quotes or newlines" {
        val csv = entriesToCsv(
            listOf(
                ExpenseEntry(
                    id = 5,
                    category = food,
                    description = "Coffee, \"large\"\nwith milk",
                    account = cash,
                    amount = 450,
                    createdAt = Instant.fromEpochMilliseconds(1_700_400_000_000),
                    createdZone = TimeZone.UTC,
                ),
            ),
        )

        csv shouldContain "\"Coffee, \"\"large\"\"\nwith milk\""
    }

    "entriesToCsv formats amounts with two decimals and no floating point" {
        val csv = entriesToCsv(
            listOf(
                ExpenseEntry(
                    id = 6,
                    category = food,
                    description = "A",
                    account = cash,
                    amount = 1_234_567,
                    createdAt = Instant.fromEpochMilliseconds(1_700_500_000_000),
                    createdZone = TimeZone.UTC,
                ),
            ),
        )

        val lines = csv.trimEnd().lines()
        lines[1] shouldContain ",12345.67,CHF"
    }

    "CsvExportManager writes all entries from the database" {
        val database = lindenDatabase().apply {
            accountQueries.insert("Cash", "CHF", 0)
            val cashId = importQueries.lastInsertId().awaitAsOne()
            categoryQueries.insert("Food", CategoryType.Expense.name, null)
            val categoryId = importQueries.lastInsertId().awaitAsOne()
            entryQueries.insert(
                type = "Expense",
                categoryId = categoryId,
                description = "Coffee",
                accountId = cashId,
                amount = 450,
                toAccountId = null,
                toAmount = null,
                createdAt = 1_700_000_000_000,
                createdZone = "Europe/Zurich",
            )
        }

        val output = ByteArrayOutputStream()
        CsvExportManager(EntryDao(database.entryQueries)).exportTo(output)

        val csv = output.toString()
        csv shouldContain "Expense,"
        csv shouldContain "Coffee"
        csv shouldContain ",4.50,CHF"
    }
})
