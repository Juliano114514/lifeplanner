package com.example.libui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.dp
import com.example.libui.R
import com.example.libui.theme.Dimens

/**
 * 胶囊形选项 chip，替换 Material FilterChip。
 * 选中态：品牌绿浅底 + 绿描边 + 厚度底影 + 前置勾；未选态：中性浅底、无厚度。
 * 仅用轻量的勾入场动画，避免每帧 scale 弹簧造成换卡时的叠帧卡顿。
 */
@Composable
fun ChunkyChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
) {
  val shape = RoundedCornerShape(Dimens.radiusChip)
  val scheme = MaterialTheme.colorScheme

  val faceColor = if (selected) scheme.primaryContainer else scheme.surfaceVariant
  val depthColor = if (selected) colorResource(R.color.depth_primary) else Color.Transparent
  val textColor = if (selected) scheme.primary else scheme.onSurfaceVariant
  val borderModifier =
    if (selected) Modifier.border(BorderStroke(2.dp, scheme.primary), shape) else Modifier

  TactileSurface(
    onClick = onClick,
    color = faceColor,
    depthColor = depthColor,
    shape = shape,
    depth = Dimens.depthChip,
    modifier = modifier,
  ) {
    Row(
      modifier = Modifier
        .then(borderModifier)
        .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      AnimatedVisibility(
        visible = selected,
        enter = fadeIn() + expandHorizontally(),
        exit = fadeOut() + shrinkHorizontally(),
      ) {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          tint = scheme.primary,
          modifier = Modifier
            .padding(end = Dimens.space4)
            .size(18.dp),
        )
      }
      Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = textColor,
      )
    }
  }
}
