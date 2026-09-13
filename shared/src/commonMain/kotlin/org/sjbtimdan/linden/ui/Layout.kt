package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
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

/**
 * Root layout shared by every screen: insets, full size, edge padding and a max
 * content width.
 */
@Composable
fun Modifier.screenContainer(): Modifier = this.screenInsets()
    .fillMaxSize()
    .padding(ScreenPadding)
    .widthIn(max = ScreenMaxWidth)

/** [screenContainer] with the IME inset applied above the content padding, for screens under a keyboard. */
@Composable
fun Modifier.screenContainerWithIme(): Modifier = this.screenInsets()
    .fillMaxSize()
    .imePadding()
    .padding(ScreenPadding)
    .widthIn(max = ScreenMaxWidth)
