package com.example.libui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.libui.R
import com.example.libui.theme.Dimens

/** 厚按钮的两种视觉：实心主操作 / 描边次操作。 */
enum class ChunkyStyle { Primary, Outline }

/**
 * 多邻国式 3D 厚按钮。所有主操作（下一张 / 完成 / 复制 / 返回）统一走此组件。
 * 视觉与按压手感由 [TactileSurface] 提供，本组件只负责配色与文字。
 */
@Composable
fun ChunkyButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  style: ChunkyStyle = ChunkyStyle.Primary,
  enabled: Boolean = true,
) {
  val shape = RoundedCornerShape(Dimens.radiusButton)
  val isPrimary = style == ChunkyStyle.Primary
  val scheme = MaterialTheme.colorScheme

  val faceColor = when {
    !enabled -> scheme.surfaceVariant
    isPrimary -> scheme.primary
    else -> scheme.surface
  }
  val depthColor = when {
    !enabled -> Color.Transparent
    isPrimary -> colorResource(R.color.depth_primary)
    else -> colorResource(R.color.depth_surface)
  }
  val textColor = when {
    !enabled -> scheme.onSurfaceVariant
    isPrimary -> scheme.onPrimary
    else -> scheme.primary
  }
  val borderModifier =
    if (!isPrimary && enabled) Modifier.border(BorderStroke(2.dp, scheme.primary), shape)
    else Modifier

  TactileSurface(
    onClick = onClick,
    color = faceColor,
    depthColor = depthColor,
    shape = shape,
    depth = Dimens.depthButton,
    enabled = enabled,
    modifier = modifier,
  ) {
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(54.dp)
        .then(borderModifier)
        .padding(horizontal = 24.dp),
      contentAlignment = Alignment.Center,
    ) {
      Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = textColor,
        textAlign = TextAlign.Center,
      )
    }
  }
}
