package com.example.libui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.libui.theme.AppSpacing
import com.example.libui.theme.AppStroke
import com.example.libui.theme.LifePlannerDesign

enum class AppCardStyle {
  Default,
  Tonal,
  Selected,
}

@Composable
fun AppCard(
  modifier: Modifier = Modifier,
  onClick: (() -> Unit)? = null,
  enabled: Boolean = true,
  style: AppCardStyle = AppCardStyle.Default,
  contentPadding: PaddingValues = PaddingValues(AppSpacing.lg),
  content: @Composable ColumnScope.() -> Unit,
) {
  val scheme = MaterialTheme.colorScheme
  val extra = LifePlannerDesign.colors
  val color = when (style) {
    AppCardStyle.Default -> scheme.surface
    AppCardStyle.Tonal -> scheme.surfaceVariant.copy(alpha = 0.55f)
    AppCardStyle.Selected -> scheme.primaryContainer
  }
  val borderColor = when (style) {
    AppCardStyle.Selected -> scheme.primary
    else -> extra.cardBorder
  }
  val body: @Composable () -> Unit = {
    Column(Modifier.padding(contentPadding), content = content)
  }

  if (onClick == null) {
    Surface(
      modifier = modifier,
      shape = MaterialTheme.shapes.large,
      color = color,
      contentColor = scheme.onSurface,
      border = BorderStroke(AppStroke.default, borderColor),
      content = body,
    )
  } else {
    Surface(
      onClick = onClick,
      modifier = modifier,
      enabled = enabled,
      shape = MaterialTheme.shapes.large,
      color = color,
      contentColor = scheme.onSurface,
      border = BorderStroke(AppStroke.default, borderColor),
      content = body,
    )
  }
}
