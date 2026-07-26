package com.example.libui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppDateNavigator(
  date: LocalDate,
  onDateChange: (LocalDate) -> Unit,
  modifier: Modifier = Modifier,
) {
  var showPicker by rememberSaveable { mutableStateOf(false) }
  val dateLabel = date.format(DateTimeFormatter.ofPattern("M月d日"))

  Row(
    modifier = modifier.fillMaxWidth(),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    IconButton(onClick = { onDateChange(date.minusDays(1)) }) {
      Icon(Icons.Rounded.ChevronLeft, contentDescription = "上一天")
    }
    TextButton(
      onClick = { showPicker = true },
      modifier = Modifier
        .weight(1f)
        .semantics { contentDescription = "选择日期，当前$dateLabel" },
    ) {
      Text(dateLabel)
    }
    IconButton(onClick = { onDateChange(date.plusDays(1)) }) {
      Icon(Icons.Rounded.ChevronRight, contentDescription = "下一天")
    }
  }

  if (showPicker) {
    val pickerState = rememberDatePickerState(
      initialSelectedDateMillis = date.toPickerMillis(),
    )
    DatePickerDialog(
      onDismissRequest = { showPicker = false },
      confirmButton = {
        Row {
          TextButton(onClick = { showPicker = false }) {
            Text("取消")
          }
          TextButton(
            enabled = pickerState.selectedDateMillis != null,
            onClick = {
              pickerState.selectedDateMillis?.let {
                onDateChange(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate())
              }
              showPicker = false
            },
          ) {
            Text("确定")
          }
        }
      },
    ) {
      DatePicker(state = pickerState)
    }
  }
}

private fun LocalDate.toPickerMillis(): Long =
  atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
