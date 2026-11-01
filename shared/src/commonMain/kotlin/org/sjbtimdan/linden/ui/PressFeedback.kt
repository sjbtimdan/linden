package org.sjbtimdan.linden.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

/** Scales a card to 0.98 while pressed and fires one light haptic on press. */
@Composable
internal fun Modifier.pressFeedback(interactionSource: MutableInteractionSource): Modifier {
    val pressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) 0.98f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioNoBouncy,
            stiffness = Spring.StiffnessHigh,
        ),
        label = "cardPressScale",
    )
    val tapHaptic = rememberTapHaptic()
    LaunchedEffect(pressed) {
        if (pressed) tapHaptic()
    }
    return graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}
