package org.sjbtimdan.linden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.viewmodel.compose.viewModel
import org.sjbtimdan.linden.ui.FirstRunScreen
import org.sjbtimdan.linden.ui.StartupError

/**
 * Bootstraps [App]: builds the [AppDependencies] asynchronously (the factory is
 * expected to hop to a background dispatcher) and shows a loading indicator
 * until they are ready, so startup never blocks the UI thread on schema
 * creation or the initial settings reads. If dependency creation fails (e.g. a
 * corrupt DB), an error screen with a retry action is shown instead of loading
 * forever. The dependencies are held by [AppRootViewModel], so they survive
 * configuration changes instead of being rebuilt.
 */
@Composable
fun AppRoot(createDependencies: suspend () -> AppDependencies) {
    val viewModel: AppRootViewModel = viewModel { AppRootViewModel(createDependencies) }
    val state by viewModel.state.collectAsState()

    when (val current = state) {
        is AppRootState.Ready -> App(current.dependencies)

        is AppRootState.FirstRun -> MaterialTheme {
            FirstRunScreen(onComplete = viewModel::completeFirstRun)
        }

        AppRootState.Failed -> MaterialTheme {
            StartupError(onRetry = viewModel::retry)
        }

        AppRootState.Loading -> MaterialTheme {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(modifier = Modifier.testTag("loading"))
            }
        }
    }
}
