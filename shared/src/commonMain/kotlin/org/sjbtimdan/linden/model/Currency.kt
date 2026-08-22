package org.sjbtimdan.linden.model

enum class Currency(
    val symbol: String,
) {
    CHF("CHF"),
    EUR("€"),
    GBP("£"),
    HKD("HK$"),
    JPY("¥"),
    SGD("S$"),
    USD("$"),
    ;

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.name == code }
            ?: error("Unknown currency code: $code")
    }
}
