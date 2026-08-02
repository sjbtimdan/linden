package org.sjbtimdan.linden.data

import org.sjbtimdan.linden.CategoryEntity
import org.sjbtimdan.linden.CategoryQueries
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

class CategoryDao(private val queries: CategoryQueries) {
    suspend fun create(name: String, type: CategoryType): Category {
        queries.insert(name, type.name)
        val id = queries.lastInsertId().executeAsOne()
        return queries.selectById(id).executeAsOne().toCategory()
    }

    suspend fun update(category: Category): Unit {
        queries.update(category.name, category.type.name, category.id!!)
    }

    fun getAll(): List<Category> {
        return queries.selectAll().executeAsList().map { it.toCategory() }
    }

    private fun CategoryEntity.toCategory() = Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type)
    )
}
