package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable

/**
 * Keeps the system-bar icon polarity aligned with the in-app theme instead of
 * the system night mode, so a forced Light/Dark/AMOLED mode never leaves
 * unreadable icons over the app background. Desktop has no system chrome.
 */
@Composable
expect fun SystemBarsAppearance(darkTheme: Boolean)
