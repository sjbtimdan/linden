package org.sjbtimdan.linden.capture

import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.EntryType
import org.sjbtimdan.linden.ui.entry.parseAmount

/**
 * Parses a payment notification into a [ParsedCapture], or null when the text
 * is not a payment. Deliberately conservative — everything accepted here is
 * still confirmed by the user before an entry is written:
 *
 * - a currency marker (code like CHF/EUR, or a symbol) must sit next to a
 *   parseable amount; OTP codes and most non-payment noise carry no currency;
 * - OTP and declined/failed wording drops the notification outright;
 * - only the first currency-marked amount is used;
 * - the amount is returned positive and the direction (sign or credited
 *   wording) is carried by [ParsedCapture.type], defaulting to an expense.
 */
fun parseCaptureNotification(notification: CaptureNotification): ParsedCapture? {
    val body = notification.bigText?.takeIf { it.isNotBlank() }
        ?: notification.text?.takeIf { it.isNotBlank() }
    val searchText = listOfNotNull(notification.title, body, notification.subText)
        .filter { it.isNotBlank() }
        .joinToString(" ")
        .replace('\n', ' ')
        .trim()
    if (searchText.isEmpty()) return null

    val lower = searchText.lowercase()
    if (OTP_KEYWORDS.any { lower.contains(it) }) return null
    if (DECLINED_KEYWORDS.any { lower.contains(it) }) return null

    val match = firstAmountMatch(searchText) ?: return null
    val description = (body ?: notification.title.orEmpty())
        .replace('\n', ' ')
        .trim()
        .take(DESCRIPTION_MAX_LENGTH)
    return ParsedCapture(
        amountMinor = match.amountMinor,
        currency = match.currency,
        description = description,
        type = direction(match.sign, lower),
    )
}

private const val DESCRIPTION_MAX_LENGTH = 140

/** First amount directly adjacent to a currency marker, in text order. */
private fun firstAmountMatch(text: String): AmountMatch? {
    for (marker in markerMatches(text)) {
        val decimalDigits = marker.currency?.decimalDigits ?: 2
        val amount = amountAfter(text, marker.start + marker.length, decimalDigits)
            ?: amountBefore(text, marker.start, decimalDigits)
            ?: continue
        return AmountMatch(amount.amountMinor, marker.currency, amount.sign)
    }
    return null
}

private data class MarkerMatch(val start: Int, val length: Int, val currency: Currency?)

/**
 * Every currency marker in the text, in order of appearance. Overlapping
 * matches are resolved longest-first, so "CN¥24.50" reads as CNY rather than
 * matching the "¥" inside it.
 */
private fun markerMatches(text: String): List<MarkerMatch> {
    val candidates = mutableListOf<MarkerMatch>()
    for (currency in Currency.entries) {
        val code = currency.name
        var index = text.indexOf(code, ignoreCase = true)
        while (index >= 0) {
            if (isCodeBoundary(text, index, code.length)) {
                candidates += MarkerMatch(index, code.length, currency)
            }
            index = text.indexOf(code, index + 1, ignoreCase = true)
        }
    }
    for ((symbol, currency) in CURRENCY_SYMBOLS) {
        var index = text.indexOf(symbol)
        while (index >= 0) {
            if (isSymbolBoundary(text, index, symbol)) {
                candidates += MarkerMatch(index, symbol.length, currency)
            }
            index = text.indexOf(symbol, index + 1)
        }
    }
    val accepted = mutableListOf<MarkerMatch>()
    for (candidate in candidates.sortedWith(compareBy({ it.start }, { -it.length }))) {
        val overlaps = accepted.any {
            candidate.start < it.start + it.length &&
                it.start < candidate.start + candidate.length
        }
        if (!overlaps) accepted += candidate
    }
    return accepted
}

private fun isCodeBoundary(text: String, index: Int, length: Int): Boolean =
    text.getOrNull(index - 1)?.isLetter() != true && text.getOrNull(index + length)?.isLetter() != true

private fun isSymbolBoundary(text: String, index: Int, symbol: String): Boolean {
    if (symbol.first().isLetter() && text.getOrNull(index - 1)?.isLetter() == true) return false
    if (symbol.last().isLetter() && text.getOrNull(index + symbol.length)?.isLetter() == true) return false
    return true
}

private data class TokenAmount(val amountMinor: Long, val sign: Char?)

private data class AmountMatch(val amountMinor: Long, val currency: Currency?, val sign: Char?)

/**
 * The number right after a marker, allowing a small gap and a sign. The token
 * charset excludes the regular space so a following number or word cannot be
 * swallowed; NBSP/NNBSP grouping (used by French-style formatting) is kept.
 */
