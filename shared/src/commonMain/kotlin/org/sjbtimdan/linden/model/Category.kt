package org.sjbtimdan.linden.model

enum class CategoryType {
    Expense,
    Income,
    Both
}

data class Category(
    val name: String,
    val type: CategoryType
)
