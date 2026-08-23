package org.sjbtimdan.linden.ui.entry

import kotlin.math.abs

enum class CalculatorOp { Add, Subtract, Multiply, Divide }

/**
 * Exact decimal calculator state machine for amount entry. Arithmetic runs on
 * reduced fractions (Long numerator/denominator) so chains like `100 / 3 * 3`
 * stay exact (100.00, not 99.99); values are rounded to two decimals only for
 * display and commit. Evaluation is left-to-right with no operator precedence.
 */
class CalculatorModel(initialMinor: Long?) {

    /** Current display text: the typed entry, a rounded result, or "Err". */
    var display: String = initialMinor?.let(::formatMinorUnits) ?: "0.00"
        private set

    private var entry = ""
    private var acc: Fraction? = initialMinor?.let { Fraction(it, 100).reduced() }
    private var op: CalculatorOp? = null
    private var resultMode = initialMinor != null
    private var error = false

    /**
     * Commits the current value as an amount string ("33.33"), or null when it
     * is not a valid positive amount (empty, zero, negative, error, overflow).
     * Pending operations are not evaluated here — call [onEquals] first.
     */
    val commitValue: String?
        get() {
            if (error) return null
            val value = if (entry.isEmpty()) acc else parseEntry(entry)
            if (value == null) return null
            val minor = value.toMinorUnitsOrNull() ?: return null
            if (minor <= 0) return null
            return formatMinorUnits(minor)
        }

    fun onDigit(digit: Char) {
        if (digit !in '0'..'9') return
        if (error) reset()
        if (resultMode) {
            entry = ""
            resultMode = false
        }
        val dot = entry.indexOf('.')
        val intDigits = if (dot == -1) entry.length else dot
        val fracDigits = if (dot == -1) 0 else entry.length - dot - 1
        if (intDigits >= MAX_INT_DIGITS || fracDigits >= MAX_FRAC_DIGITS) return
        entry = if (entry == "0") digit.toString() else entry + digit
        updateDisplay()
    }

    fun onDot() {
        if (error) reset()
        if (resultMode) {
            entry = ""
            resultMode = false
        }
        if ('.' in entry) return
        entry = if (entry.isEmpty()) "0." else entry + '.'
        updateDisplay()
    }

    fun onOperator(operator: CalculatorOp) {
        if (error) return
        val operand = entryFraction()
        val a = acc
        if (operand == null) {
            if (a != null) op = operator
            return
        }
        val next = if (a != null && op != null) {
            val o = op ?: return
            apply(a, o, operand)
        } else {
            operand
        }
        if (next == null) {
            error = true
            updateDisplay()
            return
        }
        acc = next
        op = operator
        entry = ""
        resultMode = false
        updateDisplay()
    }

    fun onEquals() {
        if (error) return
        val a = acc
        val o = op
        if (a != null && o != null) {
            val b = entryFraction() ?: a
            val result = apply(a, o, b)
            if (result == null) {
                error = true
                updateDisplay()
                return
            }
            acc = result
        } else {
            val b = entryFraction() ?: return
            acc = b
        }
        op = null
        entry = ""
        resultMode = true
        updateDisplay()
    }

    fun onBackspace() {
        if (error || resultMode) return
        entry = entry.dropLast(1)
        updateDisplay()
    }

    fun onClear() {
        reset()
    }

    private fun reset() {
        entry = ""
        acc = null
        op = null
        resultMode = false
        error = false
        updateDisplay()
    }

    private fun entryFraction(): Fraction? = if (entry.isEmpty()) null else parseEntry(entry)

    private fun updateDisplay() {
        display = when {
            error -> "Err"
            entry.isNotEmpty() -> entry
            acc != null -> acc?.toDisplayStringOrNull() ?: "Err"
            else -> "0.00"
        }
    }

    private companion object {
        const val MAX_INT_DIGITS = 14
        const val MAX_FRAC_DIGITS = 4
    }
}

/** A reduced fraction; [den] is always positive. */
private data class Fraction(val num: Long, val den: Long)

private fun gcd(a: Long, b: Long): Long {
    var x = a
    var y = b
    while (y != 0L) {
        val t = x % y
        x = y
        y = t
    }
    return x
}

