package com.example.libui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
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
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.libui.R
import com.example.libui.theme.Dimens

/**
 * 胶囊形选项 chip，替换 Material FilterChip。
 * 未选：surface + outline 描边；选中：品牌绿浅底 + 绿描边 + 厚度底影 + 前置勾。
 * 对勾仅用 fade，不用横向展开，避免挤压同行选项。内容在格内居中，选中时外框宽度不变。
 */
@Composable
fun ChunkyChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  fillCell: Boolean = false,
) {
  val shape = RoundedCornerShape(Dimens.radiusChip)
  val scheme = MaterialTheme.colorScheme

  val faceColor = if (selected) scheme.primaryContainer else scheme.surface
  val depthColor = if (selected) colorResource(R.color.depth_primary) else Color.Transparent
  val textColor = if (selected) scheme.primary else scheme.onSurfaceVariant
  val borderStroke = if (selected) {
    BorderStroke(Dimens.chipBorderSelected, scheme.primary)
  } else {
    BorderStroke(Dimens.chipBorderUnselected, scheme.outline)
  }

  TactileSurface(
    onClick = onClick,
    color = faceColor,
    depthColor = depthColor,
    shape = shape,
    depth = if (selected) Dimens.depthChip else 0.dp,
    modifier = if (fillCell) modifier.fillMaxWidth() else modifier,
  ) {
    Box(
      modifier = Modifier
        .then(if (fillCell) Modifier.fillMaxWidth() else Modifier)
        .border(borderStroke, shape)
        .padding(horizontal = Dimens.space16, vertical = Dimens.space12),
      contentAlignment = Alignment.Center,
    ) {
      Row(
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
      ) {
        AnimatedVisibility(
          visible = selected,
          enter = fadeIn(),
          exit = fadeOut(),
        ) {
          Icon(
            imageVector = Icons.Rounded.Check,
            contentDescription = null,
            tint = scheme.primary,
            modifier = Modifier
              .padding(end = Dimens.chipIconGap)
              .size(Dimens.chipIconSize),
          )
        }
        Text(
          text = text,
          style = MaterialTheme.typography.labelLarge,
          color = textColor,
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
        )
      }
    }
  }
}
