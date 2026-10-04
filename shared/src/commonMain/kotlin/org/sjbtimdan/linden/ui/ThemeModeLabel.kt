package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.settings_theme_dark
import org.sjbtimdan.linden.resources.settings_theme_light
import org.sjbtimdan.linden.resources.settings_theme_system

/** Label for a theme option in the theme pickers. */
@Composable
internal fun ThemeMode.displayName(): String = stringResource(
    when (this) {
        ThemeMode.SYSTEM -> Res.string.settings_theme_system
        ThemeMode.LIGHT -> Res.string.settings_theme_light
        ThemeMode.DARK -> Res.string.settings_theme_dark
    },
)
