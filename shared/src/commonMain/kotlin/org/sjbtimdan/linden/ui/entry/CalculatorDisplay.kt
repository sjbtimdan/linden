package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_calculator
import org.sjbtimdan.linden.resources.entry_simple_keypad

@Composable
internal fun CalculatorDisplay(
    display: String,
    expression: String?,
    currencySymbol: String?,
    calculatorMode: Boolean,
    onToggleMode: () -> Unit,
) {
    val expressionAlpha by animateFloatAsState(
        targetValue = if (expression != null) 1f else 0f,
        animationSpec = tween(120),
        label = "calculatorExpressionAlpha",
    )
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = onToggleMode,
            modifier = Modifier.testTag("calculatorModeToggle"),
        ) {
            Icon(
                imageVector = if (calculatorMode) Icons.Filled.Dialpad else Icons.Filled.Calculate,
                contentDescription = if (calculatorMode) {
                    stringResource(Res.string.entry_simple_keypad)
                } else {
                    stringResource(Res.string.entry_calculator)
                },
            )
        }
        Column(
            modifier = Modifier.weight(1f),
            horizontalAlignment = Alignment.End,
        ) {
            Text(
                text = expression.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .fillMaxWidth()
                    .alpha(expressionAlpha)
                    .testTag("calculatorExpression"),
            )
            Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                AnimatedContent(
                    targetState = display,
                    transitionSpec = { fadeIn(tween(120)) togetherWith fadeOut(tween(120)) },
                    modifier = Modifier.weight(1f),
                    label = "calculatorDisplayValue",
                ) { value ->
                    Text(
                        text = value,
                        style = MaterialTheme.typography.displayMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.fillMaxWidth().testTag("calculatorDisplay"),
                    )
                }
                Spacer(modifier = Modifier.width(4.dp))
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(34.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.6f)),
                )
                currencySymbol?.let { symbol ->
                    Text(
                        text = symbol,
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                    )
                }
            }
        }
    }
}
