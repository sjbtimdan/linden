package org.sjbtimdan.linden.ui

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import org.sjbtimdan.linden.Screen
import org.sjbtimdan.linden.topLevelScreens
import kotlin.math.abs

/**
 * Follow-finger pager over the app's top-level screens, kept in sync with the
 * bottom navigation: changing [screen] (a tab tap or an in-app shortcut)
 * animates the pager to its page, while a settled swipe reports the page back
 * through [onScreenChange]. The host owns [screen]; pages are addressed by
 * [topLevelScreens]. Only composed while a top-level screen is active, so
 * entering it starts on [screen] without a slide.
 */
@Composable
internal fun TopLevelPager(
    screen: Screen,
    onScreenChange: (Screen) -> Unit,
    modifier: Modifier = Modifier,
    pageContent: @Composable (Screen) -> Unit,
) {
    val pagerState = rememberPagerState(
        initialPage = topLevelScreens.indexOf(screen).coerceAtLeast(0),
    ) { topLevelScreens.size }

    // The host's latest screen, read by the settle collector without restarting it.
    val latestScreen by rememberUpdatedState(screen)

    // A non-adjacent jump (e.g. Settings -> Ledger) would slide through the
    // middle screen; fade the target in over the current page instead.
    val crossFadeAlpha = remember { Animatable(1f) }
    var crossFadeTarget by remember { mutableStateOf<Screen?>(null) }

    LaunchedEffect(screen) {
        val index = topLevelScreens.indexOf(screen)
        if (index >= 0 && pagerState.currentPage != index) {
            if (abs(index - pagerState.currentPage) > 1) {
                crossFadeTarget = screen
                try {
                    crossFadeAlpha.snapTo(0f)
                    crossFadeAlpha.animateTo(1f, tween(180))
                    pagerState.scrollToPage(index)
                } finally {
                    crossFadeTarget = null
                }
            } else {
                pagerState.animateScrollToPage(index)
            }
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            topLevelScreens[page].takeIf { it != latestScreen }?.let(onScreenChange)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize().testTag("topLevelPager"),
        ) { page ->
            pageContent(topLevelScreens[page])
        }

        crossFadeTarget?.let { target ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer { alpha = crossFadeAlpha.value },
            ) {
                pageContent(target)
            }
        }
    }
}
