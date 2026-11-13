package org.sjbtimdan.linden.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.PressInteraction
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import kotlinx.coroutines.flow.collect

/**
 * Scales a card to 0.98 while pressed and fires one light haptic when the press
 * becomes a click. Listening for [PressInteraction.Release] instead of the
 * pressed state keeps scroll starts and cancelled gestures quiet.
 */
@Composable
internal fun Modifier.pressFeedback(
    interactionSource: MutableInteractionSource,
    haptic: () -> Unit = rememberTapHaptic(),
): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "cardPressScale",
    )
    val currentHaptic by rememberUpdatedState(haptic)
    LaunchedEffect(interactionSource) {
        interactionSource.interactions.collect { interaction ->
            if (interaction is PressInteraction.Release) currentHaptic()
        }
    }
    // An unpressed card scales to 1f, so drop the layer entirely: a permanent
    // graphicsLayer on every row/button is an offscreen render target that is
    // only needed while the press scale is animating.
    if (scale >= 1f) return this
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
