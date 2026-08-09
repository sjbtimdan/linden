package org.sjbtimdan.linden.ui.ledger

/**
 * All supported currencies use a 2-decimal minor unit, so amounts are stored as
 * `Long` in minor units (e.g. 4250 == "42.50").
 */
fun formatAmount(amount: Long): String {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    val major = absolute / 100
    val minor = absolute % 100
    val text = "$major.${minor.toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}

/**
 * Parses a user-typed amount (e.g. "42.50", "42,5", "42") into minor units.
 * Returns null when the input is not a valid non-negative amount.
 */
fun parseAmount(input: String): Long? {
    var text = input.trim()
    if (text.isEmpty()) return null
    if (text.startsWith("-") || text.startsWith("+")) return null
    val parts = text.split('.', ',')
    if (parts.size > 2) return null
    val integerPart = parts[0]
    val fractionPart = if (parts.size == 2) parts[1] else ""
    if (integerPart.isEmpty() && fractionPart.isEmpty()) return null
    if (integerPart.any { !it.isDigit() } || fractionPart.any { !it.isDigit() }) return null
    if (fractionPart.length > 2) return null
    val major = integerPart.ifEmpty { "0" }.toLongOrNull() ?: return null
    val minor = fractionPart.padEnd(2, '0').toLongOrNull() ?: return null
    return major * 100 + minor
}
