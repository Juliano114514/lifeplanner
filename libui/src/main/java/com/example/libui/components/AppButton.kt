package com.example.libui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.libui.theme.AppDepth
import com.example.libui.theme.AppMotion
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing
import com.example.libui.theme.AppStroke
import com.example.libui.theme.LifePlannerDesign

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
  val extra = LifePlannerDesign.colors
  val shape = MaterialTheme.shapes.medium
  val effectiveEnabled = enabled && !loading
  val tactile = variant == AppButtonVariant.Primary || variant == AppButtonVariant.Danger
  val faceColor = when (variant) {
    AppButtonVariant.Primary -> scheme.primary
    AppButtonVariant.Secondary -> scheme.secondaryContainer
    AppButtonVariant.Outline, AppButtonVariant.Text -> Color.Transparent
    AppButtonVariant.Danger -> scheme.error
  }
  val contentColor = when (variant) {
    AppButtonVariant.Primary -> scheme.onPrimary
    AppButtonVariant.Secondary -> scheme.onSecondaryContainer
    AppButtonVariant.Outline, AppButtonVariant.Text -> scheme.primary
    AppButtonVariant.Danger -> scheme.onError
  }
  val border = when (variant) {
    AppButtonVariant.Outline -> BorderStroke(AppStroke.default, scheme.outline)
    else -> null
  }
  val resolvedFace = if (effectiveEnabled) faceColor else scheme.surfaceVariant
  val resolvedContent = if (effectiveEnabled) contentColor else scheme.onSurfaceVariant.copy(alpha = 0.6f)

  val content: @Composable () -> Unit = {
    Row(
      modifier = Modifier.padding(horizontal = size.horizontalPadding),
      horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      when {
        loading -> CircularProgressIndicator(
          modifier = Modifier.size(AppSize.iconSmall),
          color = resolvedContent,
          strokeWidth = 2.dp,
        )
        leadingIcon != null -> androidx.compose.material3.Icon(
          imageVector = leadingIcon,
          contentDescription = null,
          modifier = Modifier.size(AppSize.iconSmall),
        )
      }
      Text(text = text, style = MaterialTheme.typography.labelLarge)
    }
  }

  if (tactile) {
    TactileButton(
      onClick = onClick,
      faceColor = resolvedFace,
      depthColor = if (variant == AppButtonVariant.Danger) extra.errorDepth else extra.primaryDepth,
      contentColor = resolvedContent,
      shape = shape,
      faceHeight = size.faceHeight,
      enabled = effectiveEnabled,
      modifier = modifier,
      content = content,
    )
  } else {
    Surface(
      onClick = onClick,
      modifier = modifier
        .minimumInteractiveComponentSize()
        .defaultMinSize(minWidth = AppSize.touchTarget)
        .height(size.faceHeight),
      enabled = effectiveEnabled,
      shape = shape,
      color = resolvedFace,
      contentColor = resolvedContent,
      border = border,
    ) {
      Box(contentAlignment = Alignment.Center) { content() }
    }
  }
}

@Composable
private fun TactileButton(
  onClick: () -> Unit,
  faceColor: Color,
  depthColor: Color,
  contentColor: Color,
  shape: Shape,
  faceHeight: Dp,
  enabled: Boolean,
  modifier: Modifier,
  content: @Composable () -> Unit,
) {
  val interaction = remember { MutableInteractionSource() }
  val pressed by interaction.collectIsPressedAsState()
  val depth = if (enabled) AppDepth.button else 0.dp
  val offset by animateDpAsState(
    targetValue = if (pressed) depth else 0.dp,
    animationSpec = tween(AppMotion.fast),
    label = "buttonPressOffset",
  )

  Box(
    modifier = modifier
      .minimumInteractiveComponentSize()
      .defaultMinSize(minWidth = AppSize.touchTarget)
      .height(faceHeight + depth)
      .clickable(
        interactionSource = interaction,
        indication = null,
        enabled = enabled,
        role = Role.Button,
        onClick = onClick,
      ),
  ) {
    if (enabled) {
      Box(
        Modifier
          .fillMaxWidth()
          .height(faceHeight)
          .offset(y = depth)
          .clip(shape)
          .background(depthColor),
      )
    }
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(faceHeight)
        .offset { IntOffset(x = 0, y = offset.roundToPx()) }
        .clip(shape)
        .background(faceColor)
        .then(
          if (enabled) Modifier
          else Modifier.border(AppStroke.default, MaterialTheme.colorScheme.outlineVariant, shape),
        ),
      contentAlignment = Alignment.Center,
    ) {
      androidx.compose.runtime.CompositionLocalProvider(
        androidx.compose.material3.LocalContentColor provides contentColor,
      ) {
        content()
      }
    }
  }
}
