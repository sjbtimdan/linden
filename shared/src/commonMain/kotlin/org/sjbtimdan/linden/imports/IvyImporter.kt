package org.sjbtimdan.linden.imports

import app.cash.sqldelight.async.coroutines.awaitAsOne
import java.io.BufferedInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.zip.ZipInputStream
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.datetime.TimeZone
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
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
    private const val INITIAL_BALANCE_TITLE = "initial balance"
    private const val ADJUST_BALANCE_TITLE = "adjust balance"

class IvyImporter(private val database: LindenDatabase) {
    private val json = Json { ignoreUnknownKeys = true }
    private var fallbackCategoryId: Long? = null
    private var fallbackCategoryCreated = false
    private val fallbackAccounts = mutableMapOf<Currency, ResolvedAccount>()
    private val initialBalanceApplied = mutableSetOf<Long>()

    suspend fun import(input: InputStream): IvyImportResult {
        val backup = decodeBackup(input)
        val defaultCurrency = SettingsDao(database.settingsQueries).getDefaultCurrency()
        fallbackCategoryId = null
        fallbackCategoryCreated = false
        fallbackAccounts.clear()
        initialBalanceApplied.clear()
        val transactions = backup.transactions.filter { it.dateTime != null }
        var importedTransactions = 0
        database.transaction {
            database.entryQueries.deleteAll()
            database.categoryQueries.deleteAll()
            database.accountQueries.deleteAll()

            val accounts = backup.accounts.associate { account ->
                account.id to insertAccount(account)
            }
            val categoryTypes = inferCategoryTypes(backup.categories, transactions)
            val categories = backup.categories.associate { category ->
                category.id to insertCategory(category.name, categoryTypes.getValue(category.id))
            }

            transactions.forEach { transaction ->
                if (insertTransaction(transaction, accounts, categories, backup.categories, defaultCurrency)) {
                    importedTransactions++
                }
            }
        }
        return IvyImportResult(
            accounts = backup.accounts.size + fallbackAccounts.size,
            categories = backup.categories.size + if (fallbackCategoryCreated) 1 else 0,
            transactions = importedTransactions,
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
        database.accountQueries.insert(account.name, currency.name, 0)
        val id = database.importQueries.lastInsertId().awaitAsOne()
        return ResolvedAccount(id, currency)
    }

    private suspend fun insertCategory(name: String, type: CategoryType): Long {
        database.categoryQueries.insert(name, type.name)
        return database.importQueries.lastInsertId().awaitAsOne()
    }

    private fun inferCategoryTypes(
        backupCategories: List<IvyCategory>,
        transactions: List<IvyTransaction>,
    ): Map<String, CategoryType> {
        val usage = mutableMapOf<String, MutableSet<EntryType>>()
        for (transaction in transactions) {
            val categoryId = transaction.categoryId ?: continue
            val type = when (transaction.type) {
                "EXPENSE" -> EntryType.Expense
                "INCOME" -> EntryType.Income
                else -> continue
            }
            usage.getOrPut(categoryId) { mutableSetOf() }.add(type)
        }
        return backupCategories.associate { category ->
            category.id to when (val types = usage[category.id]) {
                null -> CategoryType.Both
                else -> when {
                    EntryType.Expense in types && EntryType.Income in types -> CategoryType.Both
                    EntryType.Expense in types -> CategoryType.Expense
                    else -> CategoryType.Income
                }
            }
        }
    }

    private suspend fun fallbackAccount(currency: Currency): ResolvedAccount {
        fallbackAccounts[currency]?.let { return it }
        database.accountQueries.insert("Imported Account (${currency.name})", currency.name, 0)
        val id = database.importQueries.lastInsertId().awaitAsOne()
        return ResolvedAccount(id, currency).also { fallbackAccounts[currency] = it }
    }

    private suspend fun insertTransaction(
        transaction: IvyTransaction,
        accounts: Map<String, ResolvedAccount>,
        categories: Map<String, Long>,
        backupCategories: List<IvyCategory>,
        defaultCurrency: Currency,
    ): Boolean {
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

        if (type != EntryType.Transfer &&
            (transaction.title.equals(INITIAL_BALANCE_TITLE, ignoreCase = true) ||
                transaction.title.equals(ADJUST_BALANCE_TITLE, ignoreCase = true))
        ) {
            if (initialBalanceApplied.add(account.id)) {
                val balance = transaction.amount
                database.accountQueries.updateInitialBalance(
                    if (type == EntryType.Expense) -balance else balance,
                    account.id,
                )
                return false
            }
        }

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
                transaction.toAmount
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
            amount = transaction.amount,
            toAccountId = toAccount?.id,
            toAmount = toAmount,
            createdAt = (transaction.dateTime?.let(Instant::fromEpochMilliseconds) ?: Clock.System.now())
                .toEpochMilliseconds(),
            createdZone = TimeZone.currentSystemDefault().id,
        )
        return true
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
            insertCategory(FALLBACK_CATEGORY_NAME, CategoryType.Both)
        }
        fallbackCategoryId = id
        return id
    }

    private fun resolveBackupCategory(categories: Map<String, Long>, backupCategories: List<IvyCategory>): Long? =
        backupCategories.firstOrNull { it.name == FALLBACK_CATEGORY_NAME }?.let { categories[it.id] }

    private inline fun parseCurrency(code: String, message: () -> String): Currency =
        Currency.entries.firstOrNull { it.name == code } ?: throw IvyImportException(message())

    private data class ResolvedAccount(val id: Long, val currency: Currency)
}

/**
 * Parses a decimal amount such as "45.50", "3200.0" or "-5.75" into minor units
 * without floating-point arithmetic. Amounts with more than two decimal places
 * are rounded to the nearest minor unit (half-up).
 */
internal fun parseMinorUnits(text: String): Long {
    val trimmed = text.trim()
    val negative = trimmed.startsWith("-")
    val unsigned = if (negative) trimmed.drop(1) else trimmed
    if (unsigned.isEmpty() || unsigned.any { it != '.' && !it.isDigit() }) {
        throw SerializationException("Invalid amount \"$text\"")
    }
    val parts = unsigned.split('.')
    if (parts.size > 2 || parts[0].isEmpty()) {
        throw SerializationException("Invalid amount \"$text\"")
    }
    val whole = parts[0].toLongOrNull()
        ?: throw SerializationException("Invalid amount \"$text\"")
    val fraction = parts.getOrNull(1).orEmpty()
    val minor = when {
        fraction.length <= 2 -> whole * 100 + fraction.padEnd(2, '0').toLong()
        else -> {
            val base = whole * 100 + fraction.take(2).toLong()
            if (fraction[2] >= '5') base + 1 else base
        }
    }
    return if (negative) -minor else minor
}

private object MinorUnitsSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IvyMinorUnits", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Ivy amounts require a JSON decoder")
        val content = jsonDecoder.decodeJsonElement().jsonPrimitive.contentOrNull
            ?: throw SerializationException("Amount is missing")
        return parseMinorUnits(content)
    }

    override fun serialize(encoder: Encoder, value: Long) {
        error("Ivy backups are only decoded, never serialized")
    }
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
    @Serializable(with = MinorUnitsSerializer::class)
    val amount: Long,
    val accountId: String,
    val categoryId: String? = null,
    val title: String? = null,
    val toAccountId: String? = null,
    @Serializable(with = MinorUnitsSerializer::class)
    val toAmount: Long? = null,
    val currency: String? = null,
    val toCurrency: String? = null,
    val dateTime: Long? = null,
)
