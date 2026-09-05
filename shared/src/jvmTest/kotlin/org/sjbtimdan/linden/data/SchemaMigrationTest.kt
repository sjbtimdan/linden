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

        LindenDatabase.Schema.migrate(driver, 1, 2).await()

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

    "migrating from v2 to v3 preserves accounts/categories, renames duplicates and enforces unique names" {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Create the v2 schema (no UNIQUE constraints) and seed duplicates.
        val createAccount = """
            CREATE TABLE AccountEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                currency TEXT NOT NULL,
                initialBalance INTEGER NOT NULL DEFAULT 0
            );
        """.trimIndent()
        driver.execute(null, createAccount, 0)
        val createCategory = """
            CREATE TABLE CategoryEntity (
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                type TEXT NOT NULL,
                icon TEXT
            );
        """.trimIndent()
        driver.execute(null, createCategory, 0)

        driver.execute(null, "INSERT INTO AccountEntity (name, currency, initialBalance) VALUES ('Cash', 'CHF', 0)", 0)
        driver.execute(null, "INSERT INTO AccountEntity (name, currency, initialBalance) VALUES ('Cash', 'CHF', 0)", 0)
        driver.execute(
            null,
            "INSERT INTO AccountEntity (name, currency, initialBalance) VALUES ('Savings', 'USD', 0)",
            0,
        )
        driver.execute(null, "INSERT INTO CategoryEntity (name, type, icon) VALUES ('Food', 'Expense', NULL)", 0)
        driver.execute(null, "INSERT INTO CategoryEntity (name, type, icon) VALUES ('Food', 'Expense', NULL)", 0)

        LindenDatabase.Schema.migrate(driver, 2, 3).await()

        val db = LindenDatabase(driver)
        val accounts = db.accountQueries.selectAll().executeAsList()
        accounts.map { it.name } shouldBe listOf("Cash", "Cash (2)", "Savings")
        val categories = db.categoryQueries.selectAll().executeAsList()
        categories.map { it.name } shouldBe listOf("Food", "Food (2)")

        // The UNIQUE constraint now rejects a duplicate name.
        shouldThrow<Exception> {
            db.accountQueries.insert(name = "Cash", currency = "EUR", initialBalance = 0)
        }
        shouldThrow<Exception> {
            db.categoryQueries.insert(name = "Food", type = "Expense", icon = null)
        }
    }

    "migrating from v3 to v4 creates BudgetEntity and migrates JSON budgets" {
        val driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)

        // Create the v3 schema (no BudgetEntity) and seed a JSON budget in settings.
        val createSettings = """
            CREATE TABLE AppSettingsEntity (
                key TEXT PRIMARY KEY,
                value TEXT NOT NULL
            );
        """.trimIndent()
        driver.execute(null, createSettings, 0)
        driver.execute(
            null,
            "INSERT INTO AppSettingsEntity (key, value) VALUES ('budgets', '[{\"categoryName\":\"Groceries\",\"limitMinor\":80000},{\"categoryName\":\"Food\",\"limitMinor\":50000}]')",
            0,
        )

        LindenDatabase.Schema.migrate(driver, 3, 4).await()

        val db = LindenDatabase(driver)
        val budgets = db.budgetQueries.selectAll().executeAsList()
        budgets.map { it.category_name to it.limit_minor } shouldBe
            listOf("Food" to 50_000L, "Groceries" to 80_000L)

        // The settings key is removed.
        db.settingsQueries.selectAll().executeAsList().map { it.key } shouldBe emptyList()
    }
})
