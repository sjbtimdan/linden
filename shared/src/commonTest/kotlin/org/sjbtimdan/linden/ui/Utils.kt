package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.sjbtimdan.linden.AppDependencies
import org.sjbtimdan.linden.backup.LindenBackupManager
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.BudgetDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.FakeFxRatesSource
import org.sjbtimdan.linden.data.FxRateDao
import org.sjbtimdan.linden.data.FxRatesRepository
import org.sjbtimdan.linden.data.FxRatesSource
import org.sjbtimdan.linden.data.RatesFlowProvider
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.export.CsvExportManager
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.AppLanguage
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.Entry
import org.sjbtimdan.linden.model.FxRate
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.time.AppClock
import org.sjbtimdan.linden.time.FakeClock
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.budget.BudgetViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.entry.EntryPointViewModel
import org.sjbtimdan.linden.ui.insights.InsightsViewModel
import org.sjbtimdan.linden.ui.ledger.LedgerPeriod
import org.sjbtimdan.linden.ui.ledger.LedgerViewModel
import org.sjbtimdan.linden.ui.rates.RatesViewModel
import org.sjbtimdan.linden.ui.settings.SettingsViewModel

@OptIn(ExperimentalCoroutinesApi::class)
fun onTestMain(block: suspend () -> Unit) {
    Dispatchers.setMain(UnconfinedTestDispatcher())
    try {
        runBlocking { block() }
    } finally {
        Dispatchers.resetMain()
    }
}

/**
 * Builds a [RatesFlowProvider] over an in-memory database for ViewModels that
 * take a shared provider as a required dependency (mirroring the composition
 * root wiring); the provider collects on the test's Main dispatcher.
 */
internal fun testRatesProvider(settingsDao: SettingsDao, fxRateDao: FxRateDao): RatesFlowProvider = RatesFlowProvider(
    settingsDao,
    FxRatesRepository(fxRateDao, FakeFxRatesSource(), clock = FakeClock()),
    CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate),
)

/**
 * Caches [entryDao]'s full entry list into one [StateFlow] for ViewModels that
 * take a shared all-entries flow as a required dependency (mirroring the
 * composition root wiring); the flow collects on the test's Main dispatcher.
 */
internal fun testAllEntries(entryDao: EntryDao): StateFlow<List<Entry>> = entryDao.getAll()
    .stateIn(CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate), SharingStarted.Eagerly, emptyList())

