package org.sjbtimdan.linden.imports

import kotlinx.datetime.TimeZone
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.io.readFirstJsonEntry
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.time.AppClock
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import kotlin.time.Instant

data class IvyImportResult(
    val accounts: Int,
    val categories: Int,
    val transactions: Int,
    val splitTransactions: Int = 0,
)

class IvyImportException(message: String, cause: Throwable? = null) : Exception(message, cause)

class IvyImporter(
    private val database: LindenDatabase,
    private val clock: AppClock,
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun import(input: InputStream): IvyImportResult {
        val backup = decodeBackup(input)
        if (backup.accounts.isEmpty() && backup.categories.isEmpty() && backup.transactions.isEmpty()) {
            throw IvyImportException("The backup is not a valid Ivy backup: it is empty")
        }
        val defaultCurrency = SettingsDao(database.settingsQueries).getDefaultCurrency()
        val transactions = backup.transactions.filter { it.dateTime != null }
        var importedTransactions = 0
        var createdCategories = 0
        lateinit var session: IvyImportSession
        database.transaction {
            database.entryQueries.deleteAll()
            database.categoryQueries.deleteAll()
            database.accountQueries.deleteAll()

            val accounts = backup.accounts.associate { account ->
                account.id to insertAccount(account)
            }
            val categoryTypes = inferCategoryTypes(backup.categories, transactions)
            val categories = backup.categories.filter {
                !it.name.equals(INITIAL_BALANCE_TITLE, ignoreCase = true)
            }.associate { category ->
                val icon = guessIcon(category.name)
                category.id to insertCategory(category.name, categoryTypes.getValue(category.id), icon)
            }
            createdCategories = categories.size
            session = IvyImportSession(
                database = database,
                accounts = accounts,
                categories = categories,
                backupCategories = backup.categories,
                defaultCurrency = defaultCurrency,
            )

            transactions.forEach { transaction ->
                val createdAt = (transaction.dateTime?.let(Instant::fromEpochMilliseconds) ?: clock.now())
                    .toEpochMilliseconds()
                val resolved = session.resolveTransaction(transaction, createdAt) ?: return@forEach
                writeTransaction(resolved)
                importedTransactions++
            }
        }
        return IvyImportResult(
            accounts = backup.accounts.size + session.fallbackAccountsCreated + session.splitAccountsCreated,
            categories = createdCategories + if (session.fallbackCategoryCreated) 1 else 0,
            transactions = importedTransactions,
            splitTransactions = session.splitTransactions,
        )
    }

    private suspend fun writeTransaction(transaction: ResolvedTransaction) {
        database.entryQueries.insert(
            type = transaction.type.name,
            categoryId = transaction.categoryId,
            description = transaction.description,
            accountId = transaction.account.id,
            amount = transaction.amount,
            toAccountId = transaction.toAccount?.id,
            toAmount = transaction.toAmount,
            createdAt = transaction.createdAt,
            createdZone = TimeZone.currentSystemDefault().id,
        )
    }

    private fun decodeBackup(input: InputStream): IvyBackup = try {
        val text = readFirstJsonEntry(input) { bytes -> String(bytes, detectCharset(bytes)) }
            ?: throw IvyImportException("The backup archive does not contain a JSON file")
        json.decodeFromString(text)
    } catch (e: SerializationException) {
        throw IvyImportException("The backup is not a valid Ivy backup: ${e.message}", e)
    } catch (e: IOException) {
        throw IvyImportException("Failed to read the backup file: ${e.message}", e)
    }

    private fun detectCharset(bytes: ByteArray): Charset = when {
        bytes.size >= 2 && bytes[0] == 0xFE.toByte() && bytes[1] == 0xFF.toByte() -> Charsets.UTF_16
        bytes.size >= 2 && bytes[0] == 0xFF.toByte() && bytes[1] == 0xFE.toByte() -> Charsets.UTF_16
        bytes.size >= 2 && bytes[0] == 0x00.toByte() -> Charsets.UTF_16BE
        bytes.size >= 2 && bytes[1] == 0x00.toByte() -> Charsets.UTF_16LE
        else -> Charsets.UTF_8
    }

    private suspend fun insertAccount(account: IvyAccount): ResolvedAccount {
        val currency = parseIvyCurrency(account.currency) {
            "Account \"${account.name}\" has unknown currency \"${account.currency}\""
        }
        return insertResolvedAccount(database, account.name, currency)
    }

    private suspend fun insertCategory(name: String, type: CategoryType, icon: CategoryIcon? = null): Long =
        insertIvyCategory(database, name, type, icon)

    private fun guessIcon(name: String): CategoryIcon? {
        val lower = name.lowercase()
        return GUESS_ICON_RULES.firstOrNull { (keywords, _) -> keywords.any { lower.contains(it) } }?.second
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
}

/** Keyword-to-icon mapping for Ivy category name guessing. First match wins. */
private val GUESS_ICON_RULES: List<Pair<List<String>, CategoryIcon>> = listOf(
    listOf("food", "dining", "restaurant", "meal", "lunch", "dinner", "breakfast", "cafe", "coffee") to
        CategoryIcon.Restaurant,
    listOf("entertainment", "movie", "cinema", "game", "gaming", "fun", "hobby") to CategoryIcon.Movie,
    listOf("grocer", "supermarket", "market", "produce") to CategoryIcon.ShoppingCart,
    listOf("bank", "finance", "interest", "fee", "charge", "credit") to CategoryIcon.AccountBalance,
    listOf("salary", "wage", "income", "earning", "dividend", "invest") to CategoryIcon.Savings,
    listOf("shop", "retail", "store", "purchase", "clothes", "clothing", "fashion") to CategoryIcon.ShoppingBag,
    listOf("home", "rent", "mortgage", "utility", "utilities", "electric", "water", "internet", "house") to
        CategoryIcon.Home,
    listOf(
        "health",
        "medical",
        "doctor",
        "hospital",
        "pharmacy",
        "medicine",
        "dental",
        "insurance",
    ) to CategoryIcon.LocalHospital,
    listOf("gift", "donation", "charity", "present") to CategoryIcon.FavoriteBorder,
    listOf("pet", "dog", "cat", "animal", "vet") to CategoryIcon.Pets,
    listOf("education", "school", "university", "tuition", "book", "course", "training") to CategoryIcon.School,
    listOf("travel", "flight", "hotel", "vacation", "trip", "holiday", "airbnb", "airline") to CategoryIcon.Flight,
    listOf("gym", "fitness", "sport", "exercise", "health club", "yoga") to CategoryIcon.Spa,
    listOf("personal", "care", "beauty", "salon", "spa", "haircut") to CategoryIcon.Spa,
)
