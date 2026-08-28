package org.sjbtimdan.linden.backup

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.LocalDateTime
import org.sjbtimdan.linden.data.THEME_KEY
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.ThemeMode
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

class LindenBackupManagerTest : StringSpec({

    /** Creates a database with accounts, a category, entries, a setting and an FX rate. */
    suspend fun seedDatabase() = lindenDatabase().apply {
        accountQueries.insert("Cash", "CHF", 500)
        val cashId = importQueries.lastInsertId().awaitAsOne()
        accountQueries.insert("Savings", "EUR", 0)
        val savingsId = importQueries.lastInsertId().awaitAsOne()
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
        entryQueries.insert(
            type = "Transfer",
            categoryId = null,
            description = "Move",
            accountId = cashId,
            amount = 1_000,
            toAccountId = savingsId,
            toAmount = 90,
            createdAt = 1_700_100_000_000,
            createdZone = "Europe/Zurich",
        )
        entryQueries.insert(
            type = "Income",
            categoryId = categoryId,
            description = "Salary",
            accountId = cashId,
            amount = 5_000,
            toAccountId = null,
            toAmount = null,
            createdAt = 1_700_200_000_000,
            createdZone = "Europe/Zurich",
        )
        settingsQueries.insertOrReplace(THEME_KEY, ThemeMode.DARK.name)
        fxRateQueries.insertOrReplace("CHF", "EUR", 1.04, "2026-08-23", 42)
    }

    "backupTo writes a backup that restores into an empty database" {
        val source = seedDatabase()
        val bytes = ByteArrayOutputStream().also { LindenBackupManager(source).backupTo(it) }.toByteArray()

        val restored = lindenDatabase()
        val result = LindenBackupManager(restored).restoreFrom(ByteArrayInputStream(bytes))

        result.accounts shouldBe 2
        result.categories shouldBe 1
        result.entries shouldBe 3

        restored.accountQueries.selectAll().awaitAsList().map { it.id to it.name to it.currency to it.initialBalance }
            .shouldBe(listOf(1L to "Cash" to "CHF" to 500L, 2L to "Savings" to "EUR" to 0L))
        restored.categoryQueries.selectAll().awaitAsList().map { it.id to it.name to it.type }
            .shouldBe(listOf(1L to "Food" to CategoryType.Expense.name))

        val entries = restored.entryQueries.selectAllRows().awaitAsList()
        entries.map { it.id to it.description to it.type } shouldBe
            listOf(
                1L to "Coffee" to "Expense",
                2L to "Move" to "Transfer",
                3L to "Salary" to "Income",
            )
        entries[0].let {
            it.category_id shouldBe 1L
            it.account_id shouldBe 1L
            it.amount shouldBe 450L
            it.created_at shouldBe 1_700_000_000_000L
            it.created_zone shouldBe "Europe/Zurich"
        }
        entries[1].let {
            it.category_id shouldBe null
            it.account_id shouldBe 1L
            it.to_account_id shouldBe 2L
            it.to_amount shouldBe 90L
        }
        entries[2].let {
            it.type shouldBe "Income"
            it.category_id shouldBe 1L
            it.amount shouldBe 5_000L
            it.created_at shouldBe 1_700_200_000_000L
        }

        restored.settingsQueries.selectAll().awaitAsList().map { it.key to it.value_ }
            .shouldBe(listOf(THEME_KEY to ThemeMode.DARK.name))
        restored.fxRateQueries.selectAll().awaitAsList().map { it.baseCurrency to it.quoteCurrency to it.rate }
            .shouldBe(listOf("CHF" to "EUR" to 1.04))
    }

    "restoreFrom replaces all existing data" {
        val bytes = ByteArrayOutputStream().also { LindenBackupManager(seedDatabase()).backupTo(it) }.toByteArray()
        val target = lindenDatabase().apply {
            accountQueries.insert("Old", "USD", 0)
            categoryQueries.insert("Old category", CategoryType.Income.name, null)
            settingsQueries.insertOrReplace(THEME_KEY, ThemeMode.LIGHT.name)
        }

        LindenBackupManager(target).restoreFrom(ByteArrayInputStream(bytes))

        target.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Cash", "Savings")
        target.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Food")
        target.settingsQueries.selectAll().awaitAsList().map { it.key to it.value_ }
            .shouldBe(listOf(THEME_KEY to ThemeMode.DARK.name))
    }

    "backupTo on an empty database produces a backup that restoreFrom rejects" {
        val bytes = ByteArrayOutputStream().also { LindenBackupManager(lindenDatabase()).backupTo(it) }.toByteArray()

        val exception = shouldThrow<LindenBackupException> {
            LindenBackupManager(lindenDatabase()).restoreFrom(ByteArrayInputStream(bytes))
        }

        exception.message shouldContain "empty"
    }

    "restoreFrom rejects a backup with an unsupported format version" {
        val exception = shouldThrow<LindenBackupException> {
            LindenBackupManager(lindenDatabase())
                .restoreFrom(ByteArrayInputStream("""{"formatVersion": 99}""".encodeToByteArray()))
        }

        exception.message shouldContain "incompatible app version"
    }

    "restoreFrom rejects input that is not a Linden backup" {
        val exception = shouldThrow<LindenBackupException> {
            LindenBackupManager(lindenDatabase())
                .restoreFrom(ByteArrayInputStream("this is not json".encodeToByteArray()))
        }

        exception.message shouldContain "not a valid Linden backup"
    }

    "restoreFrom leaves the database untouched when the backup is invalid" {
        val database = seedDatabase()
        val before = database.accountQueries.selectAll().awaitAsList().size

        shouldThrow<LindenBackupException> {
            LindenBackupManager(database).restoreFrom(ByteArrayInputStream("garbage".encodeToByteArray()))
        }

        database.accountQueries.selectAll().awaitAsList().size shouldBe before
        database.entryQueries.selectAllRows().awaitAsList().size shouldBe 3
    }

    "restoreFrom rolls back when an insert fails mid-transaction" {
        val database = seedDatabase()
        val accountsBefore = database.accountQueries.selectAll().awaitAsList()
        val entriesBefore = database.entryQueries.selectAllRows().awaitAsList()
        val settingsBefore = database.settingsQueries.selectAll().awaitAsList()

        // Decodes fine, but the second account duplicates id 1 and violates the primary key.
        val corrupt = """
            {
              "formatVersion": 1,
              "accounts": [
                {"id": 1, "name": "A", "currency": "CHF", "initialBalance": 0},
                {"id": 1, "name": "B", "currency": "USD", "initialBalance": 0}
              ]
            }
        """.trimIndent()

        shouldThrow<Exception> {
            LindenBackupManager(database).restoreFrom(ByteArrayInputStream(corrupt.encodeToByteArray()))
        }

        database.accountQueries.selectAll().awaitAsList() shouldBe accountsBefore
        database.entryQueries.selectAllRows().awaitAsList() shouldBe entriesBefore
        database.settingsQueries.selectAll().awaitAsList() shouldBe settingsBefore
    }

    "restoreFrom rejects an empty backup and leaves the database untouched" {
        val database = seedDatabase()
        val emptyBackup = """{"formatVersion": 1}""".encodeToByteArray()

        shouldThrow<LindenBackupException> {
            LindenBackupManager(database).restoreFrom(ByteArrayInputStream(emptyBackup))
        }

        database.accountQueries.selectAll().awaitAsList().size shouldBe 2
        database.entryQueries.selectAllRows().awaitAsList().size shouldBe 3
        database.settingsQueries.selectAll().awaitAsList().size shouldBe 1
        database.fxRateQueries.selectAll().awaitAsList().size shouldBe 1
    }

    "backupFileName includes a zero-padded local date-time in the name" {
        backupFileName(LocalDateTime(2026, 8, 28, 15, 30, 45)) shouldBe "linden-backup-2026-08-28-153045.json"
    }

    "backupFileName zero-pads single-digit date and time parts" {
        backupFileName(LocalDateTime(2026, 1, 3, 9, 5, 7)) shouldBe "linden-backup-2026-01-03-090507.json"
    }
})
