package org.sjbtimdan.linden.ui.entry

private val ENGLISH_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
private val ITALIAN_MONTHS = listOf(
    "gen", "feb", "mar", "apr", "mag", "giu",
    "lug", "ago", "set", "ott", "nov", "dic",
)
private val HINDI_MONTHS = listOf(
    "जन", "फ़र", "मार्च", "अप्रै", "मई", "जून",
    "जुल", "अग", "सित", "अक्तू", "नव", "दिस",
)

/**
 * The language used for date display. Dates never follow the raw system
 * locale: unsupported locales render in English, so numeric date formats
 * (e.g. "08/23/2026") can never leak into the UI. This is also the seam the
 * in-app language override will drive.
 */
internal enum class DateLanguage {
    English,
    Italian,
    Hindi,
    Chinese,

    ;

    /** Short month name for [monthNumber] (1..12): "Aug", "ago", "अग", "8月". */
    internal fun monthShort(monthNumber: Int): String = when (this) {
        English -> ENGLISH_MONTHS[monthNumber - 1]
        Italian -> ITALIAN_MONTHS[monthNumber - 1]
        Hindi -> HINDI_MONTHS[monthNumber - 1]
        Chinese -> "${monthNumber}月"
    }

    /** Localized single date, e.g. "Aug 13, 2026", "13 ago 2026", "13 अग 2026", "2026年8月13日". */
    internal fun dateText(day: Int, monthNumber: Int, year: Int): String = when (this) {
        English -> "${monthShort(monthNumber)} $day, $year"
        Italian, Hindi -> "$day ${monthShort(monthNumber)} $year"
        Chinese -> "${year}年${monthShort(monthNumber)}${day}日"
    }

    /** Localized month-and-year label, e.g. "Aug 2026", "ago 2026", "अग 2026", "2026年8月". */
    internal fun monthYearText(monthNumber: Int, year: Int): String = when (this) {
        English, Italian, Hindi -> "${monthShort(monthNumber)} $year"
        Chinese -> "${year}年${monthShort(monthNumber)}"
    }
}

/** Maps a BCP-47-style language code (e.g. "it", "hi", "zh-Hant-TW") onto a [DateLanguage]. */
internal fun dateLanguage(languageCode: String?): DateLanguage = when {
    languageCode.orEmpty().startsWith("it", ignoreCase = true) -> DateLanguage.Italian
    languageCode.orEmpty().startsWith("hi", ignoreCase = true) -> DateLanguage.Hindi
    languageCode.orEmpty().startsWith("zh", ignoreCase = true) -> DateLanguage.Chinese
    else -> DateLanguage.English
}

/**
 * Full BCP-47 tag of the platform's active locale, e.g. "en-US", "it-CH",
 * "zh-HK". [dateLanguage] only needs the language, while the app-language
 * resolution ([org.sjbtimdan.linden.model.AppLanguage.fromSystemLanguage])
 * needs the region/script to split Simplified from Traditional Chinese.
 */
internal expect fun platformLocaleTag(): String
