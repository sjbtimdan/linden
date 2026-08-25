package org.sjbtimdan.linden.ui.entry

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.TimePickerDialog
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.number
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Date and time buttons with their picker dialogs. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateAndTimeButtons(createdAt: Instant, createdZone: TimeZone, onChange: (Instant) -> Unit) {
    var showDatePicker by remember { mutableStateOf(false) }
    var showTimePicker by remember { mutableStateOf(false) }

    Text(
        text = "Date & time",
        style = MaterialTheme.typography.titleSmall,
        modifier = Modifier.padding(bottom = 8.dp),
    )
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        OutlinedButton(onClick = { showDatePicker = true }) {
            Text(formatDate(createdAt, createdZone))
        }
        OutlinedButton(onClick = { showTimePicker = true }) {
            Text(formatTime(createdAt, createdZone))
        }
    }

    if (showDatePicker) {
        val local = createdAt.toLocalDateTime(createdZone)
        val initialUtcMillis = LocalDateTime(local.year, local.month.number, local.day, 0, 0)
            .toInstant(TimeZone.UTC)
            .toEpochMilliseconds()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialUtcMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val utcMillis = datePickerState.selectedDateMillis
                    if (utcMillis != null) {
                        onChange(combineDateAndTime(utcMillis, local.hour, local.minute, createdZone))
                    }
                    showDatePicker = false
                }) {
                    Text("OK")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            DatePicker(state = datePickerState)
        }
    }

    if (showTimePicker) {
        val local = createdAt.toLocalDateTime(createdZone)
        val timePickerState = rememberTimePickerState(
            initialHour = local.hour,
            initialMinute = local.minute,
            is24Hour = true,
        )
        TimePickerDialog(
            onDismissRequest = { showTimePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    val newInstant = LocalDateTime(
                        local.year,
                        local.month.number,
                        local.day,
                        timePickerState.hour,
                        timePickerState.minute,
                    ).toInstant(createdZone)
                    onChange(newInstant)
                    showTimePicker = false
                }) {
                    Text("OK")
                }
            },
            title = {
                Text("Select time")
            },
            dismissButton = {
                TextButton(onClick = { showTimePicker = false }) {
                    Text("Cancel")
                }
            },
        ) {
            TimePicker(state = timePickerState)
        }
    }
}
