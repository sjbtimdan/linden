package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout

@OptIn(ExperimentalCoroutinesApi::class)
class HideEntryTotalSettingTest : StringSpec({
    "seeds the state with the initial value before the database emits" {
        runTest {
            val database = lindenDatabase()
            val setting = HideEntryTotalSetting(SettingsDao(database.settingsQueries), initial = true, backgroundScope)

            setting.state.value shouldBe true
        }
    }

    "set persists the value and the database flow propagates it back" {
        runTest {
            val database = lindenDatabase()
            val dao = SettingsDao(database.settingsQueries)
            val setting = HideEntryTotalSetting(dao, initial = false, backgroundScope)
            setting.state.value shouldBe false

            setting.set(true)

            withTimeout(5_000) { setting.state.first { it } }
            dao.getHideEntryTotal() shouldBe true
        }
    }
})
