package org.sjbtimdan.linden

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.ui.settings.SettingsScreen
import org.sjbtimdan.linden.ui.settings.SettingsViewModel
import org.sjbtimdan.linden.ui.theme.LindenTheme

@Composable
fun App(database: LindenDatabase) {
    val settingsDao = remember { SettingsDao(database.settingsQueries) }
    val viewModel = remember { SettingsViewModel(settingsDao) }
    val themeMode by viewModel.themeMode.collectAsState()

    LindenTheme(themeMode = themeMode) {
        SettingsScreen(viewModel)
    }
}
