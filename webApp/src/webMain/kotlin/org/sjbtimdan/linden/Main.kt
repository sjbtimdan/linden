package org.sjbtimdan.linden

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.ComposeViewport
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.sjbtimdan.linden.data.DatabaseDriverFactory
import org.sjbtimdan.linden.data.createLindenDatabase
import org.sjbtimdan.linden.db.LindenDatabase

private val dbDeferred = CompletableDeferred<LindenDatabase>()

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
    val factory = DatabaseDriverFactory()

    CoroutineScope(SupervisorJob()).launch {
        dbDeferred.complete(createLindenDatabase(factory.createDriver()))
    }

    ComposeViewport {
        var db by remember { mutableStateOf<LindenDatabase?>(null) }

        LaunchedEffect(Unit) {
            db = dbDeferred.await()
        }

        val d = db ?: return@ComposeViewport

        App(database = d)
    }
}
