package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable

/** Light tap haptic for card presses; a no-op where the platform has none. */
@Composable
expect fun rememberTapHaptic(): () -> Unit
