package org.sjbtimdan.linden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.SettingsDao
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.model.ThemeMode

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val (db, initialTheme) = runBlocking {
            val database = createLindenDatabase(DatabaseDriverFactory(this@MainActivity).createDriver())
            val theme = SettingsDao(database.settingsQueries).getTheme()
            database to theme
        }

        setContent {
            App(database = db, initialTheme = initialTheme)
        }
    }
}
