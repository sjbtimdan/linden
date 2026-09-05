package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.AccountEntity
import org.sjbtimdan.linden.AccountQueries
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency

class AccountDao(private val queries: AccountQueries) {
    suspend fun create(name: String, currency: Currency, initialBalance: Long = 0) {
        queries.insert(name, currency.name, initialBalance)
    }

    suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    suspend fun update(account: Account) {
        queries.update(account.name, account.currency.name, account.initialBalance, account.id)
    }

    suspend fun setHidden(id: Long, hidden: Boolean) {
        queries.updateHidden(if (hidden) 1 else 0, id)
    }

    fun getAll(): Flow<List<Account>> = queries.selectAll()
        .asFlow()
        .map { it.awaitAsList().map { row -> row.toAccount() } }

    private fun AccountEntity.toAccount() = Account(
        id = id,
        name = name,
        currency = Currency.fromCode(currency),
        initialBalance = initialBalance,
        hidden = hidden != 0L,
    )
}
