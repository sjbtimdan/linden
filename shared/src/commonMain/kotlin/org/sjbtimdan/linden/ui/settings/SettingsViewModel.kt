package org.sjbtimdan.linden.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.model.ThemeMode

class SettingsViewModel(
    private val settingsDao: SettingsDao,
    initialTheme: ThemeMode,
) : ViewModel() {
    private val _themeMode = MutableStateFlow(initialTheme)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        viewModelScope.launch {
            settingsDao.setTheme(mode)
        }
    }
}
