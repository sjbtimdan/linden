package org.sjbtimdan.linden.ui.entry

import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.util.Locale

/**
 * [NumberFormat] and [DecimalFormatSymbols] are expensive to build and not
 * thread-safe, so cache one pair per thread, rebuilt only when the locale
 * changes. formatAmount runs on the main thread during composition, so this
 * turns a per-call allocation into an effective no-op after the first use.
 */
private class LocaleFormats(val locale: Locale, val integer: NumberFormat, val symbols: DecimalFormatSymbols)

private val localeFormats = ThreadLocal<LocaleFormats?>()

private fun formats(locale: Locale): LocaleFormats {
    localeFormats.get()?.let { if (it.locale == locale) return it }
    return LocaleFormats(locale, NumberFormat.getIntegerInstance(locale), DecimalFormatSymbols(locale))
        .also { localeFormats.set(it) }
}

actual fun formatAmount(amount: Long, decimalDigits: Int): String {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val formats = formats(Locale.getDefault())
    if (decimalDigits == 0) {
        // Zero-decimal currencies have no sub-unit to show: round half-up.
        val rounded = absolute / 100 + if (absolute % 100 >= 50) 1 else 0
        if (rounded == 0L) return "0"
        val major = formats.integer.format(rounded)
        return if (negative) "-$major" else major
    }
    val major = formats.integer.format(absolute / 100)
    val text = "$major${formats.symbols.decimalSeparator}${(absolute % 100).toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}
