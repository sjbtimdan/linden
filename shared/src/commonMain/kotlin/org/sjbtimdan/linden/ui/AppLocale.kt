package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable
import org.sjbtimdan.linden.model.AppLanguage

/**
 * Applies [language] as the active platform locale while the app runs, so
 * locale-driven reads follow the override: `Locale.getDefault()`-based money
 * and date formatting (incl. the [org.sjbtimdan.linden.ui.entry.DateLanguage]
 * resolution) and, on desktop, the compose resource lookup. [AppLanguage.SYSTEM]
 * restores the platform locale captured at startup.
 *
 * The Android actual additionally needs the configuration-locale update once
 * per-locale string resources exist (Phase 4): resource resolution there reads
 * the app context configuration, not `Locale.getDefault()`.
 *
 * Must run before the subtree it affects composes, which is why it applies
 * during composition (like the locale workaround the CMP docs describe) and why
 * [App] wraps its content in `key(language)` to force a full recomposition.
 */
@Composable
internal expect fun ApplyLanguageOverride(language: AppLanguage)
