package com.example.libui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.colorResource
import com.example.libui.R
import com.example.libui.theme.Dimens

/**
 * 3D 厚卡片：大圆角 + 底部硬阴影。
 * 可点击（[onClick] 非空）时复用 [TactileSurface] 的按压下沉手感，
 * 用于首页入口卡等可交互卡面；禁用态去除厚度。
 */
@Composable
fun ChunkyCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(Dimens.radiusCard)
  val scheme = MaterialTheme.colorScheme
  val faceColor = if (enabled) scheme.surface else scheme.surfaceVariant
  val depthColor = if (enabled) colorResource(R.color.depth_card) else Color.Transparent

  TactileSurface(
    onClick = { onClick?.invoke() },
    color = faceColor,
    depthColor = depthColor,
    shape = shape,
    depth = Dimens.depthCard,
    enabled = enabled && onClick != null,
    modifier = modifier,
  ) {
    Box { content() }
  }
}
