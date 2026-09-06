package org.sjbtimdan.linden.data

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Reactive holder for the "Hide totals" privacy setting, shared by the settings
 * screen and the entry/ledger screens. The state is seeded with [initial] so a
 * masked total never flashes before the database flow emits; [set] persists the
 * value and the database flow propagates it back to [state].
 */
class HideEntryTotalSetting(
    private val settingsDao: SettingsDao,
    initial: Boolean,
    private val scope: CoroutineScope,
) {
    val state: StateFlow<Boolean> = settingsDao.hideEntryTotalFlow()
        .stateIn(scope = scope, started = SharingStarted.Eagerly, initialValue = initial)

    fun set(hidden: Boolean) {
        scope.launch {
            settingsDao.setHideEntryTotal(hidden)
        }
    }
}
