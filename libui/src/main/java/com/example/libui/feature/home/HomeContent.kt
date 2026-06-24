package com.example.libui.feature.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.libui.components.ChunkyCard
import com.example.libui.theme.Dimens

@Composable
fun HomeContent(
  state: HomeUiState,
  onAction: (HomeAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(Dimens.cardPadding),
    verticalArrangement = Arrangement.spacedBy(16.dp),
  ) {
    Text(
      text = "生活规划",
      style = MaterialTheme.typography.headlineMedium,
      modifier = Modifier.padding(bottom = 8.dp),
    )
    EntryCard(
      emoji = "📅",
      title = "一天规划",
      subtitle = if (state.hasIncompletePlan) "继续未完成的规划" else "用卡片快速规划今天",
      onClick = { onAction(HomeAction.OnPlanClick) },
    )
    EntryCard(
      emoji = "📦",
      title = "库存统计",
      subtitle = "敬请期待",
      enabled = false,
      onClick = { onAction(HomeAction.OnInventoryClick) },
    )
  }
}

@Composable
private fun EntryCard(
  emoji: String,
  title: String,
  subtitle: String,
  onClick: () -> Unit,
  enabled: Boolean = true,
) {
  ChunkyCard(
    onClick = onClick,
    enabled = enabled,
    modifier = Modifier.fillMaxWidth(),
  ) {
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(20.dp),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(text = emoji, fontSize = 32.sp)
      Column(modifier = Modifier.padding(start = 16.dp)) {
        Text(
          text = title,
          style = MaterialTheme.typography.titleLarge,
          color = MaterialTheme.colorScheme.onBackground,
        )
        Text(
          text = subtitle,
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = 4.dp),
        )
      }
    }
  }
}
