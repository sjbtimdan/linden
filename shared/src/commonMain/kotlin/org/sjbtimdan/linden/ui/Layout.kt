package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/** Edge padding of the content column shared by all screens. */
val ScreenPadding = 8.dp

/** Max content column width: phones stay full-width, larger screens get more room. */
val ScreenMaxWidth = 720.dp

/**
 * Space a screen's scroll content must reserve at the bottom for the floating
 * navigation pill. Zero outside the app chrome (dialogs, first-run, tests that
 * render a screen directly). Apply it as content padding (LazyColumn
 * `contentPadding` or [BottomBarContentSpacer]), never as container padding:
 * the content must keep drawing behind the pill for the blur to sample it.
 */
internal val LocalBottomBarContentPadding = compositionLocalOf { 0.dp }

/** Scroll-content spacer equal to [LocalBottomBarContentPadding]. */
@Composable
internal fun BottomBarContentSpacer() {
    Spacer(modifier = Modifier.height(LocalBottomBarContentPadding.current))
}

/**
 * Root insets for screens: status bar and side cutouts only. The bottom is
 * owned by the floating navigation bar, and screens reserve it inside their
 * scroll content via [LocalBottomBarContentPadding].
 */
@Composable
fun Modifier.screenInsets(): Modifier = windowInsetsPadding(
    WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal),
)

/**
 * Root layout shared by every screen: insets, full size, a centred max content
 * width and edge padding. The width is capped only inside [wrapContentWidth]:
 * after `fillMaxSize` the incoming constraints are already fixed, so a
 * `widthIn` further in would be clamped back to the window width.
 */
@Composable
fun Modifier.screenContainer(): Modifier = this.screenInsets()
    .fillMaxSize()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = ScreenMaxWidth)
    .fillMaxWidth()
    .padding(ScreenPadding)

/** [screenContainer] with the IME inset applied above the content padding, for screens under a keyboard. */
@Composable
fun Modifier.screenContainerWithIme(): Modifier = this.screenInsets()
    .fillMaxSize()
    .imePadding()
    .wrapContentWidth(Alignment.CenterHorizontally)
    .widthIn(max = ScreenMaxWidth)
    .fillMaxWidth()
    .padding(ScreenPadding)
