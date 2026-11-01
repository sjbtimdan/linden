package org.sjbtimdan.linden.ui.entry

/**
 * A formatted amount split for the hero card: [whole] keeps the large style
 * while [fraction] (including the locale decimal separator) and a compact
 * [unitSuffix] ("m"/"b") render smaller in the gold accent.
 */
internal data class HeroAmountParts(
    val whole: String,
    val fraction: String? = null,
    val unitSuffix: String? = null,
) {
    /** The exact formatted amount, also used as the text semantics and animation key. */
    val text: String
        get() = buildString {
            append(whole)
            fraction?.let(::append)
            unitSuffix?.let(::append)
        }
}

private const val MILLION_MINOR = 100_000_000L

/**
 * Splits [amount] (minor units) into the parts [HeroAmountParts] describes.
 * Matches [formatAmount] below one million and [formatAmountCompact] at or
 * above it, so [HeroAmountParts.text] always equals the amount the rest of the
 * UI shows. Zero-decimal currencies (IDR, JPY) have no fraction; the decimal
 * separator follows the platform locale.
 */
internal fun heroAmountParts(amount: Long, decimalDigits: Int): HeroAmountParts {
    val negative = amount < 0
    val absolute = if (negative) -amount else amount
    if (absolute >= MILLION_MINOR) return compactParts(formatAmountCompact(amount, decimalDigits))
    if (decimalDigits == 0) return HeroAmountParts(whole = formatAmount(amount, 0))
    val wholeMinor = absolute - absolute % 100
    val whole = formatAmount(wholeMinor, 0).let { if (negative) "-$it" else it }
    val fraction = (absolute % 100).toString().padStart(2, '0')
    return HeroAmountParts(whole = whole, fraction = "${decimalSeparator()}$fraction")
}

private fun compactParts(formatted: String): HeroAmountParts {
    val suffix = formatted.last().takeIf { it == 'm' || it == 'b' }?.toString()
    val number = if (suffix == null) formatted else formatted.dropLast(1)
    val dot = number.lastIndexOf('.')
    return if (dot == -1) {
        HeroAmountParts(whole = number, unitSuffix = suffix)
    } else {
        HeroAmountParts(
            whole = number.substring(0, dot),
            fraction = number.substring(dot),
            unitSuffix = suffix,
        )
    }
}

private fun decimalSeparator(): Char = formatAmount(1, 2).getOrElse(1) { '.' }
