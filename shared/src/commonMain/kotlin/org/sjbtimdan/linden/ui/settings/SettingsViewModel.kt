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
    private val settingsDao: SettingsDao
) : ViewModel() {
    private val _themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()
    private var userSelected = false

    init {
        viewModelScope.launch {
            val saved = settingsDao.getTheme()
            if (!userSelected) {
                _themeMode.value = saved
            }
        }
    }

    fun setThemeMode(mode: ThemeMode) {
        userSelected = true
        _themeMode.value = mode
        viewModelScope.launch {
            settingsDao.setTheme(mode)
        }
    }
}
