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
import org.sjbtimdan.linden.data.CategoryDao
import org.sjbtimdan.linden.data.lindenDatabase
import org.sjbtimdan.linden.ui.categories.CategoryListViewModel

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
