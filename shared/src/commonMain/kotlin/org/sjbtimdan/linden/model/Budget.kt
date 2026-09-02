package org.sjbtimdan.linden.model

/**
 * A monthly spending limit for a single category, in the default currency's minor
 * units. Budgets are keyed by category name (not id) so they survive category
 * renames. A budget whose category no longer exists is simply unused.
 */
data class Budget(
    val categoryName: String,
    val limitMinor: Long,
)
