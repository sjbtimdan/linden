package org.sjbtimdan.linden.ui

import androidx.compose.runtime.Composable

/**
 * Handles the system back press on Android.
 * On non-Android platforms this is a no-op.
 *
 * [enabled] - whether the back handler is active
 * [onBack] - callback invoked when the back gesture is performed
 */
@Composable
expect fun BackHandler(
    enabled: Boolean,
    onBack: () -> Unit,
)