private fun Fraction.reduced(): Fraction {
    val g = gcd(abs(num), den)
    return if (g > 1) Fraction(num / g, den / g) else this
}

/** Overflow-safe Long arithmetic; returns null on overflow (or Long.MIN / -1). */
private fun safeAdd(a: Long, b: Long): Long? {
    val result = a + b
    return if ((b > 0 && result < a) || (b < 0 && result > a)) null else result
}

private fun safeSubtract(a: Long, b: Long): Long? {
    val result = a - b
    return if ((b > 0 && result > a) || (b < 0 && result < a)) null else result
}

private fun safeMultiply(a: Long, b: Long): Long? = try {
    val result = a * b
    if (b != 0L && result / b != a) null else result
} catch (_: ArithmeticException) {
    null
}

private fun Fraction.add(other: Fraction): Fraction? {
    val numSum = safeAdd(safeMultiply(num, other.den) ?: return null, safeMultiply(other.num, den) ?: return null)
        ?: return null
    val denProduct = safeMultiply(den, other.den) ?: return null
    return Fraction(numSum, denProduct).reduced()
}

private fun Fraction.subtract(other: Fraction): Fraction? {
    val numDiff = safeSubtract(safeMultiply(num, other.den) ?: return null, safeMultiply(other.num, den) ?: return null)
        ?: return null
    val denProduct = safeMultiply(den, other.den) ?: return null
    return Fraction(numDiff, denProduct).reduced()
}

private fun Fraction.multiply(other: Fraction): Fraction? {
    val numProduct = safeMultiply(num, other.num) ?: return null
    val denProduct = safeMultiply(den, other.den) ?: return null
    return Fraction(numProduct, denProduct).reduced()
}

private fun Fraction.divide(other: Fraction): Fraction? {
    if (other.num == 0L) return null
    val n = safeMultiply(num, other.den) ?: return null
    val d = safeMultiply(den, other.num) ?: return null
    // Keep the denominator positive: flip both signs when it came out negative.
    return if (d < 0) {
        val negN = safeSubtract(0, n) ?: return null
        val negD = safeSubtract(0, d) ?: return null
        Fraction(negN, negD).reduced()
    } else {
        Fraction(n, d).reduced()
    }
}

private fun apply(a: Fraction, op: CalculatorOp, b: Fraction): Fraction? = when (op) {
    CalculatorOp.Add -> a.add(b)
    CalculatorOp.Subtract -> a.subtract(b)
    CalculatorOp.Multiply -> a.multiply(b)
    CalculatorOp.Divide -> a.divide(b)
}

/** Parses the typed entry ("12.50") into a fraction; "12." is treated as 12. */
private fun parseEntry(input: String): Fraction? {
    if (input.isEmpty()) return null
    val dot = input.indexOf('.')
    val intPart = if (dot == -1) input else input.substring(0, dot)
    val fracPart = if (dot == -1) "" else input.substring(dot + 1)
    val integer = intPart.toLongOrNull() ?: return null
    if (fracPart.length > 18) return null
    val fraction = if (fracPart.isEmpty()) 0L else fracPart.toLongOrNull() ?: return null
    var den = 1L
    repeat(fracPart.length) { den *= 10 }
    val combined = safeAdd(safeMultiply(integer, den) ?: return null, fraction) ?: return null
    return Fraction(combined, den).reduced()
}

/** Rounds the fraction to minor units, half away from zero; null on overflow. */
private fun Fraction.toMinorUnitsOrNull(): Long? {
    if (num == Long.MIN_VALUE || abs(num) > Long.MAX_VALUE / 100) return null
    val scaled = num * 100
    val q = scaled / den
    val r = scaled % den
    val roundUp = abs(r) >= den - abs(r)
    return if (roundUp) q + if (scaled >= 0) 1 else -1 else q
}

private fun Fraction.toDisplayStringOrNull(): String? = toMinorUnitsOrNull()?.let(::formatMinorUnits)

/** Formats minor units as a plain two-decimal string, e.g. 1000 -> "10.00". */
private fun formatMinorUnits(minor: Long): String {
    val negative = minor < 0
    val absolute = if (negative) -minor else minor
    val text = "${absolute / 100}.${(absolute % 100).toString().padStart(2, '0')}"
    return if (negative) "-$text" else text
}
