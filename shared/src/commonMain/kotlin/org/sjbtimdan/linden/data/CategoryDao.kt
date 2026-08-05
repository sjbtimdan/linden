package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.async.coroutines.awaitAsOne
import org.sjbtimdan.linden.CategoryEntity
import org.sjbtimdan.linden.CategoryQueries
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryType

class CategoryDao(private val queries: CategoryQueries) {
    suspend fun create(name: String, type: CategoryType): Category {
        return queries.transactionWithResult {
            queries.insert(name, type.name)
            val id = queries.lastInsertId().awaitAsOne()
            queries.selectById(id).awaitAsOne().toCategory()
        }
    }

    suspend fun update(category: Category): Unit {
        queries.update(category.name, category.type.name, category.id)
    }

    suspend fun getAll(): List<Category> {
        return queries.selectAll().awaitAsList().map { it.toCategory() }
    }

    private fun CategoryEntity.toCategory() = Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type)
    )
}
