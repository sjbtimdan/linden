package org.sjbtimdan.linden.model

import kotlinx.datetime.TimeZone
import kotlin.time.Instant

enum class EntryType {
    Income,
    Transfer,
    Expense,
}

sealed interface Entry {
    val type: EntryType
    val id: Long
    val category: Category?
    val description: String?
    val account: Account
    val amount: Long
    val createdAt: Instant
    val createdZone: TimeZone
}

data class ExpenseEntry(
    override val id: Long,
    override val category: Category,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    override val createdZone: TimeZone = TimeZone.UTC,
) : Entry {
    override val type: EntryType = EntryType.Expense
}

data class IncomeEntry(
    override val id: Long,
    override val category: Category,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    override val createdZone: TimeZone = TimeZone.UTC,
) : Entry {
    override val type: EntryType = EntryType.Income
}

data class TransferEntry(
    override val id: Long,
    override val category: Category?,
    override val description: String?,
    override val account: Account,
    override val amount: Long,
    override val createdAt: Instant = Instant.fromEpochMilliseconds(0),
    override val createdZone: TimeZone = TimeZone.UTC,
    val toAccount: Account,
    val toAmount: Long?,
) : Entry {
    override val type: EntryType = EntryType.Transfer
}
