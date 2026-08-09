package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.EntryQueries
import org.sjbtimdan.linden.SelectAll
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
    suspend fun create(entry: ExpenseEntry) {
        queries.insert(
            type = EntryType.Expense.name,
            categoryId = entry.category.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = null,
            toAmount = null,
            toCurrency = null,
        )
    }

    suspend fun create(entry: IncomeEntry) {
        queries.insert(
            type = EntryType.Income.name,
            categoryId = entry.category.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = null,
            toAmount = null,
            toCurrency = null,
        )
    }

    suspend fun create(entry: TransferEntry) {
        queries.insert(
            type = EntryType.Transfer.name,
            categoryId = entry.category?.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = entry.toAccount.id,
            toAmount = entry.toAmount,
            toCurrency = entry.toCurrency.name,
        )
    }

    suspend fun update(entry: ExpenseEntry) {
        queries.updateById(
            type = EntryType.Expense.name,
            categoryId = entry.category.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = null,
            toAmount = null,
            toCurrency = null,
            id = entry.id,
        )
    }

    suspend fun update(entry: IncomeEntry) {
        queries.updateById(
            type = EntryType.Income.name,
            categoryId = entry.category.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = null,
            toAmount = null,
            toCurrency = null,
            id = entry.id,
        )
    }

    suspend fun update(entry: TransferEntry) {
        queries.updateById(
            type = EntryType.Transfer.name,
            categoryId = entry.category?.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = entry.toAccount.id,
            toAmount = entry.toAmount,
            toCurrency = entry.toCurrency.name,
            id = entry.id,
        )
    }

    suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    fun getAll(): Flow<List<Entry>> {
        return queries.selectAll()
            .asFlow()
            .map { it.awaitAsList().map { row -> row.toEntry() } }
    }

    fun getExpenses(): Flow<List<ExpenseEntry>> = getAll().map { it.filterIsInstance<ExpenseEntry>() }

    fun getIncomes(): Flow<List<IncomeEntry>> = getAll().map { it.filterIsInstance<IncomeEntry>() }

    fun getTransfers(): Flow<List<TransferEntry>> = getAll().map { it.filterIsInstance<TransferEntry>() }

    private fun SelectAll.toEntry(): Entry {
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
                currency = Currency.fromCode(currency),
            )

            EntryType.Expense -> ExpenseEntry(
                id = id,
                category = requireNotNull(category) { "Expense entry $id has no category" },
                description = description,
                account = account,
                amount = amount,
                currency = Currency.fromCode(currency),
            )

            EntryType.Transfer -> TransferEntry(
                id = id,
                category = category,
                description = description,
                account = account,
                amount = amount,
                currency = Currency.fromCode(currency),
                toAccount = Account(
                    id = requireNotNull(toAccountId) { "Transfer entry $id has no toAccount" },
                    name = requireNotNull(toAccountName) { "Transfer entry $id has no toAccount name" },
                    currency = Currency.fromCode(requireNotNull(toAccountCurrency) { "Transfer entry $id has no toAccount currency" }),
                ),
                toAmount = requireNotNull(toAmount) { "Transfer entry $id has no toAmount" },
                toCurrency = Currency.fromCode(requireNotNull(toCurrency) { "Transfer entry $id has no toCurrency" }),
            )
        }
    }
}
