package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Dialpad
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp

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
    onEnter: (String) -> Unit,
    onInvalid: () -> Unit,
    onCancel: () -> Unit,
    contextLabel: String? = null,
) {
    val model = remember(initialMinor) { CalculatorModel(initialMinor) }
    var display by remember { mutableStateOf(model.display) }
    var calculatorMode by remember { mutableStateOf(false) }

    fun press(change: () -> Unit) {
        change()
        display = model.display
    }

    fun enter() {
        press { model.onEquals() }
        val value = model.commitValue
        if (value != null) onEnter(value) else onInvalid()
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

            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(
                        onClick = { calculatorMode = !calculatorMode },
                        modifier = Modifier.testTag("calculatorModeToggle"),
                    ) {
                        Icon(
                            imageVector = if (calculatorMode) Icons.Filled.Dialpad else Icons.Filled.Calculate,
                            contentDescription = if (calculatorMode) "Simple keypad" else "Calculator",
                        )
                    }
                    Text(
                        text = display,
                        style = MaterialTheme.typography.headlineMedium,
                        textAlign = TextAlign.End,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).testTag("calculatorDisplay"),
                    )
                    currencySymbol?.let { symbol ->
                        Text(
                            text = symbol,
                            style = MaterialTheme.typography.titleMedium,
                            modifier = Modifier.padding(start = 8.dp),
                        )
                    }
                }
            }

            val keys = if (calculatorMode) keypad else simpleKeypad
            keys.forEach { row ->
                keyRow(Modifier.weight(1f, fill = false).heightIn(max = keyHeight)) {
                    row.forEach { key ->
                        CalculatorKey(
                            label = key.label,
                            description = key.contentDescription,
                            onClick = { press { key.onPress(model) } },
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Button(
                onClick = { enter() },
                modifier = Modifier.fillMaxWidth().height(56.dp),
            ) {
                Text("Enter")
            }
        }
    }
}

/** Tallest a key row may grow before the keypad hugs the bottom of the screen. */
private val keyHeight = 56.dp

private class KeySpec(
    val label: String,
    val contentDescription: String? = null,
    val onPress: CalculatorModel.() -> Unit,
)

private fun digit(ch: Char) = KeySpec(label = ch.toString()) { onDigit(ch) }

/** The keypad layout, top to bottom. */
private val keypad = listOf(
    listOf(
        digit('7'),
        digit('8'),
        digit('9'),
        KeySpec("÷") { onOperator(CalculatorOp.Divide) },
    ),
    listOf(
        digit('4'),
        digit('5'),
        digit('6'),
        KeySpec("×") { onOperator(CalculatorOp.Multiply) },
    ),
    listOf(
        digit('1'),
        digit('2'),
        digit('3'),
        KeySpec("−") { onOperator(CalculatorOp.Subtract) },
    ),
    listOf(
        KeySpec("C") { onClear() },
        digit('0'),
        KeySpec(".") { onDot() },
        KeySpec("+") { onOperator(CalculatorOp.Add) },
    ),
    listOf(
        KeySpec("⌫", contentDescription = "Backspace") { onBackspace() },
        KeySpec("=") { onEquals() },
    ),
)

/** The simple numeric keypad, top to bottom. */
private val simpleKeypad = listOf(
    listOf(digit('1'), digit('2'), digit('3')),
    listOf(digit('4'), digit('5'), digit('6')),
    listOf(digit('7'), digit('8'), digit('9')),
    listOf(
        KeySpec(".") { onDot() },
        digit('0'),
        KeySpec("C") { onClear() },
        KeySpec("⌫", contentDescription = "Backspace") { onBackspace() },
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
private fun RowScope.CalculatorKey(label: String, onClick: () -> Unit, description: String? = null) {
    val modifier = if (description != null) {
        Modifier.weight(1f).fillMaxHeight().semantics { contentDescription = description }
    } else {
        Modifier.weight(1f).fillMaxHeight()
    }
    FilledTonalButton(
        onClick = onClick,
        modifier = modifier,
    ) {
        Text(label, style = MaterialTheme.typography.titleLarge)
    }
}
