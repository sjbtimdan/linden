package org.sjbtimdan.linden.ui.entry

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import org.sjbtimdan.linden.model.TransferEntry
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * Mutable draft of an entry being created or edited in the entry dialog.
 * Validation and conversion are pure functions of the draft and the lookup
 * lists, so they are unit-tested directly.
 */
data class EntryDraft(
    val editing: Entry?,
    val type: EntryType,
    val amountText: String,
    val categoryId: Long?,
    val accountId: Long?,
    val toAccountId: Long?,
    val toAmountText: String,
    val description: String,
    val createdAt: Instant,
    val createdZone: TimeZone,
) {
    val amount: Long? get() = parseAmount(amountText)
    val toAmount: Long? get() = parseAmount(toAmountText)

    fun isValid(accounts: List<Account>): Boolean = firstMissingRequirement(accounts) == null

    /**
     * The first requirement this draft still misses, or null when the draft is
     * valid. Mirrors [isValid]: a requirement exists exactly when saving is
     * blocked. The wording is localized in the UI via [MissingRequirement.text].
     */
    fun firstMissingRequirement(accounts: List<Account>): MissingRequirement? {
        val amountValue = amount
        if (amountValue == null || amountValue <= 0) return MissingRequirement.AMOUNT
        if (accountId == null) {
            return when (type) {
                EntryType.Expense, EntryType.Income -> MissingRequirement.ACCOUNT
                EntryType.Transfer -> MissingRequirement.SOURCE_ACCOUNT
            }
        }
        return when (type) {
            EntryType.Expense, EntryType.Income ->
                if (categoryId == null) MissingRequirement.CATEGORY else null

            EntryType.Transfer -> {
                val toAccountIdValue = toAccountId ?: return MissingRequirement.DESTINATION_ACCOUNT
                if (toAccountIdValue == accountId) return MissingRequirement.DIFFERENT_DESTINATION
                val toAccount = accounts.firstOrNull { it.id == toAccountIdValue }
                    ?: return MissingRequirement.DESTINATION_ACCOUNT
                val account = accounts.firstOrNull { it.id == accountId }
                    ?: return MissingRequirement.SOURCE_ACCOUNT
                if (account.currency == toAccount.currency) {
                    null
                } else {
                    val toValue = toAmount
                    if (toValue == null || toValue <= 0) MissingRequirement.RECEIVED_AMOUNT else null
                }
            }
        }
    }

    fun toEntry(accounts: List<Account>, categories: List<Category>): Entry? {
        val amountValue = amount ?: return null
        val account = accounts.firstOrNull { it.id == accountId } ?: return null
        val id = editing?.id ?: 0L
        val description = description.trim().ifEmpty { null }
        return when (type) {
            EntryType.Expense -> {
                val category = categories.firstOrNull { it.id == categoryId } ?: return null
                ExpenseEntry(
                    id,
                    category,
                    description,
                    account,
                    amountValue,
                    createdAt = createdAt,
                    createdZone = createdZone,
                )
            }

            EntryType.Income -> {
                val category = categories.firstOrNull { it.id == categoryId } ?: return null
                IncomeEntry(
                    id,
                    category,
                    description,
                    account,
                    amountValue,
                    createdAt = createdAt,
                    createdZone = createdZone,
                )
            }

            EntryType.Transfer -> {
                val toAccount = accounts.firstOrNull { it.id == toAccountId } ?: return null
                val sameCurrency = account.currency == toAccount.currency
                val toValue = if (sameCurrency) null else toAmount ?: return null
                TransferEntry(
                    id = id,
                    category = categories.firstOrNull { it.id == categoryId },
                    description = description,
                    account = account,
                    amount = amountValue,
                    createdAt = createdAt,
                    createdZone = createdZone,
                    toAccount = toAccount,
                    toAmount = toValue,
                )
            }
        }
    }

    /**
     * Returns this draft with the type-independent fields (amount, description,
     * date & time) replaced by [previous]'s values. Used when switching the
     * entry type so already-entered common fields are preserved.
     */
    fun carryOverCommonFields(previous: EntryDraft): EntryDraft = copy(
        amountText = previous.amountText,
        description = previous.description,
        createdAt = previous.createdAt,
        createdZone = previous.createdZone,
    )

    /**
     * Returns a copy of this draft that creates a new entry: it drops the edited
     * entry and re-dates itself to [now] in [zone]. Every other field (type,
     * amount, category, accounts, description) carries over unchanged, so saving
     * the result duplicates the original row.
     */
    fun asNewEntry(now: Instant = Clock.System.now(), zone: TimeZone = TimeZone.currentSystemDefault()): EntryDraft =
        copy(
            editing = null,
            createdAt = now,
            createdZone = zone,
        )

    companion object {
        fun forNew(type: EntryType = EntryType.Expense, previous: Entry? = null): EntryDraft {
            val empty = EntryDraft(
                editing = null,
                type = type,
                amountText = "",
                categoryId = null,
                accountId = null,
                toAccountId = null,
                toAmountText = "",
                description = "",
                createdAt = Clock.System.now(),
                createdZone = TimeZone.currentSystemDefault(),
            )
            if (previous == null || previous.type != type) return empty
            return when (previous) {
                is TransferEntry -> empty.copy(
                    accountId = previous.account.id,
                    toAccountId = previous.toAccount.id,
                    description = previous.description.orEmpty(),
                )

                is ExpenseEntry, is IncomeEntry -> empty.copy(
                    categoryId = previous.category?.id,
                    accountId = previous.account.id,
                    description = previous.description.orEmpty(),
                )
            }
        }

        fun forEdit(entry: Entry): EntryDraft = when (entry) {
            is TransferEntry -> EntryDraft(
                editing = entry,
                type = EntryType.Transfer,
                amountText = formatAmount(entry.amount),
                categoryId = entry.category?.id,
                accountId = entry.account.id,
                toAccountId = entry.toAccount.id,
                toAmountText = formatAmount(entry.toAmount ?: entry.amount),
                description = entry.description.orEmpty(),
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )

            is ExpenseEntry, is IncomeEntry -> EntryDraft(
                editing = entry,
                type = entry.type,
                amountText = formatAmount(entry.amount),
                categoryId = entry.category?.id,
                accountId = entry.account.id,
                toAccountId = null,
                toAmountText = "",
                description = entry.description.orEmpty(),
                createdAt = entry.createdAt,
                createdZone = entry.createdZone,
            )
        }
    }
}

/**
 * Categories an entry of [type] may use: expense entries cannot use income-only
 * categories, income entries cannot use expense-only ones, transfers carry none.
 * Mirrors what the category picker offers, so validation and type switches agree
 * with the form.
 */
internal fun categoriesForType(categories: List<Category>, type: EntryType): List<Category> = when (type) {
    EntryType.Expense -> categories.filter { it.type != CategoryType.Income }
    EntryType.Income -> categories.filter { it.type != CategoryType.Expense }
    EntryType.Transfer -> emptyList()
}
