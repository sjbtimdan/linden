package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

class CategoryDaoTest : StringSpec({
    "CRUD operations should work" {
        val database = lindenDatabase()
        val dao = CategoryDao(database.categoryQueries)

        dao.getAll() shouldBe emptyList()
        dao.create("Groceries", CategoryType.Expense)
        val allCreated = dao.getAll()
        allCreated shouldBe listOf(Category(
            id = allCreated.first().id,
            name = "Groceries",
            type = CategoryType.Expense
        ))
        val updated = allCreated.first().copy(name = "Bonus", type = CategoryType.Both)
        dao.update(updated)
        dao.getAll() shouldBe listOf(updated)
    }
})
