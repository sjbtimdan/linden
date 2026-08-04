package org.sjbtimdan.linden.data

import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import org.sjbtimdan.linden.model.CategoryType

class CategoryDaoTest : StringSpec({
    "CRUD operations should work" {
        val database = lindenDatabase()
        val dao = CategoryDao(database.categoryQueries)
        dao.getAll() shouldBe emptyList()

        val created = dao.create("Groceries", CategoryType.Expense)
        dao.getAll() shouldBe listOf(created)

        val updated = created.copy(name = "Bonus", type = CategoryType.Both)
        dao.update(updated)
        dao.getAll() shouldBe listOf(updated)
    }
})
