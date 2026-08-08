package org.sjbtimdan.linden

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase

fun main() {
    val driver = DatabaseDriverFactory().createDriver()
    val (database, initialTheme) = runBlocking {
        val db = createLindenDatabase(driver)
        val theme = SettingsDao(db.settingsQueries).getTheme()
        db to theme
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Linden",
        ) {
            App(database, initialTheme)
        }
    }
}
