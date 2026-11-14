package org.sjbtimdan.linden

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import org.sjbtimdan.linden.ui.ApplyLanguageOverride
import org.sjbtimdan.linden.ui.BackHandler
import org.sjbtimdan.linden.ui.FloatingNavigationBar
import org.sjbtimdan.linden.ui.FloatingNavigationBarContentGap
import org.sjbtimdan.linden.ui.FloatingNavigationBarHeight
import org.sjbtimdan.linden.ui.FloatingNavigationBarMargin
import org.sjbtimdan.linden.ui.LocalBottomBarContentPadding
import org.sjbtimdan.linden.ui.TopLevelPager
import org.sjbtimdan.linden.ui.accounts.AccountListScreen
import org.sjbtimdan.linden.ui.budget.BudgetScreen
import org.sjbtimdan.linden.ui.capture.CaptureSettingsScreen
import org.sjbtimdan.linden.ui.capture.rememberCapturableApps
import org.sjbtimdan.linden.ui.capture.rememberNotificationAccess
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
    data object CaptureSettings : Screen("CaptureSettings")

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
            CaptureSettings.key -> CaptureSettings
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
    // The first-run wizard's "Choose apps now" opens Payment capture instead of
    // the start screen; only the first composition reads it, so a later
    // rotation keeps wherever the user has navigated.
    startInCaptureSettings: Boolean = false,
    // First-run setup just finished: the start screen confirms it once.
    setupComplete: Boolean = false,
    // Test seam: desktop has no system back, so the real BackHandler is a no-op
    // and tests inject a handler they can invoke.
    systemBackHandler: @Composable (enabled: Boolean, onBack: () -> Unit) -> Unit = { enabled, onBack ->
        BackHandler(enabled, onBack)
    },
) {
    var currentScreen by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(if (startInCaptureSettings) Screen.CaptureSettings else Screen.Entry)
    }
    // The top-level screen a sub-screen was opened from; both the back arrow and
    // the system back gesture return there. The first-run shortcut opens capture
    // before any top-level screen was visited, and Entry is what finishing setup
    // leads to, so back from there lands on the start screen instead of Settings.
    var subScreenOrigin by rememberSaveable(stateSaver = ScreenSaver) {
        mutableStateOf<Screen>(if (startInCaptureSettings) Screen.Entry else Screen.Settings)
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
    // Only the chrome and the start screen's ViewModels are touched here; the
    // other screens create theirs when their branch first composes, so a screen
    // that was never opened starts no database or aggregation subscriptions.
    val settingsViewModel = dependencies.settingsViewModel
    val ratesViewModel = dependencies.ratesViewModel
    val capturedPaymentsViewModel = dependencies.capturedPaymentsViewModel
    val entryViewModel = dependencies.entryViewModel
    LaunchedEffect(Unit) {
        ratesViewModel.refreshRatesIfStale(dependencies.initialCurrency)
    }
    // The ledger's entry dialog lives in its ViewModel, which outlasts the
    // screen, so leaving the ledger finalizes it instead of it reopening on the
    // next visit. Keyed on the screen rather than a DisposableEffect in
    // LedgerScreen: a rotation recomposes the tree with the same screen and
    // must keep an in-progress draft.
    LaunchedEffect(currentScreen) {
        if (currentScreen != Screen.Ledger) dependencies.ledgerViewModelIfCreated()?.dismissDialog()
    }
    val themeMode by settingsViewModel.themeMode.collectAsState()
    val language by settingsViewModel.language.collectAsState()
    val ratesWarning by ratesViewModel.ratesWarning.collectAsState()

    // The override must land before the subtree composes, and key(language)
    // forces a full recomposition on change so dates, money and (on desktop)
    // strings re-resolve under the new locale.
    ApplyLanguageOverride(language)
    val hazeState = rememberHazeState()
    val navigationBarInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val bottomContentPadding = FloatingNavigationBarHeight +
        FloatingNavigationBarMargin +
        FloatingNavigationBarContentGap +
        navigationBarInset
    val subScreenSlide = with(LocalDensity.current) { 24.dp.roundToPx() }
    key(language) {
        LindenTheme(themeMode = themeMode) {
            CompositionLocalProvider(LocalBottomBarContentPadding provides bottomContentPadding) {
                Box(modifier = Modifier.fillMaxSize()) {
                    // Top-level screens live in a pager that owns swiping; the
                    // outer slide/fade only runs when entering or leaving a
                    // sub-screen, so a swipe never cross-fades on its own slide.
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .hazeSource(hazeState),
                    ) {
                        AnimatedContent(
                            targetState = subScreen,
                            transitionSpec = {
                                val direction = if (targetState != null) 1 else -1
                                val enter = slideInHorizontally(animationSpec = tween(220)) {
                                    direction * subScreenSlide
                                } + fadeIn(animationSpec = tween(220))
                                val exit = slideOutHorizontally(animationSpec = tween(180)) {
                                    -direction * subScreenSlide
                                } + fadeOut(animationSpec = tween(120))
                                enter togetherWith exit
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
                                            viewModel = dependencies.ledgerViewModel,
                                            onNavigateToEntry = { currentScreen = Screen.Entry },
                                            onNavigateToAccounts = { openSubScreen(Screen.AccountList) },
                                            onNavigateToCategories = { openSubScreen(Screen.CategoryList) },
                                        )

                                        Screen.Entry -> EntryPoint(
                                            viewModel = entryViewModel,
                                            onNavigateToRates = { openSubScreen(Screen.Rates) },
                                            onNavigateToInsights = { openSubScreen(Screen.Insights) },
                                            ratesWarning = ratesWarning,
                                            captureViewModel = capturedPaymentsViewModel,
                                            setupComplete = setupComplete,
                                        )

                                        else -> SettingsScreen(
                                            viewModel = settingsViewModel,
                                            onNavigateToCategories = { openSubScreen(Screen.CategoryList) },
                                            onNavigateToAccounts = { openSubScreen(Screen.AccountList) },
                                            onNavigateToRates = { openSubScreen(Screen.Rates) },
                                            onNavigateToBudgets = { openSubScreen(Screen.Budgets) },
                                            onNavigateToInsights = { openSubScreen(Screen.Insights) },
                                            onNavigateToCapture = { openSubScreen(Screen.CaptureSettings) },
                                        )
                                    }
                                }

                                Screen.CategoryList -> CategoryListScreen(
                                    viewModel = dependencies.categoryListViewModel,
                                    onNavigateBack = navigateBack,
                                )

                                Screen.AccountList -> AccountListScreen(
                                    viewModel = dependencies.accountListViewModel,
                                    onNavigateBack = navigateBack,
                                )

                                Screen.Rates -> RatesScreen(
                                    viewModel = ratesViewModel,
                                    onNavigateBack = navigateBack,
                                )

                                Screen.Budgets -> BudgetScreen(
                                    viewModel = dependencies.budgetViewModel,
                                    onNavigateBack = navigateBack,
                                )

                                Screen.Insights -> InsightsScreen(
                                    viewModel = dependencies.insightsViewModel,
                                    onNavigateBack = navigateBack,
                                )

                                Screen.CaptureSettings -> CaptureSettingsScreen(
                                    viewModel = dependencies.captureSettingsViewModel,
                                    access = rememberNotificationAccess(),
                                    capturableApps = rememberCapturableApps(),
                                    onNavigateBack = navigateBack,
                                )

                                else -> Unit
                            }
                        }
                    }
                    FloatingNavigationBar(
                        selected = currentScreen,
                        onSelect = { currentScreen = it },
                        hazeState = hazeState,
                        modifier = Modifier.align(Alignment.BottomCenter),
                    )
                }
            }
        }
    }
}
