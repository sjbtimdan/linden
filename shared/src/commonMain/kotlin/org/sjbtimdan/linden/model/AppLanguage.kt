package org.sjbtimdan.linden.model

/**
 * The app language. [SYSTEM] means "no explicit choice": the effective
 * language resolves from the platform locale via [fromSystemLanguage], and the
 * app keeps following the system on later launches until the user pins a
 * language in Settings. The remaining values pin the app to one of the
 * supported languages via their BCP-47 [tag], which drives both the locale
 * override applied at the app root and the resource qualifiers the
 * translations ship under (values/values-it/values-fr/values-de/values-hi/
 * values-id/values-zh-rCN/values-zh-rHK; Android also needs the legacy
 * values-in mirror of Indonesian because its Locale reports "in").
 * Traditional Chinese is Hong Kong (zh-HK); other zh-Hant regions are not
 * target markets.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ITALIAN("it"),
    FRENCH("fr"),
    GERMAN("de"),
    HINDI("hi"),
    INDONESIAN("id"),
    CHINESE_SIMPLIFIED("zh-CN"),
    CHINESE_TRADITIONAL_HK("zh-HK"),
    ;

    companion object {
        /** Parses a persisted tag; anything unknown (or null) means [SYSTEM]. */
        fun fromTag(tag: String?): AppLanguage = when {
            tag == "in" -> INDONESIAN
            else -> entries.firstOrNull { it.tag == tag } ?: SYSTEM
        }

        /**
         * Maps the platform's full BCP-47 tag onto the closest supported app
         * language: Italian, French, German, Hindi, Indonesian and Chinese
         * systems get their own language (Chinese split by script/region: Hans
         * or CN → Simplified, Hant or HK/MO/TW → Traditional), every other
         * system language falls back to [ENGLISH]. Android reports Indonesian
         * as the legacy tag "in", so that maps here too.
         */
        fun fromSystemLanguage(systemTag: String?): AppLanguage = when {
            systemTag.isNullOrBlank() -> ENGLISH

            systemTag.startsWith("it", ignoreCase = true) -> ITALIAN

            systemTag.startsWith("fr", ignoreCase = true) -> FRENCH

            systemTag.startsWith("de", ignoreCase = true) -> GERMAN

            systemTag.startsWith("hi", ignoreCase = true) -> HINDI

            systemTag.startsWith("id", ignoreCase = true) ||
                systemTag.startsWith("in", ignoreCase = true) -> INDONESIAN

            systemTag.startsWith("zh", ignoreCase = true) -> {
                val lower = systemTag.lowercase()
                if (lower.contains("-hant") || lower.contains("-tw") ||
                    lower.contains("-hk") || lower.contains("-mo")
                ) {
                    CHINESE_TRADITIONAL_HK
                } else {
                    CHINESE_SIMPLIFIED
                }
            }

            else -> ENGLISH
        }

        /** The concrete app language in effect: [SYSTEM] resolves against [systemTag]. */
        fun resolve(language: AppLanguage, systemTag: String?): AppLanguage =
            if (language == SYSTEM) fromSystemLanguage(systemTag) else language
    }
}
