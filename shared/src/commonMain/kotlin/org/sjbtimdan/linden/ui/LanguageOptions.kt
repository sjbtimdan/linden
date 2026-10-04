package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.model.AppLanguage

/** The language options offered by the language pickers, in display order. */
internal val languageOptions = listOf(
    AppLanguage.ENGLISH,
    AppLanguage.ITALIAN,
    AppLanguage.FRENCH,
    AppLanguage.GERMAN,
    AppLanguage.HINDI,
    AppLanguage.INDONESIAN,
    AppLanguage.CHINESE_SIMPLIFIED,
    AppLanguage.CHINESE_TRADITIONAL_HK,
)

/** Picker label: each language's own name for itself. */
@Composable
internal fun AppLanguage.label(): String = when (this) {
    // SYSTEM is never offered: the picker resolves it before selecting.
    AppLanguage.SYSTEM -> ""

    AppLanguage.ENGLISH -> "English"

    AppLanguage.ITALIAN -> "Italiano"

    AppLanguage.FRENCH -> "Français"

    AppLanguage.GERMAN -> "Deutsch"

    AppLanguage.HINDI -> "हिन्दी"

    AppLanguage.INDONESIAN -> "Bahasa Indonesia"

    AppLanguage.CHINESE_SIMPLIFIED -> "简体中文"

    AppLanguage.CHINESE_TRADITIONAL_HK -> "繁體中文（香港）"
}
