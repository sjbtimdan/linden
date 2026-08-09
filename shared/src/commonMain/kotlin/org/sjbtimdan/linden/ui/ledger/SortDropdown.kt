package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue

private fun SortOrder.label(): String = when (this) {
    SortOrder.NewestFirst -> "Newest first"
    SortOrder.OldestFirst -> "Oldest first"
    SortOrder.AmountHighToLow -> "Amount high to low"
    SortOrder.AmountLowToHigh -> "Amount low to high"
}

@Composable
fun SortDropdown(
    current: SortOrder,
    onChange: (SortOrder) -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        TextButton(onClick = { expanded = true }) {
            Text("Sort: ${current.label()}")
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = null,
            )
        }
        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
        ) {
            SortOrder.entries.forEach { order ->
                DropdownMenuItem(
                    text = { Text(order.label()) },
                    onClick = {
                        onChange(order)
                        expanded = false
                    },
                )
            }
        }
    }
}