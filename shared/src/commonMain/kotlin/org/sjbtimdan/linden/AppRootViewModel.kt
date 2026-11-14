package org.sjbtimdan.linden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.log.AppFailure
import org.sjbtimdan.linden.log.AppLogger
import org.sjbtimdan.linden.log.NoOpAppLogger
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode

/**
 * Owns the [AppDependencies] composition root for the lifetime of the host
 * (the Android activity, retained across configuration changes). Keeping them
 * in a ViewModel means the ViewModels they contain — and the in-progress entry
 * draft — survive rotation instead of being rebuilt from scratch.
 */
class AppRootViewModel(
    private val appLogger: AppLogger = NoOpAppLogger,
    private val createDependencies: suspend () -> AppDependencies,
) : ViewModel() {
    private val _state = MutableStateFlow<AppRootState>(AppRootState.Loading)
    val state: StateFlow<AppRootState> = _state.asStateFlow()

    init {
        load()
    }

    /** Retries dependency creation after a failure. */
    fun retry() = load()

    private fun load() {
        _state.value = AppRootState.Loading
        viewModelScope.launch {
            _state.value = try {
                val dependencies = createDependencies()
                if (dependencies.firstRun) {
                    AppRootState.FirstRun(dependencies)
                } else {
                    AppRootState.Ready(dependencies)
                }
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                appLogger.report(AppFailure.Startup, e)
                AppRootState.Failed
            }
        }
    }

    /**
     * Completes the first-run setup with the chosen language, currency and
     * theme, then shows the app. When the user chose to pick capture apps from
     * the last step, the app opens Payment capture instead of Entry, so the
     * allowlist they just read about is immediately in front of them; back from
     * there returns to Entry, which confirms the finished setup. The
     * dependencies are reused: only the settings write and the deferred
     * seeding happen here.
     */
    fun completeFirstRun(
        language: AppLanguage,
        currency: Currency,
        theme: ThemeMode,
        openCaptureSettings: Boolean = false,
    ) {
        val dependencies = (_state.value as? AppRootState.FirstRun)?.dependencies ?: return
        viewModelScope.launch {
            try {
                withContext(Dispatchers.IO) { dependencies.completeFirstRun(language, currency, theme) }
                _state.value = AppRootState.Ready(
                    dependencies,
                    startInCaptureSettings = openCaptureSettings,
                    justCompletedSetup = true,
                )
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Stay on the first-run screen so the user can try again; the
                // write that failed is worth reporting on its own.
                appLogger.report(AppFailure.Startup, e)
            }
        }
    }

    override fun onCleared() {
        when (val current = _state.value) {
            is AppRootState.Ready -> current.dependencies.close()
            is AppRootState.FirstRun -> current.dependencies.close()
            else -> Unit
        }
    }
}

/** Lifecycle of the app's composition root. */
sealed interface AppRootState {
    data object Loading : AppRootState
    data object Failed : AppRootState
    data class Ready(
        val dependencies: AppDependencies,
        /** The app opens Payment capture first, offered by the first-run wizard. */
        val startInCaptureSettings: Boolean = false,
        /**
         * Built by [AppRootViewModel.completeFirstRun]: the start screen says so
         * once. False on a normal start, so the confirmation never replays.
         */
        val justCompletedSetup: Boolean = false,
    ) : AppRootState

    /** Dependencies built but seeding deferred: the user must pick a currency first. */
    data class FirstRun(val dependencies: AppDependencies) : AppRootState
}
