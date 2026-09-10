package org.sjbtimdan.linden

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Owns the [AppDependencies] composition root for the lifetime of the host
 * (the Android activity, retained across configuration changes). Keeping them
 * in a ViewModel means the ViewModels they contain — and the in-progress entry
 * draft — survive rotation instead of being rebuilt from scratch.
 */
class AppRootViewModel(
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
                AppRootState.Ready(createDependencies())
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (e: Exception) {
                AppRootState.Failed
            }
        }
    }

    override fun onCleared() {
        (_state.value as? AppRootState.Ready)?.dependencies?.close()
    }
}

/** Lifecycle of the app's composition root. */
sealed interface AppRootState {
    data object Loading : AppRootState
    data object Failed : AppRootState
    data class Ready(val dependencies: AppDependencies) : AppRootState
}
