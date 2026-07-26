package com.example.libui.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import java.time.LocalDate
import java.time.format.DateTimeFormatter

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
    AppDatePickerDialog(
      value = date,
      onDismiss = { showPicker = false },
      onValueChange = {
        if (it != null) onDateChange(it)
        showPicker = false
      },
    )
  }
}
