package org.sjbtimdan.linden.model

enum class Currency {
    CHF,
    EUR,
    GBP,
    HKD,
    JPY,
    SGD,
    USD;

    companion object {
        fun fromCode(code: String): Currency = entries.firstOrNull { it.name == code }
            ?: error("Unknown currency code: $code")
    }
}
