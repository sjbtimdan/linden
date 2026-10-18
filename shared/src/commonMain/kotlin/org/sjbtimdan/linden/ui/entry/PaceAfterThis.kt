package org.sjbtimdan.linden.ui.entry

import org.sjbtimdan.linden.model.Account
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.ui.insights.PaceConfidence
import org.sjbtimdan.linden.ui.insights.PaceForecast
import org.sjbtimdan.linden.ui.money.ratesByQuote
import org.sjbtimdan.linden.ui.money.toDefaultMinor

/** Daily allowance left after the draft expense is included, in the default currency. */
data class PaceAfterThis(
    val dailyAllowanceMinor: Long,
    val currency: Currency,
)

/**
 * The Pace caption after the in-progress expense: what the daily allowance
 * becomes once [draft] is saved. Null when the forecast is too young, the
 * draft is not a new expense with a known account, or the amount cannot be
 * converted into [defaultCurrency].
 */
internal fun paceAfterThisFor(
    forecast: PaceForecast?,
    draft: EntryDraft?,
    accounts: List<Account>,
    defaultCurrency: Currency,
    rates: List<FxRate>,
): PaceAfterThis? {
    if (forecast == null || forecast.confidence == PaceConfidence.Low) return null
    if (forecast.missingCurrencies.isNotEmpty()) return null
    if (draft == null || draft.editing != null || draft.type != EntryType.Expense) return null
    val amount = draft.amount ?: return null
    if (amount <= 0L) return null
    val account = accounts.firstOrNull { it.id == draft.accountId } ?: return null
    val converted = toDefaultMinor(
        amount = amount,
        from = account.currency,
        defaultCurrency = defaultCurrency,
        ratesByQuote = ratesByQuote(rates, defaultCurrency),
    ) ?: return null
    val remaining = (forecast.typicalMonthMinor - forecast.spentMinor - converted).coerceAtLeast(0L)
    return PaceAfterThis(
        dailyAllowanceMinor = remaining / forecast.daysRemaining,
        currency = defaultCurrency,
    )
}
