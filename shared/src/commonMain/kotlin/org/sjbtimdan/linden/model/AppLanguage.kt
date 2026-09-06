package org.sjbtimdan.linden.model

/**
 * The app language. [SYSTEM] follows the platform locale; the remaining values
 * pin the app to one of the supported languages via their BCP-47 [tag]. The tag
 * drives both the locale override applied at the app root and the resource
 * qualifiers the translations will ship under (values/values-it/values-zh-rCN/
 * values-zh-rHK). Traditional Chinese is Hong Kong (zh-HK); other zh-Hant
 * regions are not target markets.
 */
enum class AppLanguage(val tag: String?) {
    SYSTEM(null),
    ENGLISH("en"),
    ITALIAN("it"),
    CHINESE_SIMPLIFIED("zh-CN"),
    CHINESE_TRADITIONAL_HK("zh-HK"),
    ;

    companion object {
        /** Parses a persisted tag; anything unknown (or null) means [SYSTEM]. */
        fun fromTag(tag: String?): AppLanguage = entries.firstOrNull { it.tag == tag } ?: SYSTEM
    }
}
