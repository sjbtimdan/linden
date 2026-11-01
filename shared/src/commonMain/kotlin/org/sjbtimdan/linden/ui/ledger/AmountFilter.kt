package org.sjbtimdan.linden.ui.ledger

import kotlin.math.abs

/** How an amount filter compares an entry's amount against a target value. */
enum class AmountOperator {
    GreaterThan,
    LessThan,
    Approximately,
}

/** A filter on entry amounts: [operator] applied to [minor] (minor units). */
data class AmountFilter(
    val operator: AmountOperator,
    val minor: Long,
) {
    fun matches(amount: Long): Boolean = when (operator) {
        AmountOperator.GreaterThan -> amount > minor
        AmountOperator.LessThan -> amount < minor
        AmountOperator.Approximately -> abs(amount - minor) <= (minor * 5 + 50) / 100
    }
}
