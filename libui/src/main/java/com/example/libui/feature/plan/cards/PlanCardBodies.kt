package com.example.libui.feature.plan.cards

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.plan.PlanCardDefinition
import com.example.foundation.domain.plan.activeFollowUp
import com.example.libui.components.TagChoiceGroup
import com.example.libui.theme.Dimens
import com.example.libui.theme.SectionTitleStyle

/**
 * 标签卡：主选（单/多）+ 条件二级选单。二级在满足触发条件时于卡内展开，
 * 不再切换整卡，避免横滑动画叠帧。
 */
@Composable
fun TagCard(
  definition: PlanCardDefinition,
  answer: PlanCardAnswer?,
  multiSelect: Boolean,
  onToggle: (String) -> Unit,
  onSelect: (String) -> Unit,
  onToggleFollowUp: (String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val selected = answer?.selectedOptions.orEmpty()
  val followUp = definition.activeFollowUp(selected)
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(vertical = Dimens.space8),
    verticalArrangement = Arrangement.spacedBy(Dimens.space24),
  ) {
    TagChoiceGroup(
      options = definition.options,
      selected = selected,
      multiSelect = multiSelect,
      onToggle = onToggle,
      onSelect = onSelect,
    )
    AnimatedVisibility(
      visible = followUp != null,
      enter = fadeIn() + expandVertically(),
      exit = fadeOut() + shrinkVertically(),
    ) {
      // 退场期间 followUp 可能为 null，回退到配置以保持内容稳定
      val spec = followUp ?: definition.followUp
      if (spec != null) {
        Column(verticalArrangement = Arrangement.spacedBy(Dimens.space12)) {
          Text(
            text = spec.title,
            style = SectionTitleStyle,
            color = MaterialTheme.colorScheme.onSurface,
          )
          TagChoiceGroup(
            options = spec.options,
            selected = answer?.subSelections.orEmpty(),
            multiSelect = spec.multiSelect,
            onToggle = onToggleFollowUp,
            onSelect = onToggleFollowUp,
          )
        }
      }
    }
  }
}

@Composable
fun OtherNoteCard(
  answer: PlanCardAnswer?,
  onNoteChange: (String) -> Unit,
  onAddExtraNote: () -> Unit,
  onExtraNoteChange: (Int, String) -> Unit,
  modifier: Modifier = Modifier,
) {
  val fieldShape = RoundedCornerShape(Dimens.radiusButton)
  Column(
    modifier = modifier.fillMaxWidth(),
    verticalArrangement = Arrangement.spacedBy(Dimens.space12),
  ) {
    OutlinedTextField(
      value = answer?.noteText.orEmpty(),
      onValueChange = onNoteChange,
      modifier = Modifier.fillMaxWidth(),
      label = { Text("今天有什么额外要做的事情") },
      placeholder = { Text("写下备注…") },
      shape = fieldShape,
      minLines = 2,
    )
    answer?.extraNotes.orEmpty().forEachIndexed { index, note ->
      OutlinedTextField(
        value = note,
        onValueChange = { onExtraNoteChange(index, it) },
        modifier = Modifier.fillMaxWidth(),
        label = { Text("额外事项 ${index + 1}") },
        shape = fieldShape,
        minLines = 2,
      )
    }
    TextButton(onClick = onAddExtraNote) {
      Icon(
        imageVector = Icons.Rounded.Add,
        contentDescription = null,
        modifier = Modifier.size(18.dp),
      )
      Text("  添加备注")
    }
  }
}
