package org.sjbtimdan.linden

import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.runBlocking
import org.jetbrains.compose.resources.painterResource
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.generated.resources.Res
import org.sjbtimdan.linden.generated.resources.linden_icon

fun main() {
    val dependencies = runBlocking {
        createAppDependencies(DatabaseDriverFactory().createDriver())
    }

    application {
        Window(
            onCloseRequest = ::exitApplication,
            title = "Linden",
            icon = painterResource(Res.drawable.linden_icon),
        ) {
            App(dependencies)
        }
    }
}
