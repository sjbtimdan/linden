package org.sjbtimdan.linden.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import java.io.InputStream
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.imports.IvyImportResult
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode

sealed interface ImportState {
    data object Idle : ImportState
    data object Importing : ImportState
    data class Success(val result: IvyImportResult) : ImportState
    data class Error(val message: String) : ImportState
}

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    private val importer: IvyImporter,
    initialTheme: ThemeMode,
    initialCurrency: Currency,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(initialCurrency)
    val defaultCurrency: StateFlow<Currency> = _defaultCurrency.asStateFlow()

    private val _importState = MutableStateFlow<ImportState>(ImportState.Idle)
    val importState: StateFlow<ImportState> = _importState.asStateFlow()

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
}
