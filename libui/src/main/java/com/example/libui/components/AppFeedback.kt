package com.example.libui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing
import com.example.libui.theme.LifePlannerDesign

@Composable
fun AppLoadingState(modifier: Modifier = Modifier) {
  Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
    CircularProgressIndicator()
  }
}

@Composable
fun AppEmptyState(
  title: String,
  message: String,
  modifier: Modifier = Modifier,
  icon: ImageVector? = null,
) {
  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(AppSpacing.xxl),
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm, Alignment.CenterVertically),
  ) {
    if (icon != null) {
      Surface(
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.primaryContainer,
        contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
      ) {
        Icon(
          imageVector = icon,
          contentDescription = null,
          modifier = Modifier.padding(AppSpacing.lg).size(AppSize.icon),
        )
      }
    }
    Text(
      text = title,
      style = MaterialTheme.typography.headlineSmall,
      textAlign = TextAlign.Center,
    )
    Text(
      text = message,
      style = MaterialTheme.typography.bodyMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      textAlign = TextAlign.Center,
    )
  }
}

@Composable
fun AppErrorState(
  message: String,
  modifier: Modifier = Modifier,
) {
  AppEmptyState(
    title = "暂时无法加载",
    message = message,
    modifier = modifier,
  )
}

@Composable
fun AppSectionHeader(
  title: String,
  modifier: Modifier = Modifier,
  count: Int? = null,
) {
  Row(
    modifier = modifier,
    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Text(title, style = MaterialTheme.typography.titleMedium)
    if (count != null) {
      Text(
        text = count.toString(),
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
    }
  }
}

enum class AppStatusTone {
  Neutral,
  Success,
  Warning,
  Error,
}

@Composable
fun AppStatusBadge(
  text: String,
  tone: AppStatusTone,
  modifier: Modifier = Modifier,
) {
  val scheme = MaterialTheme.colorScheme
  val extra = LifePlannerDesign.colors
  val (container: Color, content: Color) = when (tone) {
    AppStatusTone.Neutral -> scheme.surfaceVariant to scheme.onSurfaceVariant
    AppStatusTone.Success -> extra.successContainer to extra.onSuccessContainer
    AppStatusTone.Warning -> extra.warningContainer to extra.onWarningContainer
    AppStatusTone.Error -> scheme.errorContainer to scheme.onErrorContainer
  }
  Surface(
    modifier = modifier,
    shape = CircleShape,
    color = container,
    contentColor = content,
  ) {
    Text(
      text = text,
      style = MaterialTheme.typography.labelMedium,
      modifier = Modifier.padding(horizontal = AppSpacing.sm, vertical = AppSpacing.xs),
    )
  }
}
