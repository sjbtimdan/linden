package org.sjbtimdan.linden.model

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.DirectionsBus
import androidx.compose.material.icons.filled.DirectionsCar
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

enum class CategoryIcon {
    Restaurant,
    Movie,
    ShoppingCart,
    AccountBalance,
    Savings,
    ShoppingBag,
    Home,
    LocalHospital,
    FavoriteBorder,
    Pets,
    School,
    Flight,
    Spa,
    DirectionsCar,
    DirectionsBus,
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
        DirectionsCar -> Icons.Filled.DirectionsCar
        DirectionsBus -> Icons.Filled.DirectionsBus
    }
}

data class Category(
    override val id: Long,
    override val name: String,
    val type: CategoryType,
    val icon: CategoryIcon? = null,
) : NamedEntity
