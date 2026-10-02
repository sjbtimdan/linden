package org.sjbtimdan.linden.model

enum class Currency(
    val symbol: String,
    val decimalDigits: Int = 2,
) {
    CHF("CHF"),
    CNY("CN¥"),
    EUR("€"),
    GBP("£"),
    HKD("HK$"),
    IDR("Rp", decimalDigits = 0),
    INR("₹"),
    JPY("¥", decimalDigits = 0),
    SGD("S$"),
    USD("$"),
    ;

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.name == code }
            ?: error("Unknown currency code: $code")
    }
}
