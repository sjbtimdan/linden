package org.sjbtimdan.linden.data

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.db.LindenDatabase

class SchemaMigrationTest : StringSpec({
    "migrating from v1 to v2 preserves existing entries and enforces the amount check" {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Create the v1 schema (no CHECK constraint) and seed an entry.
        val createTable = """
            CREATE TABLE EntryEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                type TEXT NOT NULL,
                category_id INTEGER,
                description TEXT,
                account_id INTEGER NOT NULL,
                amount INTEGER NOT NULL,
                to_account_id INTEGER,
                to_amount INTEGER,
                created_at INTEGER NOT NULL,
                created_zone TEXT NOT NULL
            );
        """.trimIndent()
        driver.execute(null, createTable, 0)

        val insertEntry = """
            INSERT INTO EntryEntity (type, category_id, description, account_id, amount, to_account_id, to_amount, created_at, created_zone)
            VALUES ('Expense', NULL, 'Coffee', 1, 450, NULL, NULL, 0, 'UTC');
        """.trimIndent()
        driver.execute(null, insertEntry, 0)

        // Migrate to v2.
        LindenDatabase.Schema.migrate(driver, 1, 2).await()

        // Data is preserved.
        val db = LindenDatabase(driver)
        val entries = db.entryQueries.selectAllRows().executeAsList()
        entries.size shouldBe 1
        entries.single().amount shouldBe 450

        // The CHECK constraint now rejects a negative amount.
        shouldThrow<Exception> {
            db.entryQueries.insert(
                type = "Expense",
                categoryId = null,
                description = null,
                accountId = 1,
                amount = -450,
                toAccountId = null,
                toAmount = null,
                createdAt = 0,
                createdZone = "UTC",
            )
        }

        // The CHECK constraint rejects a negative to_amount.
        shouldThrow<Exception> {
            db.entryQueries.insert(
                type = "Transfer",
                categoryId = null,
                description = null,
                accountId = 1,
                amount = 450,
                toAccountId = 2,
                toAmount = -450,
                createdAt = 0,
                createdZone = "UTC",
            )
        }
    }
})
