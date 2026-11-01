package org.sjbtimdan.linden.imports

import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType

internal const val FALLBACK_CATEGORY_NAME = "Imported Entries"
internal const val INITIAL_BALANCE_TITLE = "initial balance"
internal const val ADJUST_BALANCE_TITLE = "adjust balance"

internal data class ResolvedAccount(val id: Long, val name: String, val currency: Currency)

internal data class ResolvedTransaction(
    val type: EntryType,
    val categoryId: Long?,
    val account: ResolvedAccount,
    val amount: Long,
    val description: String?,
    val createdAt: Long,
    val toAccount: ResolvedAccount?,
    val toAmount: Long?,
)

private data class AccountResolution(val account: ResolvedAccount, val split: Boolean)

/**
 * Per-import resolution state: the backup account/category maps, fallback and
 * split caches, initial-balance bookkeeping and the split counters. Created
 * fresh for every import, so concurrent imports never share state.
 */
internal class IvyImportSession(
    private val database: LindenDatabase,
    private val accounts: Map<String, ResolvedAccount>,
    private val categories: Map<String, Long>,
    private val backupCategories: List<IvyCategory>,
    private val defaultCurrency: Currency,
) {
    private var fallbackCategoryId: Long? = null
    private val fallbackAccounts = mutableMapOf<Currency, ResolvedAccount>()
    private val splitAccounts = mutableMapOf<String, ResolvedAccount>()
    private val initialBalanceApplied = mutableSetOf<Long>()

    var fallbackAccountsCreated = 0
        private set

    var splitAccountsCreated = 0
        private set

    var splitTransactions = 0
        private set

    var fallbackCategoryCreated = false
        private set

    /**
     * Resolves one backup transaction to the entry that should be written, or
     * null when it is a balance entry consumed into an account's initial
     * balance. Throws [IvyImportException] for an unknown type or an invalid
     * transfer.
     */
    suspend fun resolveTransaction(transaction: IvyTransaction, createdAt: Long): ResolvedTransaction? {
        val label = "\"${transaction.title}\""
        val type = resolveType(transaction, label)
        val accountResolution = resolveAccount(
            label = label,
            what = "Transaction",
            accountId = transaction.accountId,
            currencyCode = transaction.currency,
            fallbackCurrency = defaultCurrency,
        )
        val account = accountResolution.account
        val categoryName = transaction.categoryId?.let { id ->
            backupCategories.firstOrNull { it.id == id }?.name
        }
        if (isBalanceEntry(type, transaction, categoryName)) {
            if (initialBalanceApplied.add(account.id)) {
                if (accountResolution.split) splitTransactions++
                val balance = transaction.amount
                database.accountQueries.updateInitialBalance(
                    if (type == EntryType.Expense) -balance else balance,
                    account.id,
                )
                return null
            }
        }

        var splitRouted = accountResolution.split
        val categoryId = when (type) {
            EntryType.Expense, EntryType.Income -> resolveCategory(transaction, required = true)
            EntryType.Transfer -> resolveCategory(transaction, required = false)
        }

        val (toAccount, toAmount) = when (type) {
            EntryType.Transfer -> {
                val targetId = transaction.toAccountId
                    ?: throw IvyImportException("Transfer $label has no toAccountId")
                val targetResolution = resolveAccount(
                    label = label,
                    what = "Transfer",
                    accountId = targetId,
                    currencyCode = transaction.toCurrency,
                    fallbackCurrency = account.currency,
                )
                splitRouted = splitRouted || targetResolution.split
                val target = targetResolution.account
                val targetAmount = if (account.currency == target.currency) {
                    null
                } else {
                    transaction.toAmount
                        ?: throw IvyImportException("Transfer $label has no toAmount")
                }
                target to targetAmount
            }

            EntryType.Expense, EntryType.Income -> null to null
        }

        if (splitRouted) splitTransactions++
        return ResolvedTransaction(
            type = type,
            categoryId = categoryId,
            account = account,
            amount = transaction.amount,
            description = transaction.title,
            createdAt = createdAt,
            toAccount = toAccount,
            toAmount = toAmount,
        )
    }

    private fun resolveType(transaction: IvyTransaction, label: String): EntryType = when (transaction.type) {
        "EXPENSE" -> EntryType.Expense
        "INCOME" -> EntryType.Income
        "TRANSFER" -> EntryType.Transfer
        else -> throw IvyImportException("Transaction $label has unknown type \"${transaction.type}\"")
    }

    private fun isBalanceEntry(type: EntryType, transaction: IvyTransaction, categoryName: String?): Boolean =
        type != EntryType.Transfer && (
            transaction.title.equals(INITIAL_BALANCE_TITLE, ignoreCase = true) ||
                transaction.title.equals(ADJUST_BALANCE_TITLE, ignoreCase = true) ||
                categoryName.equals(INITIAL_BALANCE_TITLE, ignoreCase = true)
            )

    private suspend fun resolveAccount(
        label: String,
        what: String,
        accountId: String,
        currencyCode: String?,
        fallbackCurrency: Currency,
    ): AccountResolution {
        val currency = currencyCode?.let { code ->
            parseIvyCurrency(code) { "$what $label has unknown currency \"$code\"" }
        }
        val account = accounts[accountId] ?: fallbackAccount(currency ?: fallbackCurrency)
        if (currency == null || currency == account.currency) return AccountResolution(account, false)
        return AccountResolution(splitAccount(account, currency), true)
    }

    private suspend fun fallbackAccount(currency: Currency): ResolvedAccount {
        fallbackAccounts[currency]?.let { return it }
        return insertResolvedAccount(database, "Imported Account (${currency.name})", currency)
            .also {
                fallbackAccounts[currency] = it
                fallbackAccountsCreated++
            }
    }

    private suspend fun splitAccount(account: ResolvedAccount, currency: Currency): ResolvedAccount {
        val name = "IVY: ${account.name} (${currency.name})"
        splitAccounts[name]?.let { return it }
        database.accountQueries.selectByName(name).awaitAsOneOrNull()?.let { existing ->
            val existingCurrency = parseIvyCurrency(existing.currency) {
                "Account \"$name\" has unknown currency \"${existing.currency}\""
            }
            return ResolvedAccount(existing.id, name, existingCurrency).also {
                splitAccounts[name] = it
            }
        }
        return insertResolvedAccount(database, name, currency).also {
            splitAccounts[name] = it
            splitAccountsCreated++
        }
    }

    private suspend fun resolveCategory(transaction: IvyTransaction, required: Boolean): Long? {
        val id = transaction.categoryId
        if (id != null && id in categories) return categories.getValue(id)
        return if (id != null || required) fallbackCategory() else null
    }

    private suspend fun fallbackCategory(): Long {
        fallbackCategoryId?.let { return it }
        val id = backupCategories.firstOrNull { it.name == FALLBACK_CATEGORY_NAME }?.let { categories[it.id] }
            ?: run {
                fallbackCategoryCreated = true
                insertIvyCategory(database, FALLBACK_CATEGORY_NAME, CategoryType.Both, icon = null)
            }
        fallbackCategoryId = id
        return id
    }
}

internal suspend fun insertResolvedAccount(
    database: LindenDatabase,
    name: String,
    currency: Currency,
): ResolvedAccount {
    database.accountQueries.insert(name, currency.name, 0)
    val id = database.importQueries.lastInsertId().awaitAsOne()
    return ResolvedAccount(id, name, currency)
}

internal suspend fun insertIvyCategory(
    database: LindenDatabase,
    name: String,
    type: CategoryType,
    icon: CategoryIcon?,
): Long {
    database.categoryQueries.insert(name, type.name, icon?.name)
    return database.importQueries.lastInsertId().awaitAsOne()
}

internal inline fun parseIvyCurrency(code: String, message: () -> String): Currency =
    Currency.entries.firstOrNull { it.name == code } ?: throw IvyImportException(message())
