package org.sjbtimdan.linden.data

import app.cash.sqldelight.db.QueryResult
import app.cash.sqldelight.db.SqlCursor
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import java.io.File
import java.nio.file.Files

/**
 * Boots a database exactly like the desktop entry point does: [DatabaseDriverFactory]
 * opening an existing database file, followed by [createLindenDatabase]. Unlike the
 * in-memory helpers, this exercises the real "file already exists" path, where the
 * driver must migrate an on-disk schema instead of creating tables from scratch.
 */
class DesktopStartupMigrationTest : StringSpec({

    "startup on an existing unstamped database migrates it to the current schema" {
        val dir = Files.createTempDirectory("linden-startup-test").toFile()
        val dbFile = File(dir, "linden.db")

        // A database left behind by the previous desktop build: every table in the
        // v4 shape (AccountEntity has no `hidden` column yet) and user_version never
        // stamped — desktop builds before startup migrations existed left it at 0.
        val seed = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
        seed.execute(
            null,
            "CREATE TABLE AccountEntity (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, currency TEXT NOT NULL, initialBalance INTEGER NOT NULL DEFAULT 0)",
            0,
        )
        seed.execute(
            null,
            "CREATE TABLE CategoryEntity (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT NOT NULL UNIQUE, type TEXT NOT NULL, icon TEXT)",
            0,
        )
        seed.execute(
            null,
            "CREATE TABLE EntryEntity (id INTEGER PRIMARY KEY AUTOINCREMENT, type TEXT NOT NULL, " +
                "category_id INTEGER, description TEXT, account_id INTEGER NOT NULL, amount INTEGER NOT NULL " +
                "CHECK (amount >= 0), to_account_id INTEGER, to_amount INTEGER " +
                "CHECK (to_amount IS NULL OR to_amount >= 0), created_at INTEGER NOT NULL, created_zone TEXT NOT NULL)",
            0,
        )
        seed.execute(
            null,
            "CREATE TABLE BudgetEntity (id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "category_name TEXT NOT NULL UNIQUE, limit_minor INTEGER NOT NULL)",
            0,
        )
        seed.execute(
            null,
            "CREATE TABLE FxRateEntity (baseCurrency TEXT NOT NULL, quoteCurrency TEXT NOT NULL, " +
                "rate REAL NOT NULL, date TEXT NOT NULL, fetchedAt INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY (baseCurrency, quoteCurrency))",
            0,
        )
        seed.execute(
            null,
            "CREATE TABLE AppSettingsEntity (key TEXT PRIMARY KEY, value TEXT NOT NULL)",
            0,
        )
        seed.execute(
            null,
            "INSERT INTO AccountEntity (name, currency, initialBalance) VALUES ('Cash', 'CHF', 500)",
            0,
        )
        seed.execute(
            null,
            "INSERT INTO AccountEntity (name, currency, initialBalance) VALUES ('Savings', 'USD', 0)",
            0,
        )
        seed.execute(
            null,
            "INSERT INTO CategoryEntity (name, type, icon) VALUES ('Groceries', 'Expense', NULL)",
            0,
        )
        seed.execute(
            null,
            "INSERT INTO EntryEntity (type, category_id, description, account_id, amount, " +
                "to_account_id, to_amount, created_at, created_zone) VALUES " +
                "('Expense', 1, 'Coffee', 1, 450, NULL, NULL, 1700000000000, 'Europe/Zurich')",
            0,
        )
        seed.execute(null, "INSERT INTO AppSettingsEntity (key, value) VALUES ('theme', 'SYSTEM')", 0)

        // Boot like the desktop entry point: factory opens the file, then the
        // common bootstrap creates the schema over the (now migrated) driver.
        val driver = DatabaseDriverFactory().createDriverAt(dbFile)
        val database = createLindenDatabase(driver)

        // Current-schema queries work against the migrated file, with every
        // pre-existing account visible and the data intact.
        database.accountQueries.selectAll().executeAsList().map { it.name to it.hidden } shouldBe
            listOf("Cash" to 0L, "Savings" to 0L)
        database.accountQueries.updateHidden(1, 1)
        database.accountQueries.selectAll().executeAsList()
            .first { it.name == "Cash" }.hidden shouldBe 1L
        database.categoryQueries.selectAll().executeAsList().map { it.name } shouldBe listOf("Groceries")
        database.entryQueries.selectAllRows().executeAsList().shouldHaveSize(1)
        database.settingsQueries.selectAll().executeAsList().map { it.key } shouldBe listOf("theme")

        // The file is stamped with the current schema version, so the next boot
        // skips the migration instead of replaying it.
        userVersionOf(dbFile) shouldBe 5L
    }
})

private fun userVersionOf(dbFile: File): Long {
    val driver = JdbcSqliteDriver("jdbc:sqlite:${dbFile.absolutePath}")
    val mapper = { cursor: SqlCursor ->
        QueryResult.Value(if (cursor.next().value) cursor.getLong(0) else null)
    }
    return driver.executeQuery(null, "PRAGMA user_version", mapper, 0, null).value ?: 0L
}
