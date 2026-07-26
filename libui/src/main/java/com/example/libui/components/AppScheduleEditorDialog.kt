package com.example.libui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.example.libui.theme.AppSpacing

@Composable
fun AppScheduleEditorDialog(
  dialogTitle: String,
  initialTitle: String,
  initialNote: String,
  initialStartMinute: Int,
  initialEndMinute: Int,
  detailsEditable: Boolean,
  lockedDetailsMessage: String? = null,
  onDismiss: () -> Unit,
  onSave: (String, String, Int, Int) -> Unit,
  onDelete: (() -> Unit)? = null,
) {
  var title by remember(initialTitle) { mutableStateOf(initialTitle) }
  var note by remember(initialNote) { mutableStateOf(initialNote) }
  var start by remember(initialStartMinute) { mutableIntStateOf(initialStartMinute) }
  var end by remember(initialEndMinute) { mutableIntStateOf(initialEndMinute) }
  var error by remember { mutableStateOf<String?>(null) }
  var confirmDelete by remember { mutableStateOf(false) }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(dialogTitle) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        if (detailsEditable) {
          OutlinedTextField(
            value = title,
            onValueChange = { title = it },
            label = { Text("标题") },
            modifier = Modifier.fillMaxWidth(),
          )
          OutlinedTextField(
            value = note,
            onValueChange = { note = it },
            label = { Text("备注") },
            modifier = Modifier.fillMaxWidth(),
          )
        } else {
          lockedDetailsMessage?.let { Text(it) }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
          AppTimePickerField(
            valueMinutes = start,
            onValueChange = { start = it ?: start },
            label = "开始时间",
            modifier = Modifier.weight(1f),
            minuteStep = 15,
          )
          AppTimePickerField(
            valueMinutes = end,
            onValueChange = { end = it ?: end },
            label = "结束时间",
            modifier = Modifier.weight(1f),
            minuteStep = 15,
            allowEndOfDay = true,
          )
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      }
    },
    confirmButton = {
      TextButton(
        onClick = {
          when {
            end <= start -> error = "结束时间应晚于开始时间"
            detailsEditable && title.isBlank() -> error = "请输入标题"
            else -> onSave(title, note, start, end)
          }
        },
      ) {
        Text("保存")
      }
    },
    dismissButton = {
      Row {
        if (onDelete != null) {
          TextButton(onClick = { confirmDelete = true }) {
            Text("删除", color = MaterialTheme.colorScheme.error)
          }
        }
        TextButton(onClick = onDismiss) {
          Text("取消")
        }
      }
    },
  )

  if (confirmDelete && onDelete != null) {
    AlertDialog(
      onDismissRequest = { confirmDelete = false },
      title = { Text("删除日程？") },
      text = { Text("只会删除当前日程，不会删除关联任务或快速安排内容。") },
      confirmButton = {
        TextButton(onClick = onDelete) {
          Text("删除", color = MaterialTheme.colorScheme.error)
        }
      },
      dismissButton = {
        TextButton(onClick = { confirmDelete = false }) {
          Text("取消")
        }
      },
    )
  }
}
