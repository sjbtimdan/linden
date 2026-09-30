package org.sjbtimdan.linden.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.common_clear

/** Trailing icon button of a text field that empties it. */
@Composable
fun ClearIconButton(onClear: () -> Unit) {
    IconButton(onClick = onClear) {
        Icon(Icons.Default.Close, contentDescription = stringResource(Res.string.common_clear))
    }
}
