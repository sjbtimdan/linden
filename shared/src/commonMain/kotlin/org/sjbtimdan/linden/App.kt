package org.sjbtimdan.linden

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.accounts.AccountListScreen
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListScreen
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.ledger.LedgerScreen
import org.sjbtimdan.linden.ui.ledger.LedgerViewModel
import org.sjbtimdan.linden.ui.settings.SettingsScreen
import org.sjbtimdan.linden.ui.settings.SettingsViewModel
import org.sjbtimdan.linden.ui.theme.LindenTheme

sealed class Screen {
    data object Ledger : Screen()
    data object Settings : Screen()
    data object CategoryList : Screen()
    data object AccountList : Screen()
}

@Composable
fun App(
    database: LindenDatabase,
    initialTheme: ThemeMode,
    initialCurrency: Currency,
) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Ledger) }
    val settingsDao = remember { SettingsDao(database.settingsQueries) }
    val settingsViewModel = remember { SettingsViewModel(settingsDao, initialTheme, initialCurrency) }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val categoryDao = remember { CategoryDao(database.categoryQueries) }
    val categoryListViewModel = remember { CategoryListViewModel(categoryDao) }
    val accountDao = remember { AccountDao(database.accountQueries) }
    val accountListViewModel = remember { AccountListViewModel(accountDao) }
    val entryDao = remember { EntryDao(database.entryQueries) }
    val ledgerViewModel = remember { LedgerViewModel(entryDao, accountDao, categoryDao) }

    LindenTheme(themeMode = themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Ledger,
                        onClick = { currentScreen = Screen.Ledger },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Ledger") },
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Settings,
                        onClick = { currentScreen = Screen.Settings },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Settings,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Settings") },
                    )
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .padding(innerPadding)
                    .fillMaxSize(),
            ) {
                when (currentScreen) {
                    Screen.Ledger -> LedgerScreen(
                        viewModel = ledgerViewModel,
                        onNavigateToSettings = { currentScreen = Screen.Settings },
                    )

                    Screen.Settings -> SettingsScreen(
                        viewModel = settingsViewModel,
                        onNavigateToCategories = { currentScreen = Screen.CategoryList },
                        onNavigateToAccounts = { currentScreen = Screen.AccountList },
                    )

                    Screen.CategoryList -> CategoryListScreen(
                        viewModel = categoryListViewModel,
                        onNavigateBack = { currentScreen = Screen.Settings },
                    )

                    Screen.AccountList -> AccountListScreen(
                        viewModel = accountListViewModel,
                        onNavigateBack = { currentScreen = Screen.Settings },
                    )
                }
            }
        }
    }
}
