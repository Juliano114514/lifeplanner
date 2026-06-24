package com.example.libui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.libui.theme.Dimens
import com.example.libui.theme.Motion

/**
 * 顶部进度：分段圆角条，每段对应一张卡。已完成 / 当前段填品牌色并平滑过渡。
 */
@Composable
fun PlanProgressBar(
  currentIndex: Int,
  totalCount: Int,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val shape = RoundedCornerShape(percent = 50)
  Row(
    modifier = modifier.fillMaxWidth(),
    horizontalArrangement = Arrangement.spacedBy(Dimens.space4),
  ) {
    repeat(totalCount) { index ->
      val active = index <= currentIndex
      val color by animateColorAsState(
        targetValue = if (active) scheme.primary else scheme.surfaceVariant,
        animationSpec = tween(Motion.MEDIUM),
        label = "segment",
      )
      Box(
        modifier = Modifier
          .weight(1f)
          .height(6.dp)
          .clip(shape)
          .background(color),
      )
    }
  }
}
