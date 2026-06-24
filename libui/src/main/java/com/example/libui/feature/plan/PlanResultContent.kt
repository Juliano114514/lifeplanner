package com.example.libui.feature.plan

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.foundation.util.DateTimeUtil
import com.example.libui.components.ChunkyButton
import com.example.libui.components.ChunkyStyle
import com.example.libui.components.ConfettiBurst
import com.example.libui.theme.Dimens

@Composable
fun PlanResultContent(
  state: PlanResultUiState,
  onAction: (PlanResultAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (state.isLoading) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  val scheme = MaterialTheme.colorScheme
  Box(modifier = modifier.fillMaxSize()) {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(Dimens.cardPadding),
      verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
      AchievementHeader(date = state.date)
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
          .clip(RoundedCornerShape(Dimens.radiusCard))
          .background(scheme.surface)
          .padding(20.dp),
      ) {
        Text(
          text = state.summaryText,
          style = MaterialTheme.typography.bodyLarge,
          color = scheme.onSurface,
          modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState()),
        )
      }
      ChunkyButton(
        text = "复制到剪贴板",
        onClick = { onAction(PlanResultAction.Copy) },
        modifier = Modifier.fillMaxWidth(),
      )
      ChunkyButton(
        text = "返回首页",
        onClick = { onAction(PlanResultAction.Done) },
        style = ChunkyStyle.Outline,
        modifier = Modifier.fillMaxWidth(),
      )
    }
    // 撒花覆盖在最上层，不拦截点击
    ConfettiBurst()
  }
}

@Composable
private fun AchievementHeader(date: String) {
  val scheme = MaterialTheme.colorScheme
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(48.dp)
        .clip(CircleShape)
        .background(scheme.primary),
      contentAlignment = Alignment.Center,
    ) {
      Icon(
        imageVector = Icons.Rounded.Check,
        contentDescription = null,
        tint = scheme.onPrimary,
        modifier = Modifier.size(28.dp),
      )
    }
    Column(modifier = Modifier.padding(start = 12.dp)) {
      Text(
        text = "今日规划完成 🎉",
        style = MaterialTheme.typography.headlineSmall,
        color = scheme.onBackground,
      )
      Text(
        text = DateTimeUtil.formatDate(date),
        style = MaterialTheme.typography.bodyMedium,
        color = scheme.onSurfaceVariant,
      )
    }
  }
}
