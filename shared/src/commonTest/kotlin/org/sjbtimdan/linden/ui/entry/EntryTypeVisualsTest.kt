package org.sjbtimdan.linden.ui.entry

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import org.sjbtimdan.linden.model.EntryType

class EntryTypeVisualsTest : StringSpec({
    "every entry type has a distinct icon" {
        val icons = EntryType.entries.map { it.icon() }
        icons.distinct().size shouldBe icons.size
    }

    "expense, income and transfer icons all differ" {
        EntryType.Expense.icon() shouldNotBe EntryType.Income.icon()
        EntryType.Expense.icon() shouldNotBe EntryType.Transfer.icon()
        EntryType.Income.icon() shouldNotBe EntryType.Transfer.icon()
    }
})