@OptIn(ExperimentalTestApi::class)
fun withApp(
    fxRatesSource: FxRatesSource = FakeFxRatesSource(),
    block: suspend ComposeUiTest.(AppDependencies) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val dependencies = AppDependencies(
                database = lindenDatabase(),
                initialTheme = ThemeMode.SYSTEM,
                initialCurrency = Currency.CHF,
                fxRatesSource = fxRatesSource,
            )
            block(dependencies)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withViewModel(block: suspend ComposeUiTest.(CategoryDao, EntryDao, AccountDao, CategoryListViewModel) -> Unit) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val categoryDao = CategoryDao(database.categoryQueries)
            val entryDao = EntryDao(database.entryQueries)
            val accountDao = AccountDao(database.accountQueries)
            val viewModel = CategoryListViewModel(categoryDao, entryDao)
            block(categoryDao, entryDao, accountDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withViewModel(block: suspend ComposeUiTest.(CategoryDao, EntryDao, CategoryListViewModel) -> Unit) =
    withViewModel { categoryDao, entryDao, _, viewModel -> block(categoryDao, entryDao, viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withViewModel(block: suspend ComposeUiTest.(CategoryDao, CategoryListViewModel) -> Unit) =
    withViewModel { categoryDao, _, _, viewModel -> block(categoryDao, viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withViewModel(block: suspend ComposeUiTest.(CategoryListViewModel) -> Unit) =
    withViewModel { _, _, _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withAccountViewModel(
    block: suspend ComposeUiTest.(AccountDao, EntryDao, CategoryDao, AccountListViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val accountDao = AccountDao(database.accountQueries)
            val entryDao = EntryDao(database.entryQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val settingsDao = SettingsDao(database.settingsQueries)
            val viewModel = AccountListViewModel(accountDao, entryDao, settingsDao, testAllEntries(entryDao))
            block(accountDao, entryDao, categoryDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withAccountViewModel(block: suspend ComposeUiTest.(AccountListViewModel) -> Unit) =
    withAccountViewModel { _, _, _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withBudgetViewModel(block: suspend ComposeUiTest.(BudgetDao, CategoryDao, BudgetViewModel) -> Unit) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val budgetDao = BudgetDao(database.budgetQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val viewModel = BudgetViewModel(budgetDao, categoryDao)
            block(budgetDao, categoryDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withBudgetViewModel(block: suspend ComposeUiTest.(CategoryDao, BudgetViewModel) -> Unit) =
    withBudgetViewModel { _, categoryDao, viewModel -> block(categoryDao, viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withSettingsViewModel(
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
    initialCurrency: Currency = Currency.CHF,
    initialLanguage: AppLanguage = AppLanguage.SYSTEM,
    block: suspend ComposeUiTest.(SettingsViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                settingsDao = dao,
                importer = IvyImporter(database, clock = FakeClock()),
                backupManager = LindenBackupManager(database),
                csvExporter = CsvExportManager(EntryDao(database.entryQueries)),
                initialTheme = initialTheme,
                initialCurrency = initialCurrency,
                initialLanguage = initialLanguage,
            )
            block(viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withRatesViewModel(
    fxRatesSource: FakeFxRatesSource = FakeFxRatesSource(),
    block: suspend ComposeUiTest.(SettingsDao, RatesViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val settingsDao = SettingsDao(database.settingsQueries)
            val viewModel = RatesViewModel(
                settingsDao = settingsDao,
                fxRatesRepository = FxRatesRepository(
                    FxRateDao(database.fxRateQueries),
                    fxRatesSource,
                    clock = FakeClock(),
                ),
                clock = FakeClock(),
            )
            block(settingsDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withEntryPoint(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    hideEntryTotal: Boolean = false,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(EntryDao, AccountDao, CategoryDao, EntryPointViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val entryDao = EntryDao(database.entryQueries)
            val accountDao = AccountDao(database.accountQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val settingsDao = SettingsDao(database.settingsQueries)
            if (defaultCurrency != Currency.CHF) settingsDao.setDefaultCurrency(defaultCurrency)
            if (hideEntryTotal) settingsDao.setHideEntryTotal(true)
            val fxRateDao = FxRateDao(database.fxRateQueries)
            if (rates.isNotEmpty()) fxRateDao.replaceRates(rates, fetchedAt = 0L)
            val viewModel = EntryPointViewModel(
                entryDao,
                accountDao,
                categoryDao,
                settingsDao,
                testRatesProvider(settingsDao, fxRateDao),
                testAllEntries(entryDao),
                initialHideEntryTotal = hideEntryTotal,
                clock = clock,
            )
            block(entryDao, accountDao, categoryDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withEntryPoint(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    hideEntryTotal: Boolean = false,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(AccountDao, CategoryDao, EntryPointViewModel) -> Unit,
) = withEntryPoint(clock, defaultCurrency, hideEntryTotal, rates) { _, accountDao, categoryDao, viewModel ->
    block(accountDao, categoryDao, viewModel)
}

@OptIn(ExperimentalTestApi::class)
fun withEntryPoint(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    hideEntryTotal: Boolean = false,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(EntryPointViewModel) -> Unit,
) = withEntryPoint(clock, defaultCurrency, hideEntryTotal, rates) { _, _, _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(EntryDao, AccountDao, CategoryDao, LedgerViewModel) -> Unit,
) = withLedgerViewModel(clock, defaultCurrency, rates) { entryDao, accountDao, categoryDao, _, viewModel ->
    block(entryDao, accountDao, categoryDao, viewModel)
}

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    clock: AppClock,
    defaultCurrency: Currency,
    rates: List<FxRate>,
    block: suspend ComposeUiTest.(EntryDao, AccountDao, CategoryDao, SettingsDao, LedgerViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val entryDao = EntryDao(database.entryQueries)
            val accountDao = AccountDao(database.accountQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val settingsDao = SettingsDao(database.settingsQueries)
            if (defaultCurrency != Currency.CHF) settingsDao.setDefaultCurrency(defaultCurrency)
            val fxRateDao = FxRateDao(database.fxRateQueries)
            if (rates.isNotEmpty()) fxRateDao.replaceRates(rates, fetchedAt = 0L)
            val viewModel = LedgerViewModel(
                entryDao,
                accountDao,
                categoryDao,
                settingsDao,
                BudgetDao(database.budgetQueries),
                testRatesProvider(settingsDao, fxRateDao),
                testAllEntries(entryDao),
                clock = clock,
            )
            // Tests create entries with the default epoch timestamp, so they assume
            // the "All" period rather than the production default of "Month".
            viewModel.setPeriod(LedgerPeriod.All)
            block(entryDao, accountDao, categoryDao, settingsDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(AccountDao, CategoryDao, LedgerViewModel) -> Unit,
) = withLedgerViewModel(
    clock = clock,
    defaultCurrency = defaultCurrency,
    rates = rates,
) { _, accountDao, categoryDao, _, viewModel ->
    block(accountDao, categoryDao, viewModel)
}

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    rates: List<FxRate> = emptyList(),
    block: suspend ComposeUiTest.(LedgerViewModel) -> Unit,
) = withLedgerViewModel(
    clock = clock,
    defaultCurrency = defaultCurrency,
    rates = rates,
) { _, _, _, _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withInsightsViewModel(
    clock: AppClock,
    defaultCurrency: Currency = Currency.CHF,
    rates: List<FxRate> = emptyList(),
    hideEntryTotal: Boolean = false,
    block: suspend ComposeUiTest.(AccountDao, CategoryDao, EntryDao, InsightsViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val accountDao = AccountDao(database.accountQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val entryDao = EntryDao(database.entryQueries)
            val settingsDao = SettingsDao(database.settingsQueries)
            if (defaultCurrency != Currency.CHF) settingsDao.setDefaultCurrency(defaultCurrency)
            if (hideEntryTotal) settingsDao.setHideEntryTotal(true)
            val fxRateDao = FxRateDao(database.fxRateQueries)
            if (rates.isNotEmpty()) fxRateDao.replaceRates(rates, fetchedAt = 0L)
            val viewModel = InsightsViewModel(
                entryDao,
                settingsDao,
                testRatesProvider(settingsDao, fxRateDao),
                BudgetDao(database.budgetQueries),
                testAllEntries(entryDao),
                initialHideEntryTotal = hideEntryTotal,
                clock = clock,
            )
            block(accountDao, categoryDao, entryDao, viewModel)
        }
    }
}
