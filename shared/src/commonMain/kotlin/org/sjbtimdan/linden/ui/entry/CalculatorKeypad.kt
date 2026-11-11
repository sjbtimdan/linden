package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_backspace
import org.sjbtimdan.linden.ui.pressFeedback

/** Tallest a key row may grow before the keypad hugs the bottom of the screen. */
private val keyHeight = 56.dp

private val KeyShape = RoundedCornerShape(18.dp)

private enum class KeyKind { Digit, Operator, Action, Equals }

private class KeySpec(
    val label: String,
    val kind: KeyKind,
    val contentDescription: StringResource? = null,
    val onPress: CalculatorModel.() -> Unit,
)

private fun digit(ch: Char) = KeySpec(label = ch.toString(), kind = KeyKind.Digit) { onDigit(ch) }

/** The keypad layout, top to bottom. */
private val keypad = listOf(
    listOf(
        digit('7'),
        digit('8'),
        digit('9'),
        KeySpec("÷", KeyKind.Operator) { onOperator(CalculatorOp.Divide) },
    ),
    listOf(
        digit('4'),
        digit('5'),
        digit('6'),
        KeySpec("×", KeyKind.Operator) { onOperator(CalculatorOp.Multiply) },
    ),
    listOf(
        digit('1'),
        digit('2'),
        digit('3'),
        KeySpec("−", KeyKind.Operator) { onOperator(CalculatorOp.Subtract) },
    ),
    listOf(
        KeySpec("C", KeyKind.Action) { onClear() },
        digit('0'),
        KeySpec(".", KeyKind.Action) { onDot() },
        KeySpec("+", KeyKind.Operator) { onOperator(CalculatorOp.Add) },
    ),
    listOf(
        KeySpec("⌫", KeyKind.Action, contentDescription = Res.string.entry_backspace) { onBackspace() },
        KeySpec("=", KeyKind.Equals) { onEquals() },
    ),
)

/** The simple numeric keypad, top to bottom. */
private val simpleKeypad = listOf(
    listOf(digit('1'), digit('2'), digit('3')),
    listOf(digit('4'), digit('5'), digit('6')),
    listOf(digit('7'), digit('8'), digit('9')),
    listOf(
        KeySpec(".", KeyKind.Action) { onDot() },
        digit('0'),
        KeySpec("C", KeyKind.Action) { onClear() },
        KeySpec("⌫", KeyKind.Action, contentDescription = Res.string.entry_backspace) { onBackspace() },
    ),
)

/**
 * The key row area: either the simple keypad or the full calculator, switching
 * with a fade. Zero-decimal currencies drop the decimal key.
 */
@Composable
internal fun ColumnScope.CalculatorKeypad(
    calculatorMode: Boolean,
    decimalDigits: Int,
    onPress: (CalculatorModel.() -> Unit) -> Unit,
) {
    AnimatedContent(
        targetState = calculatorMode,
        transitionSpec = {
            fadeIn(tween(140)) togetherWith fadeOut(tween(100))
        },
        modifier = Modifier.weight(1f),
        label = "calculatorKeypad",
    ) { mode ->
        val rows = if (mode) keypad else simpleKeypad
        val shownRows = if (decimalDigits == 0) {
            rows.map { row -> row.filterNot { it.label == "." } }
        } else {
            rows
        }
        Column(
            modifier = Modifier.fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            shownRows.forEach { row ->
                keyRow(Modifier.weight(1f, fill = false).heightIn(max = keyHeight)) {
                    row.forEach { key ->
                        CalculatorKey(
                            key = key,
                            onClick = { onPress(key.onPress) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun keyRow(modifier: Modifier, content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        content = content,
    )
}

@Composable
private fun RowScope.CalculatorKey(key: KeySpec, onClick: () -> Unit) {
    val description = key.contentDescription?.let { stringResource(it) }
    val interaction = remember { MutableInteractionSource() }
    val container = when (key.kind) {
        KeyKind.Digit -> MaterialTheme.colorScheme.surfaceContainerHigh
        KeyKind.Operator -> MaterialTheme.colorScheme.secondaryContainer
        KeyKind.Action -> MaterialTheme.colorScheme.surfaceContainerHighest
        KeyKind.Equals -> MaterialTheme.colorScheme.primary
    }
    val contentColor = when (key.kind) {
        KeyKind.Digit -> MaterialTheme.colorScheme.onSurface
        KeyKind.Operator -> MaterialTheme.colorScheme.onSecondaryContainer
        KeyKind.Action -> MaterialTheme.colorScheme.onSurfaceVariant
        KeyKind.Equals -> MaterialTheme.colorScheme.onPrimary
    }
    val modifier = Modifier
        .weight(1f)
        .fillMaxHeight()
        .clip(KeyShape)
        .background(container)
        .clickable(
            interactionSource = interaction,
            indication = LocalIndication.current,
            role = Role.Button,
            onClick = onClick,
        )
        .pressFeedback(interaction)
        .then(
            if (description != null) {
                Modifier.semantics { contentDescription = description }
            } else {
                Modifier
            },
        )
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Text(
            text = key.label,
            style = MaterialTheme.typography.titleLarge,
            color = contentColor,
        )
    }
}
