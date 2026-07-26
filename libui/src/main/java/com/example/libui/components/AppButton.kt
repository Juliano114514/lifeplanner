package com.example.libui.components

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing

enum class AppButtonVariant {
  Primary,
  Secondary,
  Outline,
  Text,
  Danger,
}

enum class AppButtonSize(
  val faceHeight: Dp,
  val horizontalPadding: Dp,
) {
  Medium(AppSize.button, AppSpacing.lg),
  Large(AppSize.buttonLarge, AppSpacing.xl),
}

@Composable
fun AppButton(
  text: String,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  variant: AppButtonVariant = AppButtonVariant.Primary,
  size: AppButtonSize = AppButtonSize.Medium,
  enabled: Boolean = true,
  loading: Boolean = false,
  leadingIcon: ImageVector? = null,
) {
  val scheme = MaterialTheme.colorScheme
  val shape = MaterialTheme.shapes.medium
  val effectiveEnabled = enabled && !loading
  val buttonModifier = modifier
    .minimumInteractiveComponentSize()
    .defaultMinSize(minWidth = AppSize.touchTarget)
    .height(size.faceHeight)
  val contentPadding = PaddingValues(horizontal = size.horizontalPadding)
  val content: @Composable RowScope.() -> Unit = {
    when {
      loading -> CircularProgressIndicator(
        modifier = Modifier.size(AppSize.iconSmall),
        color = LocalContentColor.current,
        strokeWidth = 2.dp,
      )
      leadingIcon != null -> androidx.compose.material3.Icon(
        imageVector = leadingIcon,
        contentDescription = null,
        modifier = Modifier.size(AppSize.iconSmall),
      )
    }
    if (loading || leadingIcon != null) {
      Spacer(Modifier.width(AppSpacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.labelLarge)
  }

  when (variant) {
    AppButtonVariant.Primary -> Button(
      onClick = onClick,
      modifier = buttonModifier,
      enabled = effectiveEnabled,
      shape = shape,
      contentPadding = contentPadding,
      content = content,
    )
    AppButtonVariant.Secondary -> FilledTonalButton(
      onClick = onClick,
      modifier = buttonModifier,
      enabled = effectiveEnabled,
      shape = shape,
      contentPadding = contentPadding,
      content = content,
    )
    AppButtonVariant.Outline -> OutlinedButton(
      onClick = onClick,
      modifier = buttonModifier,
      enabled = effectiveEnabled,
      shape = shape,
      contentPadding = contentPadding,
      content = content,
    )
    AppButtonVariant.Text -> TextButton(
      onClick = onClick,
      modifier = buttonModifier,
      enabled = effectiveEnabled,
      shape = shape,
      contentPadding = contentPadding,
      content = content,
    )
    AppButtonVariant.Danger -> Button(
      onClick = onClick,
      modifier = buttonModifier,
      enabled = effectiveEnabled,
      shape = shape,
      colors = ButtonDefaults.buttonColors(
        containerColor = scheme.error,
        contentColor = scheme.onError,
        disabledContainerColor = scheme.onSurface.copy(alpha = 0.12f),
        disabledContentColor = scheme.onSurface.copy(alpha = 0.38f),
      ),
      contentPadding = contentPadding,
      content = content,
    )
  }
}
