package org.sjbtimdan.linden.ui.entry

/**
 * All supported currencies use a 2-decimal minor unit, so amounts are stored as
 * `Long` in minor units (e.g. 4250 == "42.50").
 */

/**
 * Formats minor units as a locale-aware amount with thousands grouping and two
 * decimal digits (e.g. 1_000_000 == "10,000.00" in en-US, "10.000,00" in de-DE).
 */
expect fun formatAmount(amount: Long): String

/**
 * Parses a user-typed amount (e.g. "42.50", "42,5", "1,000.00") into minor units.
 * The last `.` or `,` is the decimal separator; other occurrences of `.`/`,` and
 * spaces in the integer part are treated as grouping separators.
 * Returns null when the input is not a valid non-negative amount.
 */
fun parseAmount(input: String): Long? {
    val text = input.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("-") || text.startsWith("+")) return null

    val decimalIndex = maxOf(text.lastIndexOf('.'), text.lastIndexOf(','))
    val integerPart = if (decimalIndex == -1) text else text.substring(0, decimalIndex)
    val fractionPart = if (decimalIndex == -1) "" else text.substring(decimalIndex + 1)
    if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
    if (fractionPart.any { !it.isDigit() }) return null
    if (fractionPart.length > 2) return null

    val groupingChars =
        if (decimalIndex == -1) "" else if (text[decimalIndex] == '.') ", \u00A0\u202F" else ". \u00A0\u202F"
    if (decimalIndex != -1 && integerPart.contains(text[decimalIndex])) return null
    if (integerPart.any { it !in groupingChars && !it.isDigit() }) return null

    val major = integerPart.filter { it !in groupingChars }.ifEmpty { "0" }.toLongOrNull() ?: return null
    val minor = fractionPart.padEnd(2, '0').toLongOrNull() ?: return null
    return major * 100 + minor
}
