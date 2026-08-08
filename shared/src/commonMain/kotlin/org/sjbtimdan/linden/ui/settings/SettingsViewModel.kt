package org.sjbtimdan.linden.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    initialTheme: ThemeMode,
    initialCurrency: Currency,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _defaultCurrency = MutableStateFlow(initialCurrency)
    val defaultCurrency: StateFlow<Currency> = _defaultCurrency.asStateFlow()

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
}
