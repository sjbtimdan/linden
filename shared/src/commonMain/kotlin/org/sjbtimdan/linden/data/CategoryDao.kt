package org.sjbtimdan.linden.data

import kotlinx.coroutines.flow.Flow
import org.sjbtimdan.linden.CategoryEntity
import org.sjbtimdan.linden.CategoryQueries
import org.sjbtimdan.linden.model.Category
import org.sjbtimdan.linden.model.CategoryIcon
import org.sjbtimdan.linden.model.CategoryType
import org.sjbtimdan.linden.util.asListFlow

open class CategoryDao(private val queries: CategoryQueries) {
    open suspend fun create(name: String, type: CategoryType, icon: CategoryIcon? = null) {
        queries.insert(name, type.name, icon?.name)
    }

    suspend fun update(category: Category) {
        queries.update(category.name, category.type.name, category.icon?.name, category.id)
    }

    suspend fun delete(id: Long) {
        queries.deleteById(id)
    }

    fun getAll(): Flow<List<Category>> = queries.selectAll().asListFlow { it.toCategory() }

    private fun CategoryEntity.toCategory() = Category(
        id = id,
        name = name,
        type = CategoryType.valueOf(type),
        icon = icon?.let { CategoryIcon.valueOf(it) },
    )
}
