package com.example.libui.theme

import androidx.compose.ui.unit.dp

/**
 * 设计系统的尺寸与节奏 token，统一对齐 Material 3 的 8dp 栅格。
 * 组件统一引用，避免散落的魔法数字，便于全局调参与扩展。
 */
object Dimens {
  // 间距阶梯（8dp 栅格）
  val space4 = 4.dp
  val space8 = 8.dp
  val space12 = 12.dp
  val space16 = 16.dp
  val space24 = 24.dp

  // 圆角阶梯（对齐 M3 shape scale）
  val radiusChip = 16.dp
  val radiusButton = 16.dp
  val radiusCard = 24.dp

  // 3D 厚度：主体与底部硬阴影的垂直偏移
  val depthButton = 4.dp
  val depthCard = 4.dp
  val depthChip = 3.dp

  // 语义化节奏
  val gapTag = space8
  val cardPadding = space24
  val screenPadding = space16
}

/** 动效时长（毫秒）token，保证全链路节奏一致。 */
object Motion {
  const val FAST = 120
  const val MEDIUM = 240
  const val SLOW = 400
}
