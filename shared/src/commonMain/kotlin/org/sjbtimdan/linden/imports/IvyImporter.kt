package org.sjbtimdan.linden.imports

import app.cash.sqldelight.async.coroutines.awaitAsOne
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream
import kotlin.math.roundToLong
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

data class IvyImportResult(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
)

class IvyImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

private const val FALLBACK_CATEGORY_NAME = "Imported Entries"

class IvyImporter(private val database: LindenDatabase) {
    private val json = Json { ignoreUnknownKeys = true }
    private var fallbackCategoryId: Long? = null
    private var fallbackCategoryCreated = false
    private val fallbackAccounts = mutableMapOf<Currency, ResolvedAccount>()

    suspend fun import(input: InputStream): IvyImportResult {
        val backup = decodeBackup(input)
        val defaultCurrency = SettingsDao(database.settingsQueries).getDefaultCurrency()
        fallbackCategoryId = null
        fallbackCategoryCreated = false
        fallbackAccounts.clear()
        val transactions = backup.transactions.filter { it.dateTime != null }
        database.transaction {
            database.entryQueries.deleteAll()
            database.categoryQueries.deleteAll()
            database.accountQueries.deleteAll()

            val accounts = backup.accounts.associate { account ->
                account.id to insertAccount(account)
            }
            val categories = backup.categories.associate { category ->
                category.id to insertCategory(category.name)
            }

            transactions.forEach { transaction ->
                insertTransaction(transaction, accounts, categories, backup.categories, defaultCurrency)
            }
        }
        return IvyImportResult(
            accounts = backup.accounts.size + fallbackAccounts.size,
            categories = backup.categories.size + if (fallbackCategoryCreated) 1 else 0,
            transactions = transactions.size,
        )
    }

    private fun decodeBackup(input: InputStream): IvyBackup {
        val zip = ZipInputStream(BufferedInputStream(input))
        return try {
            zip.use { zis ->
                generateSequence { zis.nextEntry }
                    .firstOrNull { it.name.endsWith(".json") }
                    ?: throw IvyImportException("The backup archive does not contain a JSON file")
                val bytes = zis.readBytes()
                json.decodeFromString(String(bytes, detectCharset(bytes)))
            }
        } catch (e: SerializationException) {
            throw IvyImportException("The backup is not a valid Ivy backup: ${e.message}", e)
        } catch (e: IOException) {
            throw IvyImportException("Failed to read the backup file: ${e.message}", e)
        }
    }

    private fun detectCharset(bytes: ByteArray): Charset = when {
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16
        bytes.size >= 2 && bytes[0] == 0x00.toByte() -> Charsets.UTF_16BE
        bytes.size >= 2 && bytes[1] == 0x00.toByte() -> Charsets.UTF_16LE
        else -> Charsets.UTF_8
    }

    private suspend fun insertAccount(account: IvyAccount): ResolvedAccount {
        val currency = parseCurrency(account.currency) {
            "Account \"${account.name}\" has unknown currency \"${account.currency}\""
        }
        database.accountQueries.insert(account.name, currency.name)
        val id = database.importQueries.lastInsertId().awaitAsOne()
        return ResolvedAccount(id, currency)
    }

    private suspend fun insertCategory(name: String): Long {
        database.categoryQueries.insert(name, CategoryType.Both.name)
        return database.importQueries.lastInsertId().awaitAsOne()
    }

    private suspend fun fallbackAccount(currency: Currency): ResolvedAccount {
        fallbackAccounts[currency]?.let { return it }
        database.accountQueries.insert("Imported Account (${currency.name})", currency.name)
        val id = database.importQueries.lastInsertId().awaitAsOne()
        return ResolvedAccount(id, currency).also { fallbackAccounts[currency] = it }
    }

