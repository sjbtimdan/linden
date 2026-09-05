package org.sjbtimdan.linden

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpTimeout
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesFetcher
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.FxRatesSource
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.export.CsvExportManager
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.budget.BudgetViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.entry.EntryPointViewModel
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
    fxRatesSource: FxRatesSource? = null,
) {
    val settingsDao = SettingsDao(database.settingsQueries)
    val accountDao = AccountDao(database.accountQueries)
    val categoryDao = CategoryDao(database.categoryQueries)
    val entryDao = EntryDao(database.entryQueries)
    val fxRateDao = FxRateDao(database.fxRateQueries)
    val budgetDao = BudgetDao(database.budgetQueries)
    val httpClient by lazy {
        HttpClient {
            install(HttpTimeout) {
                // Generous timeouts: rates are fetched over a network that may be slow.
                requestTimeoutMillis = 60_000
                connectTimeoutMillis = 30_000
                socketTimeoutMillis = 60_000
            }
        }
    }
    val fxRatesRepository = FxRatesRepository(fxRateDao, fxRatesSource ?: FxRatesFetcher(httpClient))
    val backupManager = LindenBackupManager(database)
    val csvExportManager = CsvExportManager(entryDao)
    val settingsViewModel = SettingsViewModel(
        settingsDao,
        IvyImporter(database),
        backupManager,
        csvExportManager,
        initialTheme,
        initialCurrency,
        initialHideEntryTotal,
    )
    val ratesViewModel = RatesViewModel(settingsDao, fxRatesRepository)
    val categoryListViewModel = CategoryListViewModel(categoryDao, entryDao)
    val accountListViewModel = AccountListViewModel(accountDao, entryDao, settingsDao)
    val budgetViewModel = BudgetViewModel(budgetDao, categoryDao)
    val entryViewModel = EntryPointViewModel(
        entryDao,
        accountDao,
        categoryDao,
        settingsDao,
        fxRatesRepository,
        initialHideEntryTotal,
    )
    val ledgerViewModel = LedgerViewModel(entryDao, accountDao, categoryDao, settingsDao, fxRatesRepository, budgetDao)
}

suspend fun createAppDependencies(driver: SqlDriver): AppDependencies {
    val database = createLindenDatabase(driver)
    val settingsDao = SettingsDao(database.settingsQueries)
    val initialTheme = settingsDao.getTheme()
    val initialCurrency = settingsDao.getDefaultCurrency()
    val initialHideEntryTotal = settingsDao.getHideEntryTotal()
    return AppDependencies(database, initialTheme, initialCurrency, initialHideEntryTotal)
}
