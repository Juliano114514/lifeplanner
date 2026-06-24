package com.example.libui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.libui.theme.Motion

/**
 * 可按压的 3D 厚面：主体下方压一层同色系深色底块形成"厚度"，
 * 按下时主体下沉覆盖底块 = 物理按压手感。Chunky 设计语言的地基，
 * 被 [ChunkyButton] / [ChunkyChip] / [ChunkyCard] 复用。
 *
 * 总高度恒为「内容高 + [depth]」，按压只改变主体偏移，不会引起兄弟节点抖动。
 */
@Composable
fun TactileSurface(
  onClick: () -> Unit,
  color: Color,
  depthColor: Color,
  shape: Shape,
  modifier: Modifier = Modifier,
  depth: Dp = 4.dp,
  enabled: Boolean = true,
  content: @Composable () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val raised = enabled && !pressed
  val bodyOffset by animateDpAsState(
    targetValue = if (raised) 0.dp else depth,
    animationSpec = tween(Motion.FAST),
    label = "tactileOffset",
  )

  Box(
    modifier
      .padding(bottom = depth) // 为底部硬阴影预留恒定空间
      .clickable(
        interactionSource = interaction,
        indication = null, // 下沉动画即反馈，不叠加水波纹
        enabled = enabled,
        onClick = onClick,
      ),
  ) {
    Box(
      Modifier
        .matchParentSize()
        .offset(y = depth)
        .clip(shape)
        .background(depthColor),
    )
    Box(
      Modifier
        .offset(y = bodyOffset)
        .clip(shape)
        .background(color),
    ) {
      content()
    }
  }
}
