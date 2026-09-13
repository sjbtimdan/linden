package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import org.sjbtimdan.linden.AccountEntity
import org.sjbtimdan.linden.AccountQueries
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.util.asListFlow

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

    fun getAll(): Flow<List<Account>> = queries.selectAll().asListFlow { it.toAccount() }

    private fun AccountEntity.toAccount() = Account(
        id = id,
        name = name,
        currency = Currency.fromCode(currency),
        initialBalance = initialBalance,
        hidden = hidden != 0L,
    )
}
