package com.example.libui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import kotlin.random.Random

private data class Confetto(
  val xRatio: Float,
  val color: Color,
  val rotation: Float,
  val horizontalDrift: Float,
  val delayFraction: Float,
)

private val confettiColors = listOf(
  Color(0xFF00DC5A),
  Color(0xFFFFC400),
  Color(0xFFFF5252),
  Color(0xFF40A9FF),
  Color(0xFFB37FEB),
)

/**
 * 纯 Compose Canvas 撒花，进入时一次性触发，零三方依赖。
 * 约 30 个彩色小方块从顶部下落 + 旋转 + 渐隐，用于规划完成的正反馈。
 */
@Composable
fun ConfettiBurst(
  modifier: Modifier = Modifier,
  pieceCount: Int = 30,
) {
  val pieces = remember {
    List(pieceCount) {
      Confetto(
        xRatio = Random.nextFloat(),
        color = confettiColors[Random.nextInt(confettiColors.size)],
        rotation = Random.nextFloat() * 360f,
        horizontalDrift = (Random.nextFloat() - 0.5f) * 0.2f,
        delayFraction = Random.nextFloat() * 0.3f,
      )
    }
  }
  val progress = remember { Animatable(0f) }
  LaunchedEffect(Unit) {
    progress.animateTo(1f, animationSpec = tween(1200))
  }

  Canvas(modifier = modifier.fillMaxSize()) {
    val pieceSize = 12.dp.toPx()
    pieces.forEach { piece ->
      val local = ((progress.value - piece.delayFraction) / (1f - piece.delayFraction))
        .coerceIn(0f, 1f)
      if (local <= 0f) return@forEach
      val x = (piece.xRatio + piece.horizontalDrift * local) * size.width
      val y = local * (size.height + pieceSize)
      val alpha = (1f - local).coerceIn(0f, 1f)
      rotate(degrees = piece.rotation + local * 360f, pivot = Offset(x, y)) {
        drawRect(
          color = piece.color.copy(alpha = alpha),
          topLeft = Offset(x - pieceSize / 2, y - pieceSize / 2),
          size = Size(pieceSize, pieceSize),
        )
      }
    }
  }
}
