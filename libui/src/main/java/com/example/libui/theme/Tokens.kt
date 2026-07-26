package com.example.libui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.ui.unit.dp

/** 4dp 基线间距阶梯。页面只从该阶梯取值，避免散落尺寸。 */
object AppSpacing {
  val xxs = 2.dp
  val xs = 4.dp
  val sm = 8.dp
  val md = 12.dp
  val lg = 16.dp
  val xl = 24.dp
  val xxl = 32.dp
}

/** 交互与布局尺寸。所有可点击控件的触控高度不低于 48dp。 */
object AppSize {
  val touchTarget = 48.dp
  val button = 48.dp
  val buttonLarge = 56.dp
  val iconSmall = 18.dp
  val icon = 24.dp
  val fab = 56.dp
  val progress = 8.dp
  val calendarCell = 48.dp
  val calendarSelection = 36.dp
  val timelineHourLabel = 52.dp
  val foodPreviewCard = 168.dp
}

object AppStroke {
  val default = 1.dp
  val selected = 2.dp
}

object AppElevation {
  val none = 0.dp
}

/**
 * Material 3 标准动效 token。
 *
 * 只用于页面级过渡；Button、Chip、FAB 等组件使用 Material 3 自带动效。
 */
object AppMotion {
  const val durationShort = 100
  const val durationMedium = 220
  const val durationNavigation = 280

  val standard = CubicBezierEasing(0.2f, 0f, 0f, 1f)
  val standardAccelerate = CubicBezierEasing(0.3f, 0f, 1f, 1f)
  val emphasizedDecelerate = CubicBezierEasing(0.05f, 0.7f, 0.1f, 1f)
}
