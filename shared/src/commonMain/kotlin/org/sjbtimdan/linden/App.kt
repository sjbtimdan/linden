package org.sjbtimdan.linden

import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.ui.categories.CategoryListScreen
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.settings.SettingsScreen
import org.sjbtimdan.linden.ui.settings.SettingsViewModel
import org.sjbtimdan.linden.ui.theme.LindenTheme

sealed class Screen {
    data object Settings : Screen()
    data object CategoryList : Screen()
}

@Composable
fun App(database: LindenDatabase) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Settings) }
    val settingsDao = remember { SettingsDao(database.settingsQueries) }
    val settingsViewModel = remember { SettingsViewModel(settingsDao) }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val categoryDao = remember { CategoryDao(database.categoryQueries) }
    val categoryListViewModel = remember { CategoryListViewModel(categoryDao) }

    LindenTheme(themeMode = themeMode) {
        when (currentScreen) {
            Screen.Settings -> SettingsScreen(
                viewModel = settingsViewModel,
                onNavigateToCategories = { currentScreen = Screen.CategoryList },
            )

            Screen.CategoryList -> CategoryListScreen(
                viewModel = categoryListViewModel,
                onNavigateBack = { currentScreen = Screen.Settings },
            )
        }
    }
}
