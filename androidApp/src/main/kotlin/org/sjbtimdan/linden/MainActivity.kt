package org.sjbtimdan.linden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.runBlocking
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.createAppDependencies

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        val dependencies = runBlocking {
            createAppDependencies(DatabaseDriverFactory(this@MainActivity).createDriver())
        }

        setContent {
            App(
                database = dependencies.database,
                initialTheme = dependencies.initialTheme,
                initialCurrency = dependencies.initialCurrency,
            )
        }
    }
}
