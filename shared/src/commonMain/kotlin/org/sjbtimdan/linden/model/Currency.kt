package org.sjbtimdan.linden.model

enum class Currency(
    val symbol: String,
) {
    CHF("CHF"),
    CNY("CN¥"),
    EUR("€"),
    GBP("£"),
    HKD("HK$"),
    INR("₹"),
    JPY("¥"),
    SGD("S$"),
    USD("$"),
    ;

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.name == code }
            ?: error("Unknown currency code: $code")
    }
}
