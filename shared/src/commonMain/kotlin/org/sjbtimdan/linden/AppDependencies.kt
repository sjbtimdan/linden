package org.sjbtimdan.linden

import app.cash.sqldelight.db.SqlDriver
import io.ktor.client.HttpClient
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesFetcher
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.FxRatesSource
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.history.HistoryViewModel
import org.sjbtimdan.linden.ui.ledger.LedgerViewModel
import org.sjbtimdan.linden.ui.rates.RatesViewModel
import org.sjbtimdan.linden.ui.settings.SettingsViewModel

/**
 * Composition root owning every long-lived dependency of the app.
 * Properties are lazy so the graph is explicit and nothing is constructed
 * until a screen actually uses it.
 */
class AppDependencies(
    val database: LindenDatabase,
    val initialTheme: ThemeMode,
    val initialCurrency: Currency,
    private val fxRatesSource: FxRatesSource? = null,
) {
    val settingsDao by lazy { SettingsDao(database.settingsQueries) }
    val accountDao by lazy { AccountDao(database.accountQueries) }
    val categoryDao by lazy { CategoryDao(database.categoryQueries) }
    val entryDao by lazy { EntryDao(database.entryQueries) }
    val httpClient by lazy { HttpClient() }
    val fxRatesRepository by lazy {
        FxRatesRepository(FxRateDao(database.fxRateQueries), fxRatesSource ?: FxRatesFetcher(httpClient))
    }
    val settingsViewModel by lazy {
        SettingsViewModel(settingsDao, IvyImporter(database), initialTheme, initialCurrency)
    }
    val ratesViewModel by lazy { RatesViewModel(settingsDao, fxRatesRepository) }
    val categoryListViewModel by lazy { CategoryListViewModel(categoryDao, entryDao, settingsDao, fxRatesRepository) }
    val accountListViewModel by lazy { AccountListViewModel(accountDao, entryDao, settingsDao, fxRatesRepository) }
    val ledgerViewModel by lazy { LedgerViewModel(entryDao, accountDao, categoryDao) }
    val historyViewModel by lazy {
        HistoryViewModel(entryDao, accountDao, categoryDao, settingsDao, fxRatesRepository)
    }
}

suspend fun createAppDependencies(driver: SqlDriver): AppDependencies {
    val database = createLindenDatabase(driver)
    val settingsDao = SettingsDao(database.settingsQueries)
    val initialTheme = settingsDao.getTheme()
    val initialCurrency = settingsDao.getDefaultCurrency()
    return AppDependencies(database, initialTheme, initialCurrency)
}
