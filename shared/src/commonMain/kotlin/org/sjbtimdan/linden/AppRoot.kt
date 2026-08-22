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
import kotlinx.coroutines.CancellationException
import org.sjbtimdan.linden.ui.StartupError

/**
 * Bootstraps [App]: builds the [AppDependencies] asynchronously (the factory is
 * expected to hop to a background dispatcher) and shows a loading indicator
 * until they are ready, so startup never blocks the UI thread on schema
 * creation or the initial settings reads. If dependency creation fails (e.g. a
 * corrupt DB), an error screen with a retry action is shown instead of loading
 * forever.
 */
@Composable
fun AppRoot(createDependencies: suspend () -> AppDependencies) {
    var dependencies by remember { mutableStateOf<AppDependencies?>(null) }
    var loadFailed by remember { mutableStateOf(false) }
    var attempt by remember { mutableStateOf(0) }
    LaunchedEffect(attempt) {
        dependencies = null
        loadFailed = false
        try {
            dependencies = createDependencies()
        } catch (cancellation: CancellationException) {
            throw cancellation
        } catch (e: Exception) {
            loadFailed = true
        }
    }

    val deps = dependencies
    if (deps != null) {
        App(deps)
    } else if (loadFailed) {
        MaterialTheme {
            StartupError(onRetry = { attempt++ })
        }
    } else {
        MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("loading"))
            }
        }
    }
}
