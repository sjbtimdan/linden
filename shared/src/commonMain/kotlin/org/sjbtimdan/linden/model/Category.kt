package org.sjbtimdan.linden.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Flight
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Savings
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Spa
import androidx.compose.ui.graphics.vector.ImageVector

enum class CategoryType {
    Expense,
    Income,
    Both,
}

enum class CategoryIcon(val label: String) {
    Restaurant("Food & Dining"),
    Movie("Entertainment"),
    ShoppingCart("Groceries"),
    AccountBalance("Bank & Finance"),
    Savings("Savings & Salary"),
    ShoppingBag("Shopping"),
    Home("Home"),
    LocalHospital("Health"),
    FavoriteBorder("Gifts & Donations"),
    Pets("Pets"),
    School("Education"),
    Flight("Travel"),
    Spa("Personal Care"),
    ;

    fun imageVector(): ImageVector = when (this) {
        Restaurant -> Icons.Filled.Restaurant
        Movie -> Icons.Filled.Movie
        ShoppingCart -> Icons.Filled.ShoppingCart
        AccountBalance -> Icons.Filled.AccountBalance
        Savings -> Icons.Filled.Savings
        ShoppingBag -> Icons.Filled.ShoppingBag
        Home -> Icons.Filled.Home
        LocalHospital -> Icons.Filled.LocalHospital
        FavoriteBorder -> Icons.Filled.FavoriteBorder
        Pets -> Icons.Filled.Pets
        School -> Icons.Filled.School
        Flight -> Icons.Filled.Flight
        Spa -> Icons.Filled.Spa
    }
}

data class Category(
    val id: Long,
    val name: String,
    val type: CategoryType,
    val icon: CategoryIcon? = null,
)
