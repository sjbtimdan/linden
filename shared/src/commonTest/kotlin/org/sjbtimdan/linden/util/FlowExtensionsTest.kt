package org.sjbtimdan.linden.util

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runTest
import org.sjbtimdan.linden.data.lindenDatabase

@OptIn(ExperimentalCoroutinesApi::class)
class FlowExtensionsTest : StringSpec({
    "stateFlow starts its upstream only once a collector arrives" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            var subscriptions = 0
            val source = flow {
                subscriptions++
                emit(42)
            }

            val state = source.stateFlow(scope, initial = 0)

            subscriptions shouldBe 0
            state.value shouldBe 0

            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
            subscriptions shouldBe 1
            state.value shouldBe 42
            collector.cancel()
        }
    }

    "stateFlow stops its upstream after the stop timeout once the last collector leaves" {
        runTest {
            val scope = CoroutineScope(UnconfinedTestDispatcher(testScheduler))
            var cancelled = false
            val source = flow {
                try {
                    emit(1)
                    awaitCancellation()
                } finally {
                    cancelled = true
                }
            }

            val state = source.stateFlow(scope, initial = 0)
            val collector = launch(UnconfinedTestDispatcher(testScheduler)) { state.collect {} }
            state.value shouldBe 1

            collector.cancel()
            advanceTimeBy(4_999)
            cancelled shouldBe false
            advanceTimeBy(2)
            cancelled shouldBe true
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
