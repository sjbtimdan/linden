package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.model.AppLanguage
import java.util.Locale

private val systemLocale: Locale = Locale.getDefault()
private var appliedTag: String? = null
private var appliedOnce = false

@Composable
internal actual fun ApplyLanguageOverride(language: AppLanguage) {
    val tag = language.tag
    if (!appliedOnce || tag != appliedTag) {
        appliedOnce = true
        appliedTag = tag
        Locale.setDefault(tag?.let(Locale::forLanguageTag) ?: systemLocale)
        // String resources read the app context configuration on Android; the
        // configuration-locale update lands with the first translated resources.
    }
}
