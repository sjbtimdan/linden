package org.sjbtimdan.linden.ui.entry

/**
 * Formats minor units as a locale-aware amount with thousands grouping and two
 * decimal digits (e.g. 1_000_000 == "10,000.00" in en-US, "10.000,00" in de-DE).
 */
expect fun formatAmount(amount: Long): String

/**
 * Placeholder rendered in place of an amount while "Hide Totals" is on, shared
 * by every amount row and card so the mask reads identically app-wide.
 */
const val HIDDEN_AMOUNT = "••••••"

/**
 * Parses a user-typed amount ("42.50", "42,5", "1,000.00", "-500") into minor
 * units, or null when invalid; a leading "-" (or "+") is accepted, the former
 * yielding a negative result. Digits and separators are normalized first, so any
 * locale's numbering system parses: Arabic-Indic digits with "٫"/"٬", apostrophe
 * grouping ("1'234.50") and NBSP/NNBSP grouping included. The last "." or "," is
 * the decimal separator; earlier ones and spaces group the integer part. A
 * trailing separator with exactly 3 digits is grouping — minor units always have
 * 2 digits — so "1,000", "1.000" and "1 000" parse as 1000.
 */
fun parseAmount(input: String): Long? {
    val text = normalizeAmountCharacters(input.trim())
    if (text.isEmpty()) return null

    val negative = text.startsWith("-")
    val magnitudeText = if (text.startsWith("-") || text.startsWith("+")) text.substring(1) else text
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
            ". ,\u00A0\u202F'\u2019"
        } else if (magnitudeText[decimalIndex] == '.') {
            ", \u00A0\u202F'\u2019"
        } else {
            ". \u00A0\u202F'\u2019"
        }
    if (!separatorIsGrouping && decimalIndex != -1 && integerPart.contains(magnitudeText[decimalIndex])) return null
    if (integerPart.any { it !in groupingChars && !it.isDigit() }) return null

    val digits = integerPart + if (separatorIsGrouping) fractionPart else ""
    val major = digits.filter { it !in groupingChars }.ifEmpty { "0" }.toLongOrNull() ?: return null
    val minor = if (separatorIsGrouping) 0L else fractionPart.padEnd(2, '0').toLongOrNull() ?: return null
    val magnitude = major * 100 + minor
    return if (negative) -magnitude else magnitude
}

private fun normalizeAmountCharacters(text: String): String = buildString(text.length) {
    text.forEach { ch ->
        val digit = ch.digitToIntOrNull()
        when {
            digit != null -> append('0' + digit)
            ch == '\u066B' -> append('.')
            ch == '\u066C' -> append(',')
            ch == '\u2212' -> append('-')
            else -> append(ch)
        }
    }
}

private const val AMOUNT_SEPARATORS = ". ,\u00A0\u202F\u066B\u066C'\u2019"
private const val AMOUNT_SIGNS = "-+\u2212"

/**
 * Keeps only characters that can appear in an amount: any locale's decimal
 * digits, the dot and comma separators, space/NBSP/NNBSP and apostrophe grouping, the
 * Arabic "٫"/"٬" separators and the `-`/`+`/`−` signs. Applied as the user types
 * or pastes, so invalid characters (letters, emoji, currency symbols) never
 * reach Save; whether the remaining text is a valid amount is still up to
 * [parseAmount].
 */
fun filterAmountInput(input: String): String =
    input.filter { it.isDigit() || it in AMOUNT_SEPARATORS || it in AMOUNT_SIGNS }

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
