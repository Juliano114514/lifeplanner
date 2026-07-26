package com.example.libui.theme

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
  val timelineHourLabel = 52.dp
}

object AppStroke {
  val default = 1.dp
  val selected = 2.dp
}

object AppDepth {
  val button = 4.dp
}

object AppElevation {
  val none = 0.dp
}

/** 只保留三档动效；普通状态变化不超过 300ms。 */
object AppMotion {
  const val fast = 100
  const val standard = 180
  const val emphasized = 300
}
