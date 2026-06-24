package com.example.libui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.Redo
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import com.example.libui.R
import com.example.libui.theme.Dimens

/**
 * 卡片框架：厚卡面（圆角 + 底部硬阴影）填满容器，避免内容不足时露出底色。
 * 顶部为 M3 风格栏：两侧圆形图标按钮（上一张 / 跳过）+ 居中标题与步骤。
 * 保留下滑手势回退。
 */
@Composable
fun PlanCardFrame(
  title: String,
  step: String,
  onPrevious: () -> Unit,
  onSkip: () -> Unit,
  canGoPrevious: Boolean,
  modifier: Modifier = Modifier,
  onSwipeDown: (() -> Unit)? = null,
  content: @Composable () -> Unit,
) {
  val shape = RoundedCornerShape(Dimens.radiusCard)
  val scheme = MaterialTheme.colorScheme
  val swipeModifier =
    if (onSwipeDown != null && canGoPrevious) {
      Modifier.pointerInput(canGoPrevious) {
        detectVerticalDragGestures { _, dragAmount ->
          if (dragAmount > 24f) onSwipeDown()
        }
      }
    } else {
      Modifier
    }

  Box(modifier = modifier.padding(horizontal = Dimens.screenPadding)) {
    // 底部硬阴影层
    Box(
      modifier = Modifier
        .matchParentSize()
        .offset(y = Dimens.depthCard)
        .clip(shape)
        .background(colorResource(R.color.depth_card)),
    )
    Column(
      modifier = Modifier
        .fillMaxSize()
        .clip(shape)
        .background(scheme.surface)
        .then(swipeModifier)
        .padding(Dimens.cardPadding),
    ) {
      CardHeader(
        title = title,
        step = step,
        canGoPrevious = canGoPrevious,
        onPrevious = onPrevious,
        onSkip = onSkip,
      )
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .weight(1f)
          .padding(top = Dimens.space24),
        contentAlignment = Alignment.TopStart,
      ) {
        content()
      }
    }
  }
}

@Composable
private fun CardHeader(
  title: String,
  step: String,
  canGoPrevious: Boolean,
  onPrevious: () -> Unit,
  onSkip: () -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  Box(modifier = Modifier.fillMaxWidth()) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onPrevious, enabled = canGoPrevious) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
          contentDescription = "上一张",
          tint = if (canGoPrevious) scheme.onSurfaceVariant else scheme.outlineVariant,
        )
      }
      IconButton(onClick = onSkip) {
        Icon(
          imageVector = Icons.AutoMirrored.Rounded.Redo,
          contentDescription = "跳过",
          tint = scheme.onSurfaceVariant,
        )
      }
    }
    Column(
      modifier = Modifier.align(Alignment.Center),
      horizontalAlignment = Alignment.CenterHorizontally,
    ) {
      Text(
        text = title,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.SemiBold,
        color = scheme.onBackground,
        textAlign = TextAlign.Center,
      )
      Text(
        text = step,
        style = MaterialTheme.typography.labelMedium,
        color = scheme.onSurfaceVariant,
      )
    }
  }
}
