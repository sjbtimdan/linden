package org.sjbtimdan.linden.ui.entry

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle

/**
 * Renders [parts] as one Text, so semantics and tests still see the exact
 * amount while the fraction and compact suffix can use a smaller, gold span.
 */
@Composable
internal fun HeroAmountText(
    parts: HeroAmountParts,
    wholeStyle: TextStyle,
    fractionStyle: TextStyle,
    contentColor: Color,
    modifier: Modifier = Modifier,
) {
    val fractionSpan = fractionStyle.toSpanStyle()
    Text(
        text = buildAnnotatedString {
            append(parts.whole)
            parts.fraction?.let { withStyle(fractionSpan) { append(it) } }
            parts.unitSuffix?.let { withStyle(fractionSpan) { append(it) } }
        },
        modifier = modifier,
        style = wholeStyle,
        color = contentColor,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
    )
}
