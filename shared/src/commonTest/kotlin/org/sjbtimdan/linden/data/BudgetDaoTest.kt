package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Budget

class BudgetDaoTest : StringSpec({
    "upsert round-trips via budgetsFlow" {
        val database = lindenDatabase()
        val dao = BudgetDao(database.budgetQueries)
        dao.upsert("Groceries", 80_000)
        dao.upsert("Food", 50_000)
        dao.budgetsFlow().first() shouldBe listOf(
            Budget("Food", 50_000),
            Budget("Groceries", 80_000),
        )
    }

    "upsert replaces an existing budget for the same category" {
        val database = lindenDatabase()
        val dao = BudgetDao(database.budgetQueries)
        dao.upsert("Groceries", 80_000)
        dao.upsert("Groceries", 100_000)
        dao.budgetsFlow().first() shouldBe listOf(Budget("Groceries", 100_000))
    }

    "budgetsFlow emits empty by default and follows updates" {
        val database = lindenDatabase()
        val dao = BudgetDao(database.budgetQueries)
        dao.budgetsFlow().first() shouldBe emptyList()
        dao.upsert("Groceries", 80_000)
        dao.budgetsFlow().first() shouldBe listOf(Budget("Groceries", 80_000))
    }

    "delete removes a budget by category name" {
        val database = lindenDatabase()
        val dao = BudgetDao(database.budgetQueries)
        dao.upsert("Groceries", 80_000)
        dao.upsert("Food", 50_000)
        dao.delete("Groceries")
        dao.budgetsFlow().first() shouldBe listOf(Budget("Food", 50_000))
    }
})
