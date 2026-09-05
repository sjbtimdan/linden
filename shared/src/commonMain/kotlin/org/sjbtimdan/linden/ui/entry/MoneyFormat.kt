package org.sjbtimdan.linden.ui.entry

/**
 * Formats minor units as a locale-aware amount with thousands grouping and two
 * decimal digits (e.g. 1_000_000 == "10,000.00" in en-US, "10.000,00" in de-DE).
 */
expect fun formatAmount(amount: Long): String

/**
 * Parses a user-typed amount ("42.50", "42,5", "1,000.00", "-500") into minor
 * units, or null when invalid; a leading "-" yields a negative result. The last
 * "." or "," is the decimal separator; earlier ones and spaces group the
 * integer part. A trailing separator with exactly 3 digits is grouping — minor
 * units always have 2 digits — so "1,000", "1.000" and "1 000" parse as 1000.
 */
fun parseAmount(input: String): Long? {
    val text = input.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("+")) return null

    val negative = text.startsWith("-")
    val magnitudeText = if (negative) text.substring(1) else text
    if (magnitudeText.isEmpty()) return null

    val decimalIndex = maxOf(magnitudeText.lastIndexOf('.'), magnitudeText.lastIndexOf(','))
    val integerPart = if (decimalIndex == -1) magnitudeText else magnitudeText.substring(0, decimalIndex)
    val fractionPart = if (decimalIndex == -1) "" else magnitudeText.substring(decimalIndex + 1)
    if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
    if (fractionPart.any { !it.isDigit() }) return null

    val separatorIsGrouping = decimalIndex != -1 && fractionPart.length == 3 && integerPart.isNotEmpty()
    if (!separatorIsGrouping && fractionPart.length > 2) return null

    val groupingChars =
        if (separatorIsGrouping || decimalIndex == -1) {
            ". ,\u00A0\u202F"
        } else if (magnitudeText[decimalIndex] == '.') {
            ", \u00A0\u202F"
        } else {
            ". \u00A0\u202F"
        }
    if (!separatorIsGrouping && decimalIndex != -1 && integerPart.contains(magnitudeText[decimalIndex])) return null
    if (integerPart.any { it !in groupingChars && !it.isDigit() }) return null

    val digits = integerPart + if (separatorIsGrouping) fractionPart else ""
    val major = digits.filter { it !in groupingChars }.ifEmpty { "0" }.toLongOrNull() ?: return null
    val minor = if (separatorIsGrouping) 0L else fractionPart.padEnd(2, '0').toLongOrNull() ?: return null
    val magnitude = major * 100 + minor
    return if (negative) -magnitude else magnitude
}

private const val MILLION_MINOR = 100_000_000L
private const val BILLION_MINOR = 100_000_000_000L

/**
 * Formats [amount] (minor units) compactly when it is at least one million:
 * 100_000_000 -> "1m", 125_000_000 -> "1.25m", 123_456_789_000 -> "1.235b",
 * with trailing zeros trimmed and "." as the decimal separator. Smaller
 * amounts fall back to [formatAmount]. Display-only: not parseable by
 * [parseAmount], so never pre-fill an edit field with it.
 */
fun formatAmountCompact(amount: Long): String {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val text = when {
        absolute >= BILLION_MINOR -> compact(absolute, BILLION_MINOR, "b")
        absolute >= MILLION_MINOR -> compact(absolute, MILLION_MINOR, "m")
        else -> return formatAmount(amount)
    }
    return if (negative) "-$text" else text
}

private fun compact(absolute: Long, unitMinor: Long, suffix: String): String {
    var whole = absolute / unitMinor
    var decimals = (absolute % unitMinor * 1000 + unitMinor / 2) / unitMinor
    if (decimals == 1000L) {
        whole++
        decimals = 0
    }
    // Pad fractional digits to three so "1.059m" can't render as "1.59m",
    // then trim trailing zeros ("1.250m" -> "1.25m").
    return if (decimals == 0L) "$whole$suffix" else "$whole.${decimals.toString().padStart(3, '0').trimEnd('0')}$suffix"
}
