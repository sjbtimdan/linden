package org.sjbtimdan.linden.imports

import app.cash.sqldelight.async.coroutines.awaitAsList
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.types.shouldBeInstanceOf
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.time.Instant
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

class IvyImporterSpec : StringSpec({
    fun buildIvyZipWithEntryNames(vararg names: String, json: String, charset: Charset = Charsets.UTF_16BE): ByteArray {
        val jsonBytes = charset.encode(json).array()
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            names.forEachIndexed { index, name ->
                zos.putNextEntry(ZipEntry(name))
                zos.write(if (index == names.size - 1) jsonBytes else byteArrayOf())
                zos.closeEntry()
            }
        }
        return bos.toByteArray()
    }

    "imports accounts, categories and transactions from the sample backup" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val result = importer.import(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))

        result.accounts shouldBe 3
        result.categories shouldBe 3
        result.transactions shouldBe 5

        database.accountQueries.selectAll().awaitAsList().map { it.name to it.currency } shouldBe listOf(
            "Euro Bank" to "EUR",
            "UK Savings" to "GBP",
            "Wallet" to "USD",
        )
        database.categoryQueries.selectAll().awaitAsList().map { it.name to it.type } shouldBe listOf(
            "Food" to CategoryType.Both.name,
            "General" to CategoryType.Both.name,
            "Salary" to CategoryType.Both.name,
        )

        val entries = EntryDao(database.entryQueries).getAll().first()
        entries.map { it.type } shouldBe listOf(
            EntryType.Income,
            EntryType.Expense,
            EntryType.Transfer,
            EntryType.Income,
            EntryType.Expense,
        )

        val bonus = entries[0].shouldBeInstanceOf<IncomeEntry>()
        bonus.amount shouldBe 15_000
        bonus.currency shouldBe Currency.GBP
        bonus.account.name shouldBe "UK Savings"
        bonus.category.name shouldBe "Salary"
        bonus.description shouldBe "Bonus"
        bonus.createdAt shouldBe Instant.fromEpochMilliseconds(946771200000)

        val coffee = entries[1].shouldBeInstanceOf<ExpenseEntry>()
        coffee.amount shouldBe 575
        coffee.currency shouldBe Currency.USD
        coffee.account.name shouldBe "Wallet"
        coffee.category.name shouldBe "Food"
        coffee.description shouldBe "Coffee"
        coffee.createdAt shouldBe Instant.fromEpochMilliseconds(946684800000)

        val transfer = entries[2].shouldBeInstanceOf<TransferEntry>()
        transfer.amount shouldBe 50_000
        transfer.currency shouldBe Currency.USD
        transfer.account.name shouldBe "Wallet"
        transfer.toAccount.name shouldBe "UK Savings"
        transfer.toAmount shouldBe 50_000
        transfer.toCurrency shouldBe Currency.GBP
        transfer.category?.name shouldBe "General"
        transfer.description shouldBe "Transfer to savings"

        val salary = entries[3].shouldBeInstanceOf<IncomeEntry>()
        salary.amount shouldBe 320_000
        salary.currency shouldBe Currency.EUR
        salary.account.name shouldBe "Euro Bank"
        salary.category.name shouldBe "Salary"
        salary.description shouldBe "June salary"

        val groceries = entries[4].shouldBeInstanceOf<ExpenseEntry>()
        groceries.amount shouldBe 4_550
        groceries.currency shouldBe Currency.USD
        groceries.account.name shouldBe "Wallet"
        groceries.category.name shouldBe "Food"
        groceries.description shouldBe "Grocery run"
    }

    "import replaces existing data" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)
        val accountDao = org.sjbtimdan.linden.data.AccountDao(database.accountQueries)
        val categoryDao = org.sjbtimdan.linden.data.CategoryDao(database.categoryQueries)
        val entryDao = EntryDao(database.entryQueries)
        accountDao.create("Old Account", Currency.CHF)
        categoryDao.create("Old Category", CategoryType.Expense)
        val oldAccount = accountDao.getAll().first().first()
        val oldCategory = categoryDao.getAll().first().first()
        entryDao.create(ExpenseEntry(0, oldCategory, "Old entry", oldAccount, 100, Currency.CHF))

        importer.import(ByteArrayInputStream(buildIvyZip(minimalIvyJson)))

        database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe
            listOf("Euro Bank", "UK Savings", "Wallet")
        database.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe
            listOf("Food", "General", "Salary")
        entryDao.getAll().first().size shouldBe 5
    }

    "import rolls back and reports an error on invalid JSON" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)
        val accountDao = org.sjbtimdan.linden.data.AccountDao(database.accountQueries)
        accountDao.create("Survivor", Currency.CHF)

        val error = shouldThrow<IvyImportException> {
            importer.import(ByteArrayInputStream(buildIvyZip("this is not json")))
        }
        error.message shouldContain "not a valid Ivy backup"

        database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Survivor")
    }

    "import rolls back when a transaction is invalid" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)
        val accountDao = org.sjbtimdan.linden.data.AccountDao(database.accountQueries)
        accountDao.create("Survivor", Currency.CHF)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "a1",
                  "categoryId": "c1",
                  "currency": "XXX",
                  "title": "Orphan",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        shouldThrow<IvyImportException> {
            importer.import(ByteArrayInputStream(buildIvyZip(json)))
        }

        database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Survivor")
        database.categoryQueries.selectAll().awaitAsList() shouldBe emptyList()
        EntryDao(database.entryQueries).getAll().first() shouldBe emptyList()
    }

    "assigns uncategorised expenses and incomes to a single auto-created 'Imported Entries' category" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "a1",
                  "title": "Untagged coffee",
                  "dateTime": 946684800000
                },
                {
                  "id": "t2",
                  "type": "INCOME",
                  "amount": 100.0,
                  "accountId": "a1",
                  "title": "Untagged gift",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.categories shouldBe 2
        database.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Food", "Imported Entries")
        EntryDao(database.entryQueries).getAll().first().map { it.category?.name } shouldBe listOf("Imported Entries", "Imported Entries")
    }

    "does not create a fallback category when only transfers lack a category" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"},
                {"id": "a2", "name": "Savings", "currency": "USD"}
              ],
              "categories": [],
              "transactions": [
                {
                  "id": "t1",
                  "type": "TRANSFER",
                  "amount": 500.0,
                  "accountId": "a1",
                  "toAccountId": "a2",
                  "toAmount": 500.0,
                  "title": "Sweep",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.categories shouldBe 0
        database.categoryQueries.selectAll().awaitAsList() shouldBe emptyList()
        EntryDao(database.entryQueries).getAll().first().map { it.category } shouldBe listOf(null)
    }

    "reuses an existing 'Imported Entries' category from the backup" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [
                {"id": "c1", "name": "Imported Entries"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "a1",
                  "title": "Untagged coffee",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.categories shouldBe 1
        database.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Imported Entries")
        EntryDao(database.entryQueries).getAll().first().map { it.category?.name } shouldBe listOf("Imported Entries")
    }

    "assigns entries with unknown category ids to the fallback category" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"},
                {"id": "a2", "name": "Savings", "currency": "USD"}
              ],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "a1",
                  "categoryId": "missing-category",
                  "title": "Dangling expense",
                  "dateTime": 946684800000
                },
                {
                  "id": "t2",
                  "type": "TRANSFER",
                  "amount": 500.0,
                  "accountId": "a1",
                  "toAccountId": "a2",
                  "toAmount": 500.0,
                  "categoryId": "missing-category",
                  "title": "Dangling transfer",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.categories shouldBe 2
        database.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Food", "Imported Entries")
        EntryDao(database.entryQueries).getAll().first().map { it.category?.name } shouldBe listOf("Imported Entries", "Imported Entries")
    }

    "assigns entries with unknown account ids to per-currency fallback accounts" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "missing-account",
                  "categoryId": "c1",
                  "currency": "USD",
                  "title": "Dangling USD expense",
                  "dateTime": 946684800000
                },
                {
                  "id": "t2",
                  "type": "EXPENSE",
                  "amount": 20.0,
                  "accountId": "missing-account",
                  "categoryId": "c1",
                  "currency": "USD",
                  "title": "Another dangling USD expense",
                  "dateTime": 946684800000
                },
                {
                  "id": "t3",
                  "type": "INCOME",
                  "amount": 100.0,
                  "accountId": "missing-account",
                  "categoryId": "c1",
                  "currency": "EUR",
                  "title": "Dangling EUR income",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.accounts shouldBe 3
        database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe
            listOf("Imported Account (EUR)", "Imported Account (USD)", "Wallet")
        EntryDao(database.entryQueries).getAll().first().map { it.account.name } shouldBe
            listOf("Imported Account (EUR)", "Imported Account (USD)", "Imported Account (USD)")
    }

    "assigns a transfer with an unknown toAccount to a fallback account" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [],
              "transactions": [
                {
                  "id": "t1",
                  "type": "TRANSFER",
                  "amount": 500.0,
                  "accountId": "a1",
                  "toAccountId": "missing-account",
                  "toAmount": 500.0,
                  "currency": "USD",
                  "title": "Dangling transfer",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.accounts shouldBe 2
        val transfer = EntryDao(database.entryQueries).getAll().first().single()
            .shouldBeInstanceOf<TransferEntry>()
        transfer.account.name shouldBe "Wallet"
        transfer.toAccount.name shouldBe "Imported Account (USD)"
        transfer.toCurrency shouldBe Currency.USD
    }

    "uses the default currency for fallback accounts when the entry has no currency" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "missing-account",
                  "categoryId": "c1",
                  "title": "Currency-less expense",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        val result = importer.import(ByteArrayInputStream(buildIvyZip(json)))

        result.accounts shouldBe 1
        val expense = EntryDao(database.entryQueries).getAll().first().single()
            .shouldBeInstanceOf<ExpenseEntry>()
        expense.account.name shouldBe "Imported Account (CHF)"
        expense.currency shouldBe Currency.CHF
    }

    "resets fallback category state between imports" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [
                {"id": "a1", "name": "Wallet", "currency": "USD"}
              ],
              "categories": [],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "a1",
                  "title": "Untagged coffee",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        importer.import(ByteArrayInputStream(buildIvyZip(json)))
        importer.import(ByteArrayInputStream(buildIvyZip(json)))

        database.categoryQueries.selectAll().awaitAsList().map { it.name } shouldBe listOf("Imported Entries")
        EntryDao(database.entryQueries).getAll().first().map { it.category?.name } shouldBe listOf("Imported Entries")
    }

    "resets fallback account state between imports" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val json = """
            {
              "accounts": [],
              "categories": [
                {"id": "c1", "name": "Food"}
              ],
              "transactions": [
                {
                  "id": "t1",
                  "type": "EXPENSE",
                  "amount": 10.0,
                  "accountId": "missing-account",
                  "categoryId": "c1",
                  "currency": "USD",
                  "title": "Dangling expense",
                  "dateTime": 946684800000
                }
              ]
            }
        """.trimIndent()

        importer.import(ByteArrayInputStream(buildIvyZip(json)))
        importer.import(ByteArrayInputStream(buildIvyZip(json)))

        database.accountQueries.selectAll().awaitAsList().map { it.name } shouldBe
            listOf("Imported Account (USD)")
        EntryDao(database.entryQueries).getAll().first().map { it.account.name } shouldBe
            listOf("Imported Account (USD)")
    }

    "import throws when the archive contains no JSON file" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        shouldThrow<IvyImportException> {
            importer.import(ByteArrayInputStream(buildIvyZipWithEntryNames("readme.txt", "data.dat", json = "{}")))
        }
    }

    "imports a UTF-8 backup" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val result = importer.import(ByteArrayInputStream(buildIvyZip(minimalIvyJson, charset = Charsets.UTF_8)))

        result.accounts shouldBe 3
        result.transactions shouldBe 5
    }

    "imports a UTF-16LE backup without a byte order mark" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val result = importer.import(ByteArrayInputStream(buildIvyZip(minimalIvyJson, charset = Charsets.UTF_16LE)))

        result.accounts shouldBe 3
        result.transactions shouldBe 5
    }

    "imports a UTF-16BE backup without a byte order mark" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val result = importer.import(ByteArrayInputStream(buildIvyZip(minimalIvyJson, charset = Charsets.UTF_16BE)))

        result.accounts shouldBe 3
        result.transactions shouldBe 5
    }

    "imports a UTF-16LE backup with a byte order mark" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val jsonBytes = minimalIvyJson.toByteArray(Charsets.UTF_16LE)
        val withBom = byteArrayOf(0xFF.toByte(), 0xFE.toByte()) + jsonBytes
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(withBom)
            zos.closeEntry()
        }

        val result = importer.import(ByteArrayInputStream(bos.toByteArray()))

        result.accounts shouldBe 3
    }

    "imports a UTF-16 backup with a byte order mark" {
        val database = lindenDatabase()
        val importer = IvyImporter(database)

        val jsonBytes = minimalIvyJson.toByteArray(Charsets.UTF_16BE)
        val withBom = byteArrayOf(0xFE.toByte(), 0xFF.toByte()) + jsonBytes
        val bos = ByteArrayOutputStream()
        ZipOutputStream(bos).use { zos ->
            zos.putNextEntry(ZipEntry("backup.json"))
            zos.write(withBom)
            zos.closeEntry()
        }

        val result = importer.import(ByteArrayInputStream(bos.toByteArray()))

        result.accounts shouldBe 3
    }
})
