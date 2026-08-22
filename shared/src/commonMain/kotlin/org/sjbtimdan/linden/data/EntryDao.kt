package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOneOrNull
import app.cash.sqldelight.coroutines.asFlow
import kotlin.time.Instant
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.EntryQueries
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry

class EntryDao(private val queries: EntryQueries) {
    suspend fun create(entry: Entry) {
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
    }

    suspend fun update(entry: Entry) {
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

    suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    fun getAll(): Flow<List<Entry>> =
        queries.selectAll(::toEntry).asFlow().map { it.awaitAsList() }

    fun getSince(epochMs: Long): Flow<List<Entry>> =
        queries.selectSince(epochMs, ::toEntry).asFlow().map { it.awaitAsList() }

    fun getUpTo(epochMs: Long): Flow<List<Entry>> =
        queries.selectUpTo(epochMs, ::toEntry).asFlow().map { it.awaitAsList() }

    suspend fun latest(type: EntryType): Entry? =
        queries.selectLatestByType(type.name, ::toEntry).awaitAsOneOrNull()

    /** Net change per account in the account's own currency (minor units). */
    fun accountDeltas(): Flow<Map<Long, Long>> =
        queries.accountDeltas().asFlow().map { rows ->
            rows.awaitAsList()
                .mapNotNull { row -> row.accountId?.let { id -> id to row.delta } }
                .toMap()
        }

    /** Accounts referenced by at least one entry, as source or transfer target. */
    fun accountsWithEntries(): Flow<Set<Long>> =
        queries.accountsWithEntries().asFlow().map { rows ->
            rows.awaitAsList().toSet()
        }

    /** Net total per category and entry currency (minor units). */
    fun categoryTotals(): Flow<Map<Pair<Long, Currency>, Long>> =
        queries.categoryTotals().asFlow().map { rows ->
            rows.awaitAsList()
                .mapNotNull { row ->
                    row.categoryId?.let { id -> (id to Currency.fromCode(row.currency)) to row.net }
                }
                .toMap()
        }

    private fun Entry.sqlArgs(): SqlArgs =
        when (this) {
            is ExpenseEntry -> SqlArgs(category.id, null, null)
            is IncomeEntry -> SqlArgs(category.id, null, null)
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
        toAccountName: String?,
        toAccountCurrency: String?,
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
                    currency = Currency.fromCode(requireNotNull(toAccountCurrency) { "Transfer entry $id has no toAccount currency" }),
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
