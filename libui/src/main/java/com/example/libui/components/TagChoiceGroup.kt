package com.example.libui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.libui.theme.Dimens

/**
 * 标签选择组：单/多选共用，双列栅格（每行最多 2 项）。
 * 内部以 [ChunkyChip] 渲染；选中态对勾 fade 入场，格宽由 weight 固定，避免换行挤压。
 */
@Composable
fun TagChoiceGroup(
  options: List<String>,
  selected: List<String>,
  multiSelect: Boolean,
  onToggle: (String) -> Unit,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
  columns: Int = 2,
) {
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Dimens.gapTag),
  ) {
    options.chunked(columns).forEach { rowOptions ->
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(Dimens.gapTag),
      ) {
        rowOptions.forEach { option ->
          ChunkyChip(
            text = option,
            selected = option in selected,
            onClick = { if (multiSelect) onToggle(option) else onSelect(option) },
            modifier = Modifier.weight(1f),
            fillCell = true,
          )
        }
        repeat(columns - rowOptions.size) {
          Spacer(modifier = Modifier.weight(1f))
        }
      }
    }
  }
}
