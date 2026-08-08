package org.sjbtimdan.linden

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.createAppDependencies

fun main() {
    val dependencies = runBlocking {
        createAppDependencies(DatabaseDriverFactory().createDriver())
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Linden",
        ) {
            App(dependencies.database, dependencies.initialTheme)
        }
    }
}
