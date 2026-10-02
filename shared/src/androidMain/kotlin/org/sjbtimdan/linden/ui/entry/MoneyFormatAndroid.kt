package org.sjbtimdan.linden.ui.entry

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

actual fun formatAmount(amount: Long, decimalDigits: Int): String {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val locale = Locale.getDefault()
    if (decimalDigits == 0) {
        // Zero-decimal currencies have no sub-unit to show: round half-up.
        val rounded = absolute / 100 + if (absolute % 100 >= 50) 1 else 0
        if (rounded == 0L) return "0"
        val major = NumberFormat.getIntegerInstance(locale).format(rounded)
        return if (negative) "-$major" else major
    }
    val major = NumberFormat.getIntegerInstance(locale).format(absolute / 100)
    val text = "$major${DecimalFormatSymbols(locale).decimalSeparator}${(absolute % 100).toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}
