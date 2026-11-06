package org.sjbtimdan.linden.ui.entry

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.entry_backspace
import org.sjbtimdan.linden.resources.entry_calculator
import org.sjbtimdan.linden.resources.entry_enter
import org.sjbtimdan.linden.resources.entry_simple_keypad
import org.sjbtimdan.linden.ui.pressFeedback
import org.sjbtimdan.linden.ui.theme.lindenColors

/**
 * Amount entry keypad with a simple numeric mode (default) and a full
 * calculator; the display-row toggle switches modes, carrying the typed value
 * across. Enter commits a valid positive amount via [onEnter] (otherwise
 * [onInvalid] fires); Escape or the system back cancels via [onCancel].
 */
@Composable
fun AmountCalculator(
    modifier: Modifier = Modifier,
    initialMinor: Long?,
    currencySymbol: String?,
    decimalDigits: Int = 2,
    onEnter: (String) -> Unit,
    onInvalid: () -> Unit,
    onCancel: () -> Unit,
    contextLabel: String? = null,
) {
    val model = remember(initialMinor, decimalDigits) { CalculatorModel(initialMinor, decimalDigits) }
    var display by remember { mutableStateOf(model.display) }
    var expression by remember { mutableStateOf(model.pendingExpression) }
    var calculatorMode by remember { mutableStateOf(false) }

    fun press(change: () -> Unit) {
        change()
        display = model.display
        expression = model.pendingExpression
    }

    fun enter(): Boolean {
        val value = model.commitValue
        if (value == null) {
            onInvalid()
            return false
        }
        press { model.onEquals() }
        onEnter(value)
        return true
    }

    BoxWithConstraints(
        modifier = modifier
            .fillMaxWidth()
            .onPreviewKeyEvent { event ->
                if (event.type != KeyEventType.KeyDown) return@onPreviewKeyEvent false
                when (event.key) {
                    Key.Enter, Key.NumPadEnter -> {
                        enter()
                        true
                    }

                    Key.Escape -> {
                        onCancel()
                        true
                    }

                    else -> false
                }
            }
            .padding(8.dp),
    ) {
        // Keys cap at [keyHeight]; the keypad fills the available height and hugs
        // the bottom for one-handed use, falling back to a fixed height when
        // the height is unbounded.
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .then(
                    if (constraints.hasBoundedHeight) {
                        Modifier.fillMaxHeight()
                    } else {
                        Modifier.height(560.dp)
                    },
                ),
            verticalArrangement = Arrangement.spacedBy(8.dp, Alignment.Bottom),
        ) {
            contextLabel?.let { label ->
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp)
                        .testTag("calculatorContextLabel"),
                )
            }

            CalculatorDisplay(
                display = display,
                expression = expression,
                currencySymbol = currencySymbol,
                calculatorMode = calculatorMode,
                onToggleMode = { calculatorMode = !calculatorMode },
            )

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
                                    onClick = { press { key.onPress(model) } },
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            CalculatorActions(enter = { enter() })
        }
    }
}

@Composable
private fun CalculatorDisplay(
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

@Composable
private fun CalculatorActions(enter: () -> Unit) {
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

/** Tallest a key row may grow before the keypad hugs the bottom of the screen. */
private val keyHeight = 56.dp

private val KeyShape = RoundedCornerShape(18.dp)
private val ActionShape = RoundedCornerShape(18.dp)

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
