package org.sjbtimdan.linden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag

/**
 * Bootstraps [App]: builds the [AppDependencies] asynchronously (the factory is
 * expected to hop to a background dispatcher) and shows a loading indicator
 * until they are ready, so startup never blocks the UI thread on schema
 * creation or the initial settings reads.
 */
@Composable
fun AppRoot(
    createDependencies: suspend () -> AppDependencies,
) {
    var dependencies by remember { mutableStateOf<AppDependencies?>(null) }
    LaunchedEffect(Unit) {
        dependencies = createDependencies()
    }

    val deps = dependencies
    if (deps == null) {
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("loading"))
            }
        }
    } else {
        App(deps)
    }
}