    private suspend fun insertTransaction(
        transaction: IvyTransaction,
        accounts: Map<String, ResolvedAccount>,
        categories: Map<String, Long>,
        backupCategories: List<IvyCategory>,
        defaultCurrency: Currency,
    ) {
        val label = "\"${transaction.title}\""
        val type = when (transaction.type) {
            "EXPENSE" -> EntryType.Expense
            "INCOME" -> EntryType.Income
            "TRANSFER" -> EntryType.Transfer
            else -> throw IvyImportException("Transaction $label has unknown type \"${transaction.type}\"")
        }
        val entryCurrency = transaction.currency?.let { code ->
            parseCurrency(code) { "Transaction $label has unknown currency \"$code\"" }
        }
        val account = accounts[transaction.accountId]
            ?: fallbackAccount(entryCurrency ?: defaultCurrency)

        val categoryId = when (type) {
            EntryType.Expense, EntryType.Income -> categoryId(transaction, categories, backupCategories, required = true)
            EntryType.Transfer -> categoryId(transaction, categories, backupCategories, required = false)
        }

        val toAccount = if (type == EntryType.Transfer) {
            val id = transaction.toAccountId
                ?: throw IvyImportException("Transfer $label has no toAccountId")
            val toCurrencyOverride = transaction.toCurrency?.let { code ->
                parseCurrency(code) { "Transfer $label has unknown currency \"$code\"" }
            }
            accounts[id] ?: fallbackAccount(toCurrencyOverride ?: account.currency)
        } else {
            null
        }
        val toAmount = if (type == EntryType.Transfer) {
            val toAccountValue = toAccount!!
            if (account.currency == toAccountValue.currency) {
                null
            } else {
                transaction.toAmount?.let(::toMinorUnits)
                    ?: throw IvyImportException("Transfer $label has no toAmount")
            }
        } else {
            null
        }

        database.entryQueries.insert(
            type = type.name,
            categoryId = categoryId,
            description = transaction.title,
            accountId = account.id,
            amount = toMinorUnits(transaction.amount),
            toAccountId = toAccount?.id,
            toAmount = toAmount,
            createdAt = (transaction.dateTime?.let(Instant::fromEpochMilliseconds) ?: Clock.System.now())
                .toEpochMilliseconds(),
            createdZone = TimeZone.currentSystemDefault().id,
        )
    }

    private suspend fun categoryId(
        transaction: IvyTransaction,
        categories: Map<String, Long>,
        backupCategories: List<IvyCategory>,
        required: Boolean,
    ): Long? {
        val id = transaction.categoryId
        if (id != null && id in categories) return categories.getValue(id)
        return if (id != null || required) {
            resolveFallbackCategory(categories, backupCategories)
        } else {
            null
        }
    }

    private suspend fun resolveFallbackCategory(
        categories: Map<String, Long>,
        backupCategories: List<IvyCategory>,
    ): Long {
        fallbackCategoryId?.let { return it }
        val id = resolveBackupCategory(categories, backupCategories) ?: run {
            fallbackCategoryCreated = true
            insertCategory(FALLBACK_CATEGORY_NAME)
        }
        fallbackCategoryId = id
        return id
    }

    private fun resolveBackupCategory(categories: Map<String, Long>, backupCategories: List<IvyCategory>): Long? =
        backupCategories.firstOrNull { it.name == FALLBACK_CATEGORY_NAME }?.let { categories[it.id] }

    private inline fun parseCurrency(code: String, message: () -> String): Currency =
        Currency.entries.firstOrNull { it.name == code } ?: throw IvyImportException(message())

    private fun toMinorUnits(amount: Double): Long = (amount * 100).roundToLong()

    private data class ResolvedAccount(val id: Long, val currency: Currency)
}

@Serializable
private data class IvyBackup(
    val accounts: List<IvyAccount> = emptyList(),
    val categories: List<IvyCategory> = emptyList(),
    val transactions: List<IvyTransaction> = emptyList(),
)

@Serializable
private data class IvyAccount(
    val id: String,
    val name: String,
    val currency: String,
)

@Serializable
private data class IvyCategory(
    val id: String,
    val name: String,
)

@Serializable
private data class IvyTransaction(
    val id: String,
    val type: String,
    val amount: Double,
    val accountId: String,
    val categoryId: String? = null,
    val title: String? = null,
    val toAccountId: String? = null,
    val toAmount: Double? = null,
    val currency: String? = null,
    val toCurrency: String? = null,
    val dateTime: Long? = null,
)
