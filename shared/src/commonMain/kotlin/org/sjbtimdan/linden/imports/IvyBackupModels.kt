package org.sjbtimdan.linden.imports

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.JsonDecoder
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive

@Serializable
internal data class IvyBackup(
    val accounts: List<IvyAccount> = emptyList(),
    val categories: List<IvyCategory> = emptyList(),
    val transactions: List<IvyTransaction> = emptyList(),
)

@Serializable
internal data class IvyAccount(
    val id: String,
    val name: String,
    val currency: String,
)

@Serializable
internal data class IvyCategory(
    val id: String,
    val name: String,
)

@Serializable
internal data class IvyTransaction(
    val type: String,
    @Serializable(with = MinorUnitsSerializer::class)
    val amount: Long,
    val accountId: String,
    val categoryId: String? = null,
    val title: String? = null,
    val toAccountId: String? = null,
    @Serializable(with = MinorUnitsSerializer::class)
    val toAmount: Long? = null,
    val currency: String? = null,
    val toCurrency: String? = null,
    val dateTime: Long? = null,
)

/**
 * Parses an Ivy JSON amount into minor units without floating-point arithmetic.
 * Unlike `parseAmount`, which handles user-typed grouped input, this accepts
 * only a plain decimal or scientific notation ("1.0E7"); the backup is produced
 * by a JVM serializer that may use the latter for large values. Values with more
 * than two decimal places are rounded half-up.
 */
internal fun parseMinorUnits(text: String): Long {
    val trimmed = text.trim()
    if (trimmed.isEmpty()) invalidAmount(text)
    val negative = trimmed.startsWith("-")
    val unsigned = if (negative) trimmed.drop(1) else trimmed
    if (unsigned.isEmpty()) invalidAmount(text)

    val exponentIndex = unsigned.indexOfFirst { it == 'e' || it == 'E' }
    val mantissa = if (exponentIndex == -1) unsigned else unsigned.substring(0, exponentIndex)
    val exponent = if (exponentIndex == -1) 0 else parseExponent(unsigned.substring(exponentIndex + 1), text)
    if (mantissa.isEmpty() || mantissa.any { it != '.' && !it.isDigit() }) invalidAmount(text)

    val parts = mantissa.split('.')
    if (parts.size > 2 || parts[0].isEmpty()) invalidAmount(text)
    val whole = parts[0].toLongOrNull() ?: invalidAmount(text)
    val fraction = parts.getOrNull(1).orEmpty()
    val digits = (whole.toString() + fraction).toLongOrNull() ?: invalidAmount(text)
    val shift = exponent - fraction.length + 2
    val magnitude = if (shift >= 0) {
        multiplyOrNull(digits, pow10(shift) ?: invalidAmount(text)) ?: invalidAmount(text)
    } else {
        divideHalfUp(digits, pow10(-shift) ?: invalidAmount(text))
    }
    return if (negative) -magnitude else magnitude
}

private const val MAX_EXPONENT = 1_000

private fun parseExponent(text: String, original: String): Int {
    if (text.isEmpty()) invalidAmount(original)
    val sign = when (text.first()) {
        '+' -> 1
        '-' -> -1
        else -> 1
    }
    val digits = if (text.first() == '+' || text.first() == '-') text.drop(1) else text
    if (digits.isEmpty() || digits.any { !it.isDigit() }) invalidAmount(original)
    val exponent = digits.toIntOrNull() ?: invalidAmount(original)
    if (exponent > MAX_EXPONENT) invalidAmount(original)
    return exponent * sign
}

private fun pow10(exponent: Int): Long? {
    if (exponent < 0) return null
    var result = 1L
    repeat(exponent) {
        if (result > Long.MAX_VALUE / 10) return null
        result *= 10
    }
    return result
}

private fun multiplyOrNull(a: Long, b: Long): Long? {
    if (a == 0L || b == 0L) return 0L
    val result = a * b
    return if (result / b == a) result else null
}

private fun divideHalfUp(value: Long, divisor: Long): Long {
    val quotient = value / divisor
    val remainder = value % divisor
    return if (remainder * 2 >= divisor) quotient + 1 else quotient
}

private fun invalidAmount(text: String): Nothing = throw SerializationException("Invalid amount \"$text\"")

private object MinorUnitsSerializer : KSerializer<Long> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("IvyMinorUnits", PrimitiveKind.DOUBLE)

    override fun deserialize(decoder: Decoder): Long {
        val jsonDecoder = decoder as? JsonDecoder
            ?: throw SerializationException("Ivy amounts require a JSON decoder")
        val content = jsonDecoder.decodeJsonElement().jsonPrimitive.contentOrNull
            ?: throw SerializationException("Amount is missing")
        return parseMinorUnits(content)
    }

    override fun serialize(encoder: Encoder, value: Long) {
        error("Ivy backups are only decoded, never serialized")
    }
}
