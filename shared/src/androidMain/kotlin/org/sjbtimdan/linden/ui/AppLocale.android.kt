package org.sjbtimdan.linden.ui

import android.content.Context
import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
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
        val locale = tag?.let(Locale::forLanguageTag) ?: systemLocale
        // String resources on Android resolve from the app context's
        // configuration (not Locale.getDefault), so the pin must patch the
        // configuration of the context the composition reads — hence the
        // LocalContext lookup — in addition to the JVM-wide default that
        // drives date and money formatting.
        applyLocale(LocalContext.current, locale)
    }
}

private fun applyLocale(context: Context, locale: Locale) {
    Locale.setDefault(locale)
    updateResources(context, locale)
    val appResources = context.applicationContext.resources
    if (appResources !== context.resources) {
        updateResources(context.applicationContext, locale)
    }
}

@Suppress("DEPRECATION") // Still the in-place locale switch the resource library observes.
private fun updateResources(context: Context, locale: Locale) {
    val resources = context.resources
    val configuration = Configuration(resources.configuration)
    configuration.setLocale(locale)
    configuration.setLayoutDirection(locale)
    resources.updateConfiguration(configuration, resources.displayMetrics)
}
