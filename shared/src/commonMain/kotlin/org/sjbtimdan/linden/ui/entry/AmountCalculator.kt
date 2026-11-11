package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
        // Keys cap at keyHeight; the keypad fills the available height and hugs
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

            CalculatorKeypad(
                calculatorMode = calculatorMode,
                decimalDigits = decimalDigits,
                onPress = { action -> press { action(model) } },
            )

            Spacer(modifier = Modifier.height(8.dp))

            CalculatorActions(enter = { enter() })
        }
    }
}
