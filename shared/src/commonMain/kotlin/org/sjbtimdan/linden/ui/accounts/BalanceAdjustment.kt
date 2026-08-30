package org.sjbtimdan.linden.ui.accounts

import kotlinx.datetime.TimeZone
import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ExpenseEntry
import org.sjbtimdan.linden.model.IncomeEntry
import kotlin.time.Clock
import kotlin.time.Instant

/**
 * The result of reconciling an account's current balance against a target.
 * A positive [delta] means the account is short (an income adjustment is needed
 * to reach the target); a negative [delta] means it is over (an expense).
 */
data class BalanceAdjustment(
    val currentBalance: Long,
    val targetBalance: Long,
    val delta: Long,
) {
    val isZero: Boolean get() = delta == 0L
}

fun balanceAdjustment(currentBalance: Long, targetBalance: Long): BalanceAdjustment = BalanceAdjustment(
    currentBalance = currentBalance,
    targetBalance = targetBalance,
    delta = targetBalance - currentBalance,
)

/**
 * Builds the entry that records [adjustment] against [account] in the current
 * month (dated now). A positive delta becomes an income entry, a negative delta
 * an expense entry; the amount is always the absolute delta. Returns null when
 * there is nothing to adjust.
 */
fun adjustmentEntry(
    adjustment: BalanceAdjustment,
    account: Account,
    category: Category,
    now: Instant = Clock.System.now(),
    zone: TimeZone = TimeZone.currentSystemDefault(),
): Entry? {
    if (adjustment.isZero) return null
    val amount = kotlin.math.abs(adjustment.delta)
    return if (adjustment.delta > 0) {
        IncomeEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = amount,
            createdAt = now,
            createdZone = zone,
        )
    } else {
        ExpenseEntry(
            id = 0,
            category = category,
            description = null,
            account = account,
            amount = amount,
            createdAt = now,
            createdZone = zone,
        )
    }
}
