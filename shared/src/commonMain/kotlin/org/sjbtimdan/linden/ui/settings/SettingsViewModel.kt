package org.sjbtimdan.linden.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.backup.RestoreResult
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.imports.IvyImportResult
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import java.io.InputStream
import java.io.OutputStream

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Success(val result: IvyImportResult) : ImportState
    data class Error(val message: String) : ImportState
}

/** State of a backup or restore operation; [T] is the success payload. */
sealed interface BackupState<out T> {
    data object Idle : BackupState<Nothing>
    data object Working : BackupState<Nothing>
    data class Success<T>(val value: T) : BackupState<T>
    data class Error(val message: String) : BackupState<Nothing>
}

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val importer: IvyImporter,
    private val backupManager: LindenBackupManager,
    initialTheme: ThemeMode,
    initialCurrency: Currency,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(initialCurrency)
    val defaultCurrency: StateFlow<Currency> = _defaultCurrency.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

    private val _backupState = MutableStateFlow<BackupState<Unit>>(BackupState.Idle)
    val backupState: StateFlow<BackupState<Unit>> = _backupState.asStateFlow()

    private val _restoreState = MutableStateFlow<BackupState<RestoreResult>>(BackupState.Idle)
    val restoreState: StateFlow<BackupState<RestoreResult>> = _restoreState.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            settingsDao.setTheme(mode)
        }
    }

    fun setDefaultCurrency(currency: Currency) {
        _defaultCurrency.value = currency
        viewModelScope.launch {
            settingsDao.setDefaultCurrency(currency)
        }
    }

    fun importIvy(input: InputStream) {
        viewModelScope.launch {
            _importState.update { ImportState.Importing }
            _importState.update {
                try {
                    ImportState.Success(withContext(Dispatchers.IO) { importer.import(input) })
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    ImportState.Error(e.message ?: "Import failed")
                }
            }
        }
    }

    fun clearImportState() {
        _importState.update { ImportState.Idle }
    }

    fun backupTo(output: OutputStream) {
        viewModelScope.launch {
            _backupState.update { BackupState.Working }
            _backupState.update {
                try {
                    withContext(Dispatchers.IO) { backupManager.backupTo(output) }
                    BackupState.Success(Unit)
                } catch (e: CancellationException) {
                    throw e
                } catch (e: Exception) {
                    BackupState.Error(e.message ?: "Backup failed")
                }
            }
        }
    }

    fun restoreFrom(input: InputStream) {
        viewModelScope.launch {
            _restoreState.update { BackupState.Working }
            val state = try {
                val result = withContext(Dispatchers.IO) { backupManager.restoreFrom(input) }
                reloadSettings()
                BackupState.Success(result)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                BackupState.Error(e.message ?: "Restore failed")
            }
            _restoreState.update { state }
        }
    }

    /** Re-reads theme and currency so the UI reflects a restored backup. */
    private suspend fun reloadSettings() {
        _themeMode.value = settingsDao.getTheme()
        _defaultCurrency.value = settingsDao.getDefaultCurrency()
    }

    fun clearBackupState() {
        _backupState.update { BackupState.Idle }
    }

    fun clearRestoreState() {
        _restoreState.update { BackupState.Idle }
    }
}
