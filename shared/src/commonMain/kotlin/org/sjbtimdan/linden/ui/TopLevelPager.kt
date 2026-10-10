package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import org.sjbtimdan.linden.Screen
import org.sjbtimdan.linden.topLevelScreens

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

    LaunchedEffect(screen) {
        val index = topLevelScreens.indexOf(screen)
        if (index >= 0 && pagerState.currentPage != index) {
            pagerState.animateScrollToPage(index)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            topLevelScreens[page].takeIf { it != latestScreen }?.let(onScreenChange)
        }
    }

    HorizontalPager(
        state = pagerState,
        modifier = modifier.fillMaxSize().testTag("topLevelPager"),
    ) { page ->
        pageContent(topLevelScreens[page])
    }
}
