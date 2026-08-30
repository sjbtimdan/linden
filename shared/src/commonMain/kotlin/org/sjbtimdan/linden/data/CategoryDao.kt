package org.sjbtimdan.linden.data

import app.cash.sqldelight.async.coroutines.awaitAsList
import app.cash.sqldelight.coroutines.asFlow
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import org.sjbtimdan.linden.CategoryEntity
import org.sjbtimdan.linden.CategoryQueries
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType

class CategoryDao(private val queries: CategoryQueries) {
    suspend fun create(name: String, type: CategoryType, icon: CategoryIcon? = null) {
        queries.insert(name, type.name, icon?.name)
    }

    suspend fun update(category: Category) {
        queries.update(category.name, category.type.name, category.icon?.name, category.id)
    }

    suspend fun deleteAll() {
        queries.deleteAll()
    }

    fun getAll(): Flow<List<Category>> = queries.selectAll()
        .asFlow()
        .map { it.awaitAsList().map { row -> row.toCategory() } }

    private fun CategoryEntity.toCategory() = Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type),
        icon = icon?.let { CategoryIcon.valueOf(it) },
    )
}
