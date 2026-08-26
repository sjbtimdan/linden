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
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
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
 * Full calculator keypad for amount entry. Replaces the amount text field while
 * open: the display line shows the running value, "Enter" commits a valid
 * positive amount (or reports an invalid one via [onInvalid]), Escape or the
 * system back discards the edit. The screen's back arrow is the visible way out.
 */
@Composable
fun AmountCalculator(
    modifier: Modifier = Modifier,
    initialMinor: Long?,
    currencySymbol: String?,
    onEnter: (String) -> Unit,
    onInvalid: () -> Unit,
    onCancel: () -> Unit,
) {
    val model = remember(initialMinor) { CalculatorModel(initialMinor) }
    var display by remember { mutableStateOf(model.display) }

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
        // Keys cap out at [keyHeight] so they never stretch; the keypad fills
        // the available height and hugs the bottom of the screen for one-handed
        // use. When the height is unbounded the keypad gets a fixed height
        // instead, so it can't grow without limit.
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
            Box(modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
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

            keypad.forEach { row ->
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
