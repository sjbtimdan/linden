package org.sjbtimdan.linden.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.log.AppFailure
import org.sjbtimdan.linden.log.AppLogger
import org.sjbtimdan.linden.log.NoOpAppLogger
import org.sjbtimdan.linden.util.stateFlow

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
    private val logger: AppLogger = NoOpAppLogger,
) {
    // Chrome state read synchronously while masking totals; it is a single-row
    // setting, so it stays live even between screens instead of going stale.
    val state: StateFlow<Boolean> =
        settingsDao.hideEntryTotalFlow().stateFlow(scope, initial, SharingStarted.Eagerly)

    fun set(hidden: Boolean) {
        scope.launch {
            try {
                settingsDao.setHideEntryTotal(hidden)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.report(AppFailure.WriteFailed, e)
            }
        }
    }
}
