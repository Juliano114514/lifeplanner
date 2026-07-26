package com.example.libui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.Schedule
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TimePicker
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.Instant
import java.time.LocalDate
import java.time.LocalTime
import java.time.ZoneOffset

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDatePickerField(
  value: LocalDate?,
  onValueChange: (LocalDate?) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  optional: Boolean = false,
) {
  var showPicker by rememberSaveable { mutableStateOf(false) }
  PickerField(
    value = value?.toString().orEmpty(),
    label = label,
    icon = { Icon(Icons.Rounded.CalendarMonth, contentDescription = null) },
    onClick = { showPicker = true },
    modifier = modifier,
  )

  if (showPicker) {
    AppDatePickerDialog(
      value = value,
      canClear = optional && value != null,
      onDismiss = { showPicker = false },
      onValueChange = {
        onValueChange(it)
        showPicker = false
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTimePickerField(
  valueMinutes: Int?,
  onValueChange: (Int?) -> Unit,
  label: String,
  modifier: Modifier = Modifier,
  minuteStep: Int = 1,
  optional: Boolean = false,
  allowEndOfDay: Boolean = false,
) {
  require(minuteStep in 1..60 && 60 % minuteStep == 0) {
    "minuteStep must divide one hour"
  }
  require(valueMinutes == null || valueMinutes in 0..MINUTES_PER_DAY) {
    "valueMinutes must be between 0 and 1440"
  }

  var showPicker by rememberSaveable { mutableStateOf(false) }
  PickerField(
    value = valueMinutes?.let(::formatMinuteOfDay).orEmpty(),
    label = label,
    icon = { Icon(Icons.Rounded.Schedule, contentDescription = null) },
    onClick = { showPicker = true },
    modifier = modifier,
  )

  if (showPicker) {
    val initialValue = (valueMinutes ?: currentMinute()).let {
      if (it == MINUTES_PER_DAY) 0 else it
    }
    val pickerState = rememberTimePickerState(
      initialHour = initialValue / MINUTES_PER_HOUR,
      initialMinute = initialValue % MINUTES_PER_HOUR,
      is24Hour = true,
    )
    AlertDialog(
      onDismissRequest = { showPicker = false },
      title = { Text("选择$label") },
      text = {
        Box(Modifier.fillMaxWidth()) {
          TimePicker(state = pickerState)
        }
      },
      confirmButton = {
        PickerActions(
          canClear = optional && valueMinutes != null,
          onClear = {
            onValueChange(null)
            showPicker = false
          },
          onCancel = { showPicker = false },
          onConfirm = {
            val rawValue = pickerState.hour * MINUTES_PER_HOUR + pickerState.minute
            onValueChange(rawValue.snapToStep(minuteStep, allowEndOfDay))
            showPicker = false
          },
        )
      },
    )
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun AppDatePickerDialog(
  value: LocalDate?,
  canClear: Boolean = false,
  onDismiss: () -> Unit,
  onValueChange: (LocalDate?) -> Unit,
) {
  val pickerState = rememberDatePickerState(
    initialSelectedDateMillis = value?.toPickerMillis(),
  )
  DatePickerDialog(
    onDismissRequest = onDismiss,
    confirmButton = {
      PickerActions(
        canClear = canClear,
        confirmEnabled = pickerState.selectedDateMillis != null,
        onClear = { onValueChange(null) },
        onCancel = onDismiss,
        onConfirm = {
          pickerState.selectedDateMillis?.let {
            onValueChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
          }
        },
      )
    },
  ) {
    DatePicker(state = pickerState)
  }
}

@Composable
private fun PickerField(
  value: String,
  label: String,
  icon: @Composable () -> Unit,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val displayedValue = value.ifBlank { "未选择" }
  Box(modifier) {
    OutlinedTextField(
      value = value,
      onValueChange = {},
      label = { Text(label) },
      placeholder = { Text("未选择") },
      trailingIcon = icon,
      readOnly = true,
      singleLine = true,
      modifier = Modifier
        .fillMaxWidth()
        .clearAndSetSemantics {},
    )
    Box(
      Modifier
        .matchParentSize()
        .clickable(role = Role.Button, onClick = onClick)
        .semantics {
          contentDescription = "$label，$displayedValue"
        },
    )
  }
}

@Composable
private fun PickerActions(
  canClear: Boolean,
  onClear: () -> Unit,
  onCancel: () -> Unit,
  onConfirm: () -> Unit,
  confirmEnabled: Boolean = true,
) {
  Row {
    if (canClear) {
      TextButton(onClick = onClear) { Text("清除") }
    }
    TextButton(onClick = onCancel) { Text("取消") }
    TextButton(onClick = onConfirm, enabled = confirmEnabled) { Text("确定") }
  }
}

private fun LocalDate.toPickerMillis(): Long =
  atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()

private fun currentMinute(): Int {
  val now = LocalTime.now()
  return now.hour * MINUTES_PER_HOUR + now.minute
}

private fun Int.snapToStep(step: Int, allowEndOfDay: Boolean): Int {
  val rounded = ((this + step / 2) / step) * step
  return when {
    rounded < MINUTES_PER_DAY -> rounded
    allowEndOfDay -> MINUTES_PER_DAY
    else -> MINUTES_PER_DAY - step
  }
}

fun formatMinuteOfDay(value: Int): String =
  "%02d:%02d".format(value / MINUTES_PER_HOUR, value % MINUTES_PER_HOUR)

private const val MINUTES_PER_HOUR = 60
private const val MINUTES_PER_DAY = 24 * MINUTES_PER_HOUR
