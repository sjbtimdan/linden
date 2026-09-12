package org.sjbtimdan.linden

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.DefaultDataSeeder
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesFetcher
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.FxRatesSource
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.export.CsvExportManager
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.time.SystemClock
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.budget.BudgetViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.entry.EntryPointViewModel
import org.sjbtimdan.linden.ui.insights.InsightsViewModel
import org.sjbtimdan.linden.ui.ledger.LedgerViewModel
import org.sjbtimdan.linden.ui.rates.RatesViewModel
import org.sjbtimdan.linden.ui.settings.SettingsViewModel

/**
 * Composition root owning every long-lived dependency of the app.
 * DAOs, the repository and the ViewModels are created eagerly: construction
 * is cheap, and [App] touches all of them at first composition anyway. Only
 * [httpClient] stays lazy — building its engine is the one genuinely expensive
 * step, and tests injecting a fake FX source never trigger it.
 */
class AppDependencies(
    val database: LindenDatabase,
    val initialTheme: ThemeMode,
    val initialCurrency: Currency,
    initialHideEntryTotal: Boolean = false,
    initialLanguage: AppLanguage = AppLanguage.SYSTEM,
    fxRatesSource: FxRatesSource? = null,
) {
    val settingsDao = SettingsDao(database.settingsQueries)
    val accountDao = AccountDao(database.accountQueries)
    val categoryDao = CategoryDao(database.categoryQueries)
    val entryDao = EntryDao(database.entryQueries)
    val fxRateDao = FxRateDao(database.fxRateQueries)
    val budgetDao = BudgetDao(database.budgetQueries)
    private val httpClientLazy = lazy {
        HttpClient {
            install(HttpTimeout) {
                // Generous timeouts: rates are fetched over a network that may be slow.
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
        }
    }
    val httpClient: HttpClient by httpClientLazy
    val fxRatesRepository = FxRatesRepository(
        fxRateDao,
        fxRatesSource ?: FxRatesFetcher(httpClient),
        clock = SystemClock,
    )

    /** App-wide scope for the shared [ratesProvider]; lives as long as the dependencies. */
    private val appScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /**
     * The default currency and its FX rates as state flows, shared by every screen
     * that converts amounts, so they observe one subscription instead of each
     * screen querying the settings and rates tables independently.
     */
    val ratesProvider = RatesFlowProvider(settingsDao, fxRatesRepository, appScope)

    /**
     * Every entry as one shared state flow. The screens that need rows across all
     * time (insights, all-time balances, the entry screen total, the ledger's
     * "All" period) observe this instead of each querying and re-mapping the
     * whole table independently on every change.
     */
    val allEntries: StateFlow<List<Entry>> = entryDao.getAll()
        .stateIn(appScope, SharingStarted.Eagerly, emptyList())

    val backupManager = LindenBackupManager(database)
    val csvExportManager = CsvExportManager(entryDao)
    val settingsViewModel = SettingsViewModel(
        settingsDao,
        IvyImporter(database, clock = SystemClock),
        backupManager,
        csvExportManager,
        initialTheme,
        initialCurrency,
        initialHideEntryTotal,
        initialLanguage,
    )
    val ratesViewModel = RatesViewModel(settingsDao, fxRatesRepository, clock = SystemClock)
    val categoryListViewModel = CategoryListViewModel(categoryDao, entryDao)
    val accountListViewModel = AccountListViewModel(accountDao, entryDao, settingsDao, allEntries)
    val budgetViewModel = BudgetViewModel(budgetDao, categoryDao)
    val entryViewModel = EntryPointViewModel(
        entryDao,
        accountDao,
        categoryDao,
        settingsDao,
        ratesProvider,
        allEntries,
        initialHideEntryTotal = initialHideEntryTotal,
        clock = SystemClock,
    )
    val ledgerViewModel = LedgerViewModel(
        entryDao,
        accountDao,
        categoryDao,
        settingsDao,
        budgetDao,
        ratesProvider,
        allEntries,
        clock = SystemClock,
    )
    val insightsViewModel = InsightsViewModel(
        entryDao,
        settingsDao,
        ratesProvider,
        budgetDao,
        allEntries,
        initialHideEntryTotal = initialHideEntryTotal,
        clock = SystemClock,
    )

    /**
     * Releases the app-wide scope and, if it was ever built, the HTTP client.
     * Called when [AppRootViewModel] is cleared (activity finishing, not a
     * configuration change).
     */
    fun close() {
        appScope.cancel()
        if (httpClientLazy.isInitialized()) httpClientLazy.value.close()
    }
}

suspend fun createAppDependencies(driver: SqlDriver): AppDependencies {
    val database = createLindenDatabase(driver)
    val settingsDao = SettingsDao(database.settingsQueries)
    val initialTheme = settingsDao.getTheme()
    val initialCurrency = settingsDao.getDefaultCurrency()
    val initialHideEntryTotal = settingsDao.getHideEntryTotal()
    val initialLanguage = settingsDao.getLanguage()
    DefaultDataSeeder(database).seedIfEmpty(initialCurrency)
    return AppDependencies(database, initialTheme, initialCurrency, initialHideEntryTotal, initialLanguage)
}
