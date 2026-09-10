package org.sjbtimdan.linden

import androidx.compose.runtime.saveable.SaverScope
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe

private val allScreens = listOf(
    Screen.Entry,
    Screen.Ledger,
    Screen.Settings,
    Screen.CategoryList,
    Screen.AccountList,
    Screen.Rates,
    Screen.Budgets,
    Screen.Insights,
)

class ScreenSaverTest : StringSpec({
    "every screen round-trips through the saver" {
        val scope = SaverScope { true }
        allScreens.forEach { screen ->
            val saved = with(ScreenSaver) { scope.save(screen) }
            saved shouldBe screen.key
            ScreenSaver.restore(saved!!) shouldBe screen
        }
    }

    "screens have unique keys" {
        allScreens.map { it.key }.toSet().size shouldBe allScreens.size
    }

    "restoring an unknown key falls back to the entry screen" {
        ScreenSaver.restore("Unknown") shouldBe Screen.Entry
    }
})
