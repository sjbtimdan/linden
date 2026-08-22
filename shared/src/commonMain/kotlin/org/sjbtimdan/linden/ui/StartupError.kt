package org.sjbtimdan.linden.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp

@Composable
fun StartupError(onRetry: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .testTag("startupError"),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = "Linden failed to start",
                style = MaterialTheme.typography.titleMedium,
            )
            Text(
                text = "An error occurred while opening your data.",
                style = MaterialTheme.typography.bodyMedium,
            )
            Button(onClick = onRetry, modifier = Modifier.testTag("retry")) {
                Text("Retry")
            }
        }
    }
}
