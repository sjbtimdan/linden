package org.sjbtimdan.linden

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.createLindenDatabase

fun main() {
    val driver = DatabaseDriverFactory().createDriver()
    val database = runBlocking { createLindenDatabase(driver) }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Linden",
        ) {
            App(database)
        }
    }
}
