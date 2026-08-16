package org.sjbtimdan.linden

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.sjbtimdan.linden.data.DatabaseDriverFactory

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        setContent {
            AppRoot {
                withContext(Dispatchers.IO) {
                    createAppDependencies(DatabaseDriverFactory(this@MainActivity).createDriver())
                }
            }
        }
    }
}
