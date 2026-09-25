package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.EntryQueries
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import org.sjbtimdan.linden.util.asListFlow
import org.sjbtimdan.linden.util.asOneOrNullFlow
import kotlin.time.Instant

open class EntryDao(private val queries: EntryQueries) {
    /** Inserts [entry] and returns the generated id. */
    open suspend fun create(entry: Entry): Long {
        val args = entry.sqlArgs()
        queries.insert(
            type = entry.type.name,
            categoryId = args.categoryId,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            toAccountId = args.toAccountId,
            toAmount = args.toAmount,
            createdAt = entry.createdAt.toEpochMilliseconds(),
            createdZone = entry.createdZone.id,
        )
        return queries.lastInsertedId().awaitAsOne()
    }

    open suspend fun update(entry: Entry) {
        val args = entry.sqlArgs()
        queries.updateById(
            type = entry.type.name,
            categoryId = args.categoryId,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            toAccountId = args.toAccountId,
            toAmount = args.toAmount,
            createdAt = entry.createdAt.toEpochMilliseconds(),
            createdZone = entry.createdZone.id,
            id = entry.id,
        )
    }

    open suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    fun getAll(): Flow<List<Entry>> = queries.selectAll(::toEntry).asListFlow()

    fun getSince(epochMs: Long): Flow<List<Entry>> = queries.selectSince(epochMs, ::toEntry).asListFlow()

    fun getSinceByType(type: EntryType, epochMs: Long): Flow<List<Entry>> =
        queries.selectSinceByType(type.name, epochMs, ::toEntry).asListFlow()

    /** All entries of [type], without a date cutoff; used by quick entry. */
    fun getAllByType(type: EntryType): Flow<List<Entry>> = queries.selectAllByType(type.name, ::toEntry).asListFlow()

    fun getUpTo(epochMs: Long): Flow<List<Entry>> = queries.selectUpTo(epochMs, ::toEntry).asListFlow()

    fun entryExists(): Flow<Boolean> = queries.entryExists().asOneOrNullFlow().map { it != null }

    suspend fun latest(type: EntryType): Entry? = queries.selectLatestByType(type.name, ::toEntry).awaitAsOneOrNull()

    /** Categories used on entries in [accountId], most-used first (top 5). */
    suspend fun categoriesForAccount(accountId: Long): List<Category> =
        queries.selectCategoriesForAccount(accountId).awaitAsList().map { row ->
            Category(
                id = row.id,
                name = row.name,
                type = CategoryType.valueOf(row.type),
                icon = row.icon?.let { CategoryIcon.valueOf(it) },
            )
        }

    /** Accounts referenced by at least one entry, as source or transfer target. */
    fun accountsWithEntries(): Flow<Set<Long>> = queries.accountsWithEntries().asListFlow().map { it.toSet() }

    /** Categories referenced by at least one entry; they cannot be deleted while entries exist. */
    fun categoriesWithEntries(): Flow<Set<Long>> = queries.categoriesWithEntries().asListFlow().map { it.toSet() }

    private fun Entry.sqlArgs(): SqlArgs = when (this) {
        is ExpenseEntry, is IncomeEntry -> SqlArgs(category?.id, null, null)

        is TransferEntry -> SqlArgs(
            category?.id,
            toAccount.id,
            if (account.currency == toAccount.currency) null else toAmount,
        )
    }

    private fun toEntry(
        id: Long,
        type: String,
        categoryId: Long?,
        description: String?,
        accountId: Long,
        amount: Long,
        toAccountId: Long?,
        toAmount: Long?,
        createdAt: Long,
        createdZone: String,
        accountName: String,
        accountCurrency: String,
        categoryName: String?,
        categoryType: String?,
        categoryIcon: String?,
        toAccountName: String?,
        toAccountCurrency: String?,
        // Hidden flags are part of the shared EntryView row shape and are only
        // consumed by the SQL WHERE clauses of the suggestion reads.
        @Suppress("UNUSED_PARAMETER") accountHidden: Long,
        @Suppress("UNUSED_PARAMETER") toAccountHidden: Long?,
    ): Entry {
        val account = Account(
            id = accountId,
            name = accountName,
            currency = Currency.fromCode(accountCurrency),
        )
        val category = categoryId?.let {
            Category(
                id = it,
                name = requireNotNull(categoryName) { "Missing name for category $it" },
                type = CategoryType.valueOf(requireNotNull(categoryType) { "Missing type for category $it" }),
                icon = categoryIcon?.let { icon -> CategoryIcon.valueOf(icon) },
            )
        }
        return when (EntryType.valueOf(type)) {
            EntryType.Income -> IncomeEntry(
                id = id,
                category = requireNotNull(category) { "Income entry $id has no category" },
                description = description,
                account = account,
                amount = amount,
                createdAt = Instant.fromEpochMilliseconds(createdAt),
                createdZone = TimeZone.of(createdZone),
            )

            EntryType.Expense -> ExpenseEntry(
                id = id,
                category = requireNotNull(category) { "Expense entry $id has no category" },
                description = description,
                account = account,
                amount = amount,
                createdAt = Instant.fromEpochMilliseconds(createdAt),
                createdZone = TimeZone.of(createdZone),
            )

            EntryType.Transfer -> {
                val toAccount = Account(
                    id = requireNotNull(toAccountId) { "Transfer entry $id has no toAccount" },
                    name = requireNotNull(toAccountName) { "Transfer entry $id has no toAccount name" },
                    currency = Currency.fromCode(
                        requireNotNull(toAccountCurrency) { "Transfer entry $id has no toAccount currency" },
                    ),
                )
                TransferEntry(
                    id = id,
                    category = category,
                    description = description,
                    account = account,
                    amount = amount,
                    createdAt = Instant.fromEpochMilliseconds(createdAt),
                    createdZone = TimeZone.of(createdZone),
                    toAccount = toAccount,
                    toAmount = if (account.currency == toAccount.currency) null else toAmount,
                )
            }
        }
    }
}

private data class SqlArgs(
    val categoryId: Long?,
    val toAccountId: Long?,
    val toAmount: Long?,
)