private val AMOUNT_AFTER = Regex("^[\\s\\u00A0\\u202F]{0,3}([+\\-\\u2212]?\\s?\\d[\\d.,'\\u00A0\\u202F]*)")

/** Mirror of [AMOUNT_AFTER] for amounts that precede the marker ("24,50 €"). */
private val AMOUNT_BEFORE = Regex("([+\\-\\u2212]?\\s?\\d[\\d.,'\\u00A0\\u202F]*)[\\s\\u00A0\\u202F]{0,3}$")

private const val TOKEN_TRAILING = " .,'\u00A0\u202F"

private fun amountAfter(text: String, from: Int, decimalDigits: Int): TokenAmount? {
    val window = text.substring(from)
    val raw = AMOUNT_AFTER.find(window)?.groupValues?.get(1) ?: return null
    return parseToken(raw, decimalDigits)
}

private fun amountBefore(text: String, markerStart: Int, decimalDigits: Int): TokenAmount? {
    val window = text.substring(0, markerStart)
    val raw = AMOUNT_BEFORE.find(window)?.groupValues?.get(1) ?: return null
    return parseToken(raw, decimalDigits)
}

private fun parseToken(raw: String, decimalDigits: Int): TokenAmount? {
    val token = raw.trimEnd { it in TOKEN_TRAILING }
    if (token.isEmpty()) return null
    val sign = token.first().takeIf { it == '+' || it == '-' || it == '\u2212' }
    val value = parseAmount(token, decimalDigits) ?: return null
    if (value == 0L) return null
    return TokenAmount(if (value < 0) -value else value, sign)
}

private fun direction(sign: Char?, lowerText: String): EntryType = when {
    sign == '-' || sign == '\u2212' -> EntryType.Expense
    sign == '+' -> EntryType.Income
    INCOME_KEYWORDS.any { lowerText.contains(it) } -> EntryType.Income
    else -> EntryType.Expense
}

/**
 * Unambiguous symbols, longest first so the overlap filter can prefer them.
 * "$" and "¥" are recognized but ambiguous (USD/HKD/SGD, JPY/CNY): they yield
 * a null currency, and the mapped account's currency applies at review time.
 */
private val CURRENCY_SYMBOLS: List<Pair<String, Currency?>> = listOf(
    "CN¥" to Currency.CNY,
    "HK$" to Currency.HKD,
    "S$" to Currency.SGD,
    "€" to Currency.EUR,
    "£" to Currency.GBP,
    "₹" to Currency.INR,
    "Rp" to Currency.IDR,
    "$" to null,
    "¥" to null,
)

/** One-time-password wording: never captured, in every app language. */
private val OTP_KEYWORDS = listOf(
    "otp",
    "one-time",
    "one time",
    "2fa",
    "two-factor",
    "verification",
    "verify",
    "security code",
    "access code",
    "code is",
    "code:",
    "passcode",
    "authentifizierung",
    "bestätigungscode",
    "sicherheitscode",
    "verifizierung",
    "vérification",
    "code secret",
    "code de sécurité",
    "codice di verifica",
    "codice otp",
    "codice di sicurezza",
    "kode verifikasi",
    "kode otp",
    "kode keamanan",
    "验证码",
    "驗證碼",
    "动态密码",
    "動態密碼",
    "认证码",
    "認證碼",
    "सत्यापन",
    "सुरक्षा कोड",
    "ओटीपी",
)

/** Declined/voided wording: a failed charge must never become an entry. */
private val DECLINED_KEYWORDS = listOf(
    "declined",
    "rejected",
    "not approved",
    "unsuccessful",
    "failed",
    "failure",
    "cancelled",
    "canceled",
    "voided",
    "abgelehnt",
    "fehlgeschlagen",
    "nicht genehmigt",
    "storniert",
    "refusé",
    "refusée",
    "rejeté",
    "échoué",
    "annulé",
    "rifiutato",
    "negato",
    "non riuscito",
    "annullato",
    "ditolak",
    "gagal",
    "dibatalkan",
    "失败",
    "失敗",
    "已拒绝",
    "已拒絕",
    "已取消",
    "अस्वीकृत",
    "विफल",
    "रद्द",
)

/** Credited wording flips the default direction to income. */
private val INCOME_KEYWORDS = listOf(
    "received",
    "credited",
    "refund",
    "refunded",
    "incoming",
    "deposit",
    "gutschrift",
    "erstattung",
    "rückerstattung",
    "erhalten",
    "eingegangen",
    "crédité",
    "reçu",
    "remboursement",
    "encaissé",
    "accreditato",
    "ricevuto",
    "rimborso",
    "diterima",
    "dana masuk",
    "pengembalian",
    "已到账",
    "已到賬",
    "退款",
    "收入",
    "收到",
    "प्राप्त",
    "जमा",
    "वापसी",
)
