package org.sjbtimdan.linden.data

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.flow.first
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

class CategoryDaoTest : StringSpec({
    "CRUD operations should work" {
        val database = lindenDatabase()
        val dao = CategoryDao(database.categoryQueries)

        dao.getAll().first() shouldBe emptyList()
        dao.create("Groceries", CategoryType.Expense)
        val allCreated = dao.getAll().first()
        allCreated shouldBe listOf(
            Category(
                id = allCreated.first().id,
                name = "Groceries",
                type = CategoryType.Expense,
            ),
        )
        val updated = allCreated.first().copy(name = "Bonus", type = CategoryType.Both)
        dao.update(updated)
        dao.getAll().first() shouldBe listOf(updated)
    }

    "creating a category with a duplicate name throws" {
        val database = lindenDatabase()
        val dao = CategoryDao(database.categoryQueries)

        dao.create("Groceries", CategoryType.Expense)

        shouldThrow<Exception> {
            dao.create("Groceries", CategoryType.Income)
        }
    }

    "updating a category to a duplicate name throws" {
        val database = lindenDatabase()
        val dao = CategoryDao(database.categoryQueries)

        dao.create("Groceries", CategoryType.Expense)
        dao.create("Salary", CategoryType.Income)
        val groceries = dao.getAll().first().first { it.name == "Groceries" }

        shouldThrow<Exception> {
            dao.update(groceries.copy(name = "Salary"))
        }
    }
})
