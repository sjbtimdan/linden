package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_enter
import org.sjbtimdan.linden.ui.pressFeedback
import org.sjbtimdan.linden.ui.theme.lindenColors

@Composable
internal fun CalculatorActions(enter: () -> Unit) {
    GradientActionButton(
        label = stringResource(Res.string.entry_enter),
        onClick = enter,
        modifier = Modifier.fillMaxWidth().height(56.dp),
    )
}

@Composable
private fun GradientActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val colors = lindenColors()
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .alpha(if (enabled) 1f else 0.38f)
            .clip(ActionShape)
            .background(Brush.linearGradient(colors.heroGradient))
            .clickable(
                interactionSource = interaction,
                indication = LocalIndication.current,
                enabled = enabled,
                role = Role.Button,
                onClick = onClick,
            )
            .pressFeedback(interaction),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.titleMedium,
            color = colors.heroContent,
        )
    }
}

private val ActionShape = RoundedCornerShape(18.dp)
