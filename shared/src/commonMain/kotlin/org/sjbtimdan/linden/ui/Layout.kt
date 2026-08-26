package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Edge padding of the content column shared by all screens. */
val ScreenPadding = 8.dp

/** Max content column width: phones stay full-width, larger screens get more room. */
val ScreenMaxWidth = 720.dp

/**
 * Root insets for screens: status bar and side cutouts only. The bottom is
 * already handled by the scaffold's navigation bar, which extends into the
 * system gesture area — padding it again would leave a gap above the bar.
 */
@Composable
fun Modifier.screenInsets(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)
