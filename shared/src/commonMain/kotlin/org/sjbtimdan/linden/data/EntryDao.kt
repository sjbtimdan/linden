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
    suspend fun create(entry: Entry) {
        val transfer = entry as? TransferEntry
        queries.insert(
            type = entry.type.name,
            categoryId = entry.category?.id,
            description = entry.description,
            accountId = entry.account.id,
            amount = entry.amount,
            currency = entry.currency.name,
            toAccountId = transfer?.toAccount?.id,
            toAmount = transfer?.toAmount,
            toCurrency = transfer?.toCurrency?.name,
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
                description = requireNotNull(description) { "Income entry $id has no description" },
                account = account,
                amount = amount,
                currency = Currency.fromCode(currency),
            )

            EntryType.Expense -> ExpenseEntry(
                id = id,
                category = requireNotNull(category) { "Expense entry $id has no category" },
                description = requireNotNull(description) { "Expense entry $id has no description" },
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
