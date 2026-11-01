package org.sjbtimdan.linden.ui.settings

import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.backup.RestoreResult
import org.sjbtimdan.linden.data.HideEntryTotalSetting
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.export.CsvExportManager
import org.sjbtimdan.linden.imports.IvyImportResult
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.AppViewModel
import java.io.InputStream
import java.io.OutputStream

/** State of a backup, restore, export or import operation; [T] is the success payload. */
sealed interface BackupState<out T> {
    data object Idle : BackupState<Nothing>
    data object Working : BackupState<Nothing>
    data class Success<T>(val value: T) : BackupState<T>
    data class Error(val message: String?) : BackupState<Nothing>
}

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val importer: IvyImporter,
    private val backupManager: LindenBackupManager,
    private val csvExporter: CsvExportManager,
    initialTheme: ThemeMode,
    initialCurrency: Currency,
    initialHideEntryTotal: Boolean = false,
    initialLanguage: AppLanguage = AppLanguage.SYSTEM,
) : AppViewModel() {
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(initialCurrency)
    val defaultCurrency: StateFlow<Currency> = _defaultCurrency.asStateFlow()

    private val _language = MutableStateFlow(initialLanguage)
    val language: StateFlow<AppLanguage> = _language.asStateFlow()

    private val hideEntryTotalSetting = HideEntryTotalSetting(settingsDao, initialHideEntryTotal, viewModelScope)

    val hideEntryTotal: StateFlow<Boolean> = hideEntryTotalSetting.state

    init {
        // Keep theme, language and currency in sync with the settings table: the
        // first-run screen writes them before the app is shown, and a restored
        // backup can change them too. The optimistic updates in [setThemeMode],
        // [setLanguage] and [setDefaultCurrency] still apply instantly.
        viewModelScope.launch {
            settingsDao.themeFlow().collect { _themeMode.value = it }
        }
        viewModelScope.launch {
            settingsDao.languageFlow().collect { _language.value = it }
        }
        viewModelScope.launch {
            settingsDao.defaultCurrencyFlow().collect { _defaultCurrency.value = it }
        }
    }

    private val _importState = MutableStateFlow<BackupState<IvyImportResult>>(BackupState.Idle)
    val importState: StateFlow<BackupState<IvyImportResult>> = _importState.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState<Unit>>(BackupState.Idle)
    val backupState: StateFlow<BackupState<Unit>> = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<BackupState<RestoreResult>>(BackupState.Idle)
    val restoreState: StateFlow<BackupState<RestoreResult>> = _restoreState.asStateFlow()

    private val _exportState = MutableStateFlow<BackupState<Unit>>(BackupState.Idle)
    val exportState: StateFlow<BackupState<Unit>> = _exportState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) = setOptimistic(_themeMode, mode, settingsDao::setTheme)

    fun setDefaultCurrency(currency: Currency) =
        setOptimistic(_defaultCurrency, currency, settingsDao::setDefaultCurrency)

    fun setLanguage(language: AppLanguage) = setOptimistic(_language, language, settingsDao::setLanguage)

    fun setHideEntryTotal(hidden: Boolean) = hideEntryTotalSetting.set(hidden)

    fun importIvy(input: InputStream) = runOperation(_importState) {
        withContext(Dispatchers.IO) { importer.import(input) }
    }

    fun clearImportState() {
        _importState.value = BackupState.Idle
    }

    fun backupTo(output: OutputStream) = runOperation(_backupState) {
        withContext(Dispatchers.IO) { backupManager.backupTo(output) }
    }

    fun restoreFrom(input: InputStream) = runOperation(_restoreState) {
        val result = withContext(Dispatchers.IO) { backupManager.restoreFrom(input) }
        reloadSettings()
        result
    }

    /** Re-reads theme, currency and language so the UI reflects a restored backup. */
    private suspend fun reloadSettings() {
        _themeMode.value = settingsDao.getTheme()
        _defaultCurrency.value = settingsDao.getDefaultCurrency()
        _language.value = settingsDao.getLanguage()
    }

    fun exportCsv(output: OutputStream) = runOperation(_exportState) {
        withContext(Dispatchers.IO) { csvExporter.exportTo(output) }
    }

    fun clearExportState() {
        _exportState.value = BackupState.Idle
    }

    fun clearBackupState() {
        _backupState.value = BackupState.Idle
    }

    fun clearRestoreState() {
        _restoreState.value = BackupState.Idle
    }

    private fun <T> runOperation(state: MutableStateFlow<BackupState<T>>, block: suspend () -> T) {
        viewModelScope.launch {
            state.value = BackupState.Working
            state.value = try {
                BackupState.Success(block())
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BackupState.Error(e.message)
            }
        }
    }

    private fun <T> setOptimistic(state: MutableStateFlow<T>, value: T, persist: suspend (T) -> Unit) {
        state.value = value
        viewModelScope.launch {
            try {
                persist(value)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // The optimistic state is already propagated.
            }
        }
    }
}
