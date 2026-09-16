package org.sjbtimdan.linden.integration

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.collections.shouldNotBeEmpty
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.AccountEntity
import org.sjbtimdan.linden.BudgetEntity
import org.sjbtimdan.linden.CategoryEntity
import org.sjbtimdan.linden.EntryEntity
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import java.io.File
import java.nio.file.Files

/**
 * Manual-only integration tests that exercise a real on-disk SQLite database
 * seeded from the backup in the repo's `integrationTestData/` directory. These
 * are tagged "integration" and excluded from the normal test run; run them with
 * `./gradlew :shared:integrationTest`.
 */
class IntegrationDatabaseTest : StringSpec({

    "restoring the integration backup, saving, and reloading preserves all data" {
        val backupZip = integrationBackupZip()
        val dir = Files.createTempDirectory("linden-integration-test").toFile()

        // Load the integration backup into a fresh file-based database.
        val first = openDatabase(File(dir, "first.db"))
        LindenBackupManager(first).restoreFrom(backupZip.inputStream())
        val before = snapshot(first)
        before.accounts.shouldNotBeEmpty()
        before.entries.shouldNotBeEmpty()

        // Save the database and reload it into a second fresh database.
        val savedZip = File(dir, "saved.zip")
        LindenBackupManager(first).backupTo(savedZip.outputStream())
        val second = openDatabase(File(dir, "second.db"))
        LindenBackupManager(second).restoreFrom(savedZip.inputStream())
        val after = snapshot(second)

        after shouldBe before
    }

    "integration data survives closing and reopening the database file" {
        val backupZip = integrationBackupZip()
        val dir = Files.createTempDirectory("linden-integration-test").toFile()

        // Load the integration backup into a fresh file-based database.
        val dbFile = File(dir, "linden.db")
        val driver = DatabaseDriverFactory().createDriverAt(dbFile)
        val database = createLindenDatabase(driver)
        LindenBackupManager(database).restoreFrom(backupZip.inputStream())
        val before = snapshot(database)

        // SQLite persists on every write; reloading means closing and reopening the file.
        driver.close()
        val reloaded = openDatabase(dbFile)
        val after = snapshot(reloaded)

        after shouldBe before
    }
})

private data class DatabaseSnapshot(
    val accounts: List<AccountEntity>,
    val categories: List<CategoryEntity>,
    val entries: List<EntryEntity>,
    val budgets: List<BudgetEntity>,
)

private suspend fun snapshot(database: LindenDatabase): DatabaseSnapshot = DatabaseSnapshot(
    accounts = database.accountQueries.selectAll().awaitAsList(),
    categories = database.categoryQueries.selectAll().awaitAsList(),
    entries = database.entryQueries.selectAllRows().awaitAsList().sortedBy { it.id },
    budgets = database.budgetQueries.selectAll().awaitAsList(),
)

private suspend fun openDatabase(dbFile: File): LindenDatabase {
    val driver = DatabaseDriverFactory().createDriverAt(dbFile)
    return createLindenDatabase(driver)
}

private fun integrationBackupZip(): File {
    val dir = listOf(File("../integrationTestData"), File("integrationTestData"))
        .firstOrNull { it.isDirectory }
        ?: error("integrationTestData directory not found (looked in ../integrationTestData and integrationTestData)")
    return dir.listFiles { file -> file.isFile && file.extension == "zip" }?.firstOrNull()
        ?: error("No backup zip found in $dir — add one to run integration tests")
}