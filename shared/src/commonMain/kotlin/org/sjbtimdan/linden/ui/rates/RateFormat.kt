package org.sjbtimdan.linden.ui.rates

import org.sjbtimdan.linden.model.Currency
import kotlin.math.roundToLong

internal fun formatRate(rate: Double): String {
    val rounded = (rate * 10_000).roundToLong() / 10_000.0
    return if (rounded % 1.0 == 0.0) rounded.toLong().toString() else rounded.toString()
}

internal fun parseRate(input: String): Double? {
    val value = input.trim().replace(',', '.').toDoubleOrNull() ?: return null
    return if (value.isFinite() && value > 0) value else null
}

internal fun rateRowLabel(base: Currency, quote: Currency, rate: Double?): String = if (rate == null) {
    "1 ${base.symbol} = —"
} else {
    "1 ${base.symbol} = ${formatRate(rate)} ${quote.symbol}"
}
