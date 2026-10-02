package org.sjbtimdan.linden.ui.entry

private val ENGLISH_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "May", "Jun",
    "Jul", "Aug", "Sep", "Oct", "Nov", "Dec",
)
private val ITALIAN_MONTHS = listOf(
    "gen", "feb", "mar", "apr", "mag", "giu",
    "lug", "ago", "set", "ott", "nov", "dic",
)
private val FRENCH_MONTHS = listOf(
    "janv.", "févr.", "mars", "avr.", "mai", "juin",
    "juil.", "août", "sept.", "oct.", "nov.", "déc.",
)
private val GERMAN_MONTHS = listOf(
    "Jan.", "Feb.", "März", "Apr.", "Mai", "Juni",
    "Juli", "Aug.", "Sept.", "Okt.", "Nov.", "Dez.",
)
private val INDONESIAN_MONTHS = listOf(
    "Jan", "Feb", "Mar", "Apr", "Mei", "Jun",
    "Jul", "Agu", "Sep", "Okt", "Nov", "Des",
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
    French,
    German,
    Hindi,
    Indonesian,
    Chinese,

    ;

    /** Short month name for [monthNumber] (1..12): "Aug", "ago", "août", "Aug.", "अग", "Agu", "8月". */
    internal fun monthShort(monthNumber: Int): String = when (this) {
        English -> ENGLISH_MONTHS[monthNumber - 1]
        Italian -> ITALIAN_MONTHS[monthNumber - 1]
        French -> FRENCH_MONTHS[monthNumber - 1]
        German -> GERMAN_MONTHS[monthNumber - 1]
        Hindi -> HINDI_MONTHS[monthNumber - 1]
        Indonesian -> INDONESIAN_MONTHS[monthNumber - 1]
        Chinese -> "${monthNumber}月"
    }

    /** Localized single date, e.g. "Aug 13, 2026", "13 août 2026", "13. Aug. 2026", "2026年8月13日". */
    internal fun dateText(day: Int, monthNumber: Int, year: Int): String = when (this) {
        English -> "${monthShort(monthNumber)} $day, $year"
        German -> "$day. ${monthShort(monthNumber)} $year"
        Italian, French, Hindi, Indonesian -> "$day ${monthShort(monthNumber)} $year"
        Chinese -> "${year}年${monthShort(monthNumber)}${day}日"
    }

    /** Localized month-and-year label, e.g. "Aug 2026", "août 2026", "Aug. 2026", "2026年8月". */
    internal fun monthYearText(monthNumber: Int, year: Int): String = when (this) {
        English, Italian, French, German, Hindi, Indonesian -> "${monthShort(monthNumber)} $year"
        Chinese -> "${year}年${monthShort(monthNumber)}"
    }
}

/** Maps a BCP-47-style language code (e.g. "fr", "de", "id", "zh-Hant-TW") onto a [DateLanguage]. */
internal fun dateLanguage(languageCode: String?): DateLanguage = when {
    languageCode.orEmpty().startsWith("it", ignoreCase = true) -> DateLanguage.Italian

    languageCode.orEmpty().startsWith("fr", ignoreCase = true) -> DateLanguage.French

    languageCode.orEmpty().startsWith("de", ignoreCase = true) -> DateLanguage.German

    languageCode.orEmpty().startsWith("hi", ignoreCase = true) -> DateLanguage.Hindi

    languageCode.orEmpty().startsWith("id", ignoreCase = true) ||
        languageCode.orEmpty().startsWith("in", ignoreCase = true) -> DateLanguage.Indonesian

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
