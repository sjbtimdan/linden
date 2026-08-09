package org.sjbtimdan.linden.model

enum class EntryType {
    Income,
    Transfer,
    Expense
}

sealed interface Entry {
    val type: EntryType
    val id: Long
    val category: Category?
    val description: String?
    val account: Account
    val amount: Long
    val currency: Currency
}

data class ExpenseEntry(
    override val id: Long,
    override val category: Category,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val currency: Currency,
) : Entry {
    override val type: EntryType = EntryType.Expense
}

data class IncomeEntry(
    override val id: Long,
    override val category: Category,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val currency: Currency,
) : Entry {
    override val type: EntryType = EntryType.Income
}

data class TransferEntry(
    override val id: Long,
    override val category: Category?,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val currency: Currency,
    val toAccount: Account,
    val toAmount: Long,
    val toCurrency: Currency,
) : Entry {
    override val type: EntryType = EntryType.Transfer
}
