package org.sjbtimdan.linden

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.sjbtimdan.linden.ui.accounts.AccountListScreen
import org.sjbtimdan.linden.ui.budget.BudgetScreen
import org.sjbtimdan.linden.ui.categories.CategoryListScreen
import org.sjbtimdan.linden.ui.entry.EntryPoint
import org.sjbtimdan.linden.ui.ledger.LedgerScreen
import org.sjbtimdan.linden.ui.rates.RatesScreen
import org.sjbtimdan.linden.ui.settings.SettingsScreen
import org.sjbtimdan.linden.ui.theme.LindenTheme

sealed class Screen {
    data object Entry : Screen()
    data object Ledger : Screen()
    data object Settings : Screen()
    data object CategoryList : Screen()
    data object AccountList : Screen()
    data object Rates : Screen()
    data object Budgets : Screen()
}

@Composable
fun App(dependencies: AppDependencies) {
    var currentScreen by remember { mutableStateOf<Screen>(Screen.Entry) }
    val settingsViewModel = dependencies.settingsViewModel
    val ratesViewModel = dependencies.ratesViewModel
    val categoryListViewModel = dependencies.categoryListViewModel
    val accountListViewModel = dependencies.accountListViewModel
    val budgetViewModel = dependencies.budgetViewModel
    val entryViewModel = dependencies.entryViewModel
    val ledgerViewModel = dependencies.ledgerViewModel
    DisposableEffect(dependencies.httpClient) {
        onDispose { dependencies.httpClient.close() }
    }
    LaunchedEffect(Unit) {
        // Refresh rates at startup only when the cached rates are more than 24 hours old.
        ratesViewModel.refreshRatesIfStale(dependencies.initialCurrency)
    }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val ratesWarning by ratesViewModel.ratesWarning.collectAsState()

    LindenTheme(themeMode = themeMode) {
        Scaffold(
            contentWindowInsets = WindowInsets(0, 0, 0, 0),
            bottomBar = {
                NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                    NavigationBarItem(
                        selected = currentScreen == Screen.Ledger,
                        onClick = { currentScreen = Screen.Ledger },
                        icon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Ledger") },
                    )
                    NavigationBarItem(
                        selected = currentScreen == Screen.Entry,
                        onClick = { currentScreen = Screen.Entry },
                        icon = {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.List,
                                contentDescription = null,
                            )
                        },
                        label = { Text("Entry") },
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
                AnimatedContent(
                    targetState = currentScreen,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(220)) togetherWith
                            fadeOut(animationSpec = tween(120))
                    },
                    label = "screenTransition",
                ) { screen ->
                    when (screen) {
                        Screen.Entry -> EntryPoint(
                            viewModel = entryViewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                            onNavigateToRates = { currentScreen = Screen.Rates },
                            ratesWarning = ratesWarning,
                        )

                        Screen.Ledger -> LedgerScreen(
                            viewModel = ledgerViewModel,
                            onNavigateToSettings = { currentScreen = Screen.Settings },
                        )

                        Screen.Settings -> SettingsScreen(
                            viewModel = settingsViewModel,
                            onNavigateToCategories = { currentScreen = Screen.CategoryList },
                            onNavigateToAccounts = { currentScreen = Screen.AccountList },
                            onNavigateToRates = { currentScreen = Screen.Rates },
                            onNavigateToBudgets = { currentScreen = Screen.Budgets },
                        )

                        Screen.CategoryList -> CategoryListScreen(
                            viewModel = categoryListViewModel,
                            onNavigateBack = { currentScreen = Screen.Settings },
                        )

                        Screen.AccountList -> AccountListScreen(
                            viewModel = accountListViewModel,
                            onNavigateBack = { currentScreen = Screen.Settings },
                        )

                        Screen.Rates -> RatesScreen(
                            viewModel = ratesViewModel,
                            onNavigateBack = { currentScreen = Screen.Settings },
                        )

                        Screen.Budgets -> BudgetScreen(
                            viewModel = budgetViewModel,
                            onNavigateBack = { currentScreen = Screen.Settings },
                        )
                    }
                }
            }
        }
    }
}
