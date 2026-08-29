package org.sjbtimdan.linden.ui.ledger

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import kotlinx.datetime.LocalDate

@Composable
fun PeriodNavigator(
    period: LedgerPeriod,
    anchor: LocalDate,
    onPeriodChange: (LedgerPeriod) -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
) {
    var expanded by remember { mutableStateOf(false) }
    val canNavigate = period != LedgerPeriod.All

    Row(verticalAlignment = Alignment.CenterVertically) {
        IconButton(
            onClick = onPrevious,
            enabled = canNavigate,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Previous period",
            )
        }
        Box {
            TextButton(
                onClick = { expanded = true },
                modifier = Modifier.testTag("periodLabel"),
            ) {
                Text(
                    text = period.windowLabel(anchor) ?: "All",
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Ellipsis,
                )
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = null,
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
            ) {
                LedgerPeriod.entries.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.name) },
                        onClick = {
                            onPeriodChange(option)
                            expanded = false
                        },
                    )
                }
            }
        }
        IconButton(
            onClick = onNext,
            enabled = canNavigate,
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                contentDescription = "Next period",
            )
        }
    }
}
