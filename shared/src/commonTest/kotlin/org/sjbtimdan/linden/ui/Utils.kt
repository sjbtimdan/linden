package org.sjbtimdan.linden.ui

import androidx.compose.ui.test.ComposeUiTest
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.v2.runComposeUiTest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.sjbtimdan.linden.data.AccountDao
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.EntryDao
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.imports.IvyImporter
import org.sjbtimdan.linden.model.Currency
import org.sjbtimdan.linden.model.ThemeMode
import org.sjbtimdan.linden.ui.accounts.AccountListViewModel
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel
import org.sjbtimdan.linden.ui.ledger.LedgerViewModel
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

@OptIn(ExperimentalTestApi::class)
fun withViewModel(
    block: suspend ComposeUiTest.(CategoryDao, CategoryListViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val dao = CategoryDao(database.categoryQueries)
            val viewModel = CategoryListViewModel(dao)
            block(dao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withViewModel(
    block: suspend ComposeUiTest.(CategoryListViewModel) -> Unit,
) = withViewModel { _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withAccountViewModel(
    block: suspend ComposeUiTest.(AccountDao, AccountListViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val dao = AccountDao(database.accountQueries)
            val viewModel = AccountListViewModel(dao)
            block(dao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withAccountViewModel(
    block: suspend ComposeUiTest.(AccountListViewModel) -> Unit,
) = withAccountViewModel { _, viewModel -> block(viewModel) }

@OptIn(ExperimentalTestApi::class)
fun withSettingsViewModel(
    initialTheme: ThemeMode = ThemeMode.SYSTEM,
    initialCurrency: Currency = Currency.CHF,
    block: suspend ComposeUiTest.(SettingsViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val viewModel = SettingsViewModel(
                settingsDao = dao,
                importer = IvyImporter(database),
                initialTheme = initialTheme,
                initialCurrency = initialCurrency,
            )
            block(viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    block: suspend ComposeUiTest.(EntryDao, AccountDao, CategoryDao, LedgerViewModel) -> Unit,
) {
    onTestMain {
        runComposeUiTest {
            val database = lindenDatabase()
            val entryDao = EntryDao(database.entryQueries)
            val accountDao = AccountDao(database.accountQueries)
            val categoryDao = CategoryDao(database.categoryQueries)
            val viewModel = LedgerViewModel(entryDao, accountDao, categoryDao)
            block(entryDao, accountDao, categoryDao, viewModel)
        }
    }
}

@OptIn(ExperimentalTestApi::class)
fun withLedgerViewModel(
    block: suspend ComposeUiTest.(LedgerViewModel) -> Unit,
) = withLedgerViewModel { _, _, _, viewModel -> block(viewModel) }
