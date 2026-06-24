package com.example.libui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.libui.theme.Dimens

/**
 * 标签选择组：单/多选共用。内部以 [ChunkyChip] 渲染，
 * 对外 API 保持不变（multiSelect 决定走 onToggle 或 onSelect）。
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TagChoiceGroup(
  options: List<String>,
  selected: List<String>,
  multiSelect: Boolean,
  onToggle: (String) -> Unit,
  onSelect: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  FlowRow(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Dimens.gapTag),
    verticalArrangement = Arrangement.spacedBy(Dimens.gapTag),
  ) {
    options.forEach { option ->
      ChunkyChip(
        text = option,
        selected = option in selected,
        onClick = { if (multiSelect) onToggle(option) else onSelect(option) },
      )
    }
  }
}
