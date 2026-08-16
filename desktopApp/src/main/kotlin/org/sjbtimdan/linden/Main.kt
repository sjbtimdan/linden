package org.sjbtimdan.linden

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.painterResource
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.generated.resources.Res
import org.sjbtimdan.linden.generated.resources.linden_icon

fun main() {
    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Linden",
            icon = painterResource(Res.drawable.linden_icon),
        ) {
            AppRoot {
                withContext(Dispatchers.IO) {
                    createAppDependencies(DatabaseDriverFactory().createDriver())
                }
            }
        }
    }
}
