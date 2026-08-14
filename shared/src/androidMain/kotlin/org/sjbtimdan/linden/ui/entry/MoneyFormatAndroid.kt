package org.sjbtimdan.linden.ui.entry

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

actual fun formatAmount(amount: Long): String {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val locale = Locale.getDefault()
    val major = NumberFormat.getIntegerInstance(locale).format(absolute / 100)
    val text = "$major${DecimalFormatSymbols(locale).decimalSeparator}${(absolute % 100).toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}
