package org.sjbtimdan.linden.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.sjbtimdan.linden.data.lindenDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest : StringSpec({
    "stateFlow tracks the source value" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            val state = MutableStateFlow(1).stateFlow(scope, initial = 0)

            state.value shouldBe 1
        }
    }

    "stateFlow collects the upstream eagerly, without a downstream collector" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            var subscriptions = 0
            val source = flow {
                subscriptions++
                emit(42)
            }

            val state = source.stateFlow(scope, initial = 0)

            subscriptions shouldBe 1
            state.value shouldBe 42
        }
    }

    "asListFlow emits the query rows" {
        runTest {
            val database = lindenDatabase()
            database.accountQueries.insert("Wallet", "CHF", 0)

            val names = database.accountQueries.selectAll().asListFlow { it.name }.first()

            names shouldBe listOf("Wallet")
        }
    }

    "asListFlow without a mapper emits the row type" {
        runTest {
            val database = lindenDatabase()
            database.accountQueries.insert("Wallet", "CHF", 0)

            val rows = database.accountQueries.selectAll().asListFlow().first()

            rows.map { it.name } shouldBe listOf("Wallet")
        }
    }

    "asOneOrNullFlow emits the single row, or null when absent" {
        runTest {
            val database = lindenDatabase()
            database.settingsQueries.insertOrReplace("theme", "DARK")

            database.settingsQueries.selectByKey("theme").asOneOrNullFlow().first()?.value_ shouldBe "DARK"
            database.settingsQueries.selectByKey("absent").asOneOrNullFlow().first() shouldBe null
        }
    }
})
