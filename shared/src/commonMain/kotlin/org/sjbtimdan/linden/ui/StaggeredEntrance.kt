package org.sjbtimdan.linden.ui

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

private const val STAGGER_DELAY_MS = 20L
private const val MAX_STAGGER_STEPS = 8
private const val ENTRANCE_DURATION_MS = 220L
private const val ENTRANCE_SLIDE_DP = 8

/**
 * Tracks the first entrance of a list. Once the initial stagger has played,
 * items composed later (by scrolling or a data refresh) appear immediately, so
 * the effect never replays on every scroll.
 */
@Composable
internal fun rememberEntranceComplete(itemCount: Int): Boolean {
    var complete by rememberSaveable { mutableStateOf(false) }
    LaunchedEffect(itemCount) {
        if (!complete && itemCount > 0) {
            delay(minOf(itemCount, MAX_STAGGER_STEPS) * STAGGER_DELAY_MS + ENTRANCE_DURATION_MS)
            complete = true
        }
    }
    return complete
}

/** Fades a list item in from 8dp below; once the list entrance is over it is a no-op. */
@Composable
internal fun Modifier.staggeredEntrance(index: Int, animateIn: Boolean): Modifier {
    var shown by remember { mutableStateOf(!animateIn) }
    LaunchedEffect(animateIn) {
        if (!shown) {
            if (animateIn) delay(minOf(index, MAX_STAGGER_STEPS) * STAGGER_DELAY_MS)
            shown = true
        }
    }
    val slideDistance = with(LocalDensity.current) { ENTRANCE_SLIDE_DP.dp.toPx() }
    val progress by animateFloatAsState(
        targetValue = if (shown) 1f else 0f,
        animationSpec = tween(180),
        label = "staggeredEntrance",
    )
    return graphicsLayer {
        alpha = progress
        translationY = (1f - progress) * slideDistance
    }
}
