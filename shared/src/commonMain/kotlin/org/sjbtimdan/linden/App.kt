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
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import org.jetbrains.compose.resources.stringResource
import org.sjbtimdan.linden.resources.Res
import org.sjbtimdan.linden.resources.nav_entry
import org.sjbtimdan.linden.resources.nav_ledger
import org.sjbtimdan.linden.resources.nav_settings
import org.sjbtimdan.linden.ui.ApplyLanguageOverride
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.BottomNavItem
import org.sjbtimdan.linden.ui.TopLevelPager
import org.sjbtimdan.linden.ui.accounts.AccountListScreen
import org.sjbtimdan.linden.ui.budget.BudgetScreen
import org.sjbtimdan.linden.ui.categories.CategoryListScreen
import org.sjbtimdan.linden.ui.entry.EntryPoint
import org.sjbtimdan.linden.ui.insights.InsightsScreen
import org.sjbtimdan.linden.ui.ledger.LedgerScreen
import org.sjbtimdan.linden.ui.rates.RatesScreen
import org.sjbtimdan.linden.ui.settings.SettingsScreen
import org.sjbtimdan.linden.ui.theme.LindenTheme

sealed class Screen(val key: String) {
    data object Entry : Screen("Entry")
    data object Ledger : Screen("Ledger")
    data object Settings : Screen("Settings")
    data object CategoryList : Screen("CategoryList")
    data object AccountList : Screen("AccountList")
    data object Rates : Screen("Rates")
    data object Budgets : Screen("Budgets")
    data object Insights : Screen("Insights")

    companion object {
        fun fromKey(key: String): Screen = when (key) {
            Entry.key -> Entry
            Ledger.key -> Ledger
            Settings.key -> Settings
            CategoryList.key -> CategoryList
            AccountList.key -> AccountList
            Rates.key -> Rates
            Budgets.key -> Budgets
            Insights.key -> Insights
            else -> Entry
        }
    }
}

internal val ScreenSaver: Saver<Screen, String> = Saver(
    save = { it.key },
    restore = { Screen.fromKey(it) },
)

/** The screens the pager swipes between, in bottom-navigation order. */
internal val topLevelScreens = listOf(Screen.Ledger, Screen.Entry, Screen.Settings)

@Composable
fun App(
    dependencies: AppDependencies,
    // Test seam: desktop has no system back, so the real BackHandler is a no-op
    // and tests inject a handler they can invoke.
    systemBackHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { enabled, onBack ->
        BackHandler(enabled, onBack)
    },
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Entry)
    }
    // The top-level screen a sub-screen was opened from; both the back arrow and
    // the system back gesture return there.
    var subScreenOrigin by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(Screen.Settings)
    }
    val subScreen = currentScreen.takeIf { it !in topLevelScreens }
    val openSubScreen: (Screen) -> Unit = { screen ->
        if (currentScreen in topLevelScreens) {
            subScreenOrigin = currentScreen
        }
        currentScreen = screen
    }
    val navigateBack: () -> Unit = { currentScreen = subScreenOrigin }
    systemBackHandler(subScreen != null) { navigateBack() }
    val settingsViewModel = dependencies.settingsViewModel
    val ratesViewModel = dependencies.ratesViewModel
    val categoryListViewModel = dependencies.categoryListViewModel
    val accountListViewModel = dependencies.accountListViewModel
    val budgetViewModel = dependencies.budgetViewModel
    val insightsViewModel = dependencies.insightsViewModel
    val entryViewModel = dependencies.entryViewModel
    val ledgerViewModel = dependencies.ledgerViewModel
    LaunchedEffect(Unit) {
        ratesViewModel.refreshRatesIfStale(dependencies.initialCurrency)
    }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val ratesWarning by ratesViewModel.ratesWarning.collectAsState()

    // The override must land before the subtree composes, and key(language)
    // forces a full recomposition on change so dates, money and (on desktop)
    // strings re-resolve under the new locale.
    ApplyLanguageOverride(language)
    key(language) {
        LindenTheme(themeMode = themeMode) {
            Scaffold(
                contentWindowInsets = WindowInsets(0, 0, 0, 0),
                bottomBar = {
                    NavigationBar(containerColor = MaterialTheme.colorScheme.surfaceContainer) {
                        BottomNavItem(
                            label = stringResource(Res.string.nav_ledger),
                            icon = Icons.AutoMirrored.Filled.List,
                            selected = currentScreen == Screen.Ledger,
                            onSelect = { currentScreen = Screen.Ledger },
                        )
                        BottomNavItem(
                            label = stringResource(Res.string.nav_entry),
                            icon = Icons.Filled.AddCircle,
                            selected = currentScreen == Screen.Entry,
                            onSelect = { currentScreen = Screen.Entry },
                        )
                        BottomNavItem(
                            label = stringResource(Res.string.nav_settings),
                            icon = Icons.Default.Settings,
                            selected = currentScreen == Screen.Settings,
                            onSelect = { currentScreen = Screen.Settings },
                        )
                    }
                },
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .padding(innerPadding)
                        .fillMaxSize(),
                ) {
                    // Top-level screens live in a pager that owns swiping; the
                    // outer fade only runs when entering or leaving a sub-screen,
                    // so a swipe never cross-fades on top of its own slide.
                    AnimatedContent(
                        targetState = subScreen,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(220)) togetherWith
                                fadeOut(animationSpec = tween(120))
                        },
                        label = "screenTransition",
                    ) { screen ->
                        when (screen) {
                            null -> TopLevelPager(
                                screen = currentScreen,
                                onScreenChange = { currentScreen = it },
                                modifier = Modifier.fillMaxSize(),
                            ) { page ->
                                when (page) {
                                    Screen.Ledger -> LedgerScreen(
                                        viewModel = ledgerViewModel,
                                        onNavigateToEntry = { currentScreen = Screen.Entry },
                                        onNavigateToAccounts = { openSubScreen(Screen.AccountList) },
                                        onNavigateToCategories = { openSubScreen(Screen.CategoryList) },
                                    )

                                    Screen.Entry -> EntryPoint(
                                        viewModel = entryViewModel,
                                        onNavigateToRates = { openSubScreen(Screen.Rates) },
                                        ratesWarning = ratesWarning,
                                    )

                                    else -> SettingsScreen(
                                        viewModel = settingsViewModel,
                                        onNavigateToCategories = { openSubScreen(Screen.CategoryList) },
                                        onNavigateToAccounts = { openSubScreen(Screen.AccountList) },
                                        onNavigateToRates = { openSubScreen(Screen.Rates) },
                                        onNavigateToBudgets = { openSubScreen(Screen.Budgets) },
                                        onNavigateToInsights = { openSubScreen(Screen.Insights) },
                                    )
                                }
                            }

                            Screen.CategoryList -> CategoryListScreen(
                                viewModel = categoryListViewModel,
                                onNavigateBack = navigateBack,
                            )

                            Screen.AccountList -> AccountListScreen(
                                viewModel = accountListViewModel,
                                onNavigateBack = navigateBack,
                            )

                            Screen.Rates -> RatesScreen(
                                viewModel = ratesViewModel,
                                onNavigateBack = navigateBack,
                            )

                            Screen.Budgets -> BudgetScreen(
                                viewModel = budgetViewModel,
                                onNavigateBack = navigateBack,
                            )

                            Screen.Insights -> InsightsScreen(
                                viewModel = insightsViewModel,
                                onNavigateBack = navigateBack,
                            )

                            else -> Unit
                        }
                    }
                }
            }
        }
    }
}
