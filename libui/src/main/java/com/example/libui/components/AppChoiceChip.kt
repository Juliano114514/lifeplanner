package com.example.libui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppStroke

@Composable
fun AppChoiceChip(
  text: String,
  selected: Boolean,
  onClick: () -> Unit,
  modifier: Modifier = Modifier,
  enabled: Boolean = true,
) {
  val scheme = MaterialTheme.colorScheme
  FilterChip(
    selected = selected,
    onClick = onClick,
    modifier = modifier.defaultMinSize(minHeight = AppSize.touchTarget),
    enabled = enabled,
    label = { Text(text, style = MaterialTheme.typography.labelLarge) },
    leadingIcon = if (selected) {
      {
        Icon(
          imageVector = Icons.Rounded.Check,
          contentDescription = null,
          modifier = Modifier.size(AppSize.iconSmall),
        )
      }
    } else {
      null
    },
    shape = MaterialTheme.shapes.medium,
    colors = FilterChipDefaults.filterChipColors(
      containerColor = scheme.surface,
      labelColor = scheme.onSurfaceVariant,
      iconColor = scheme.primary,
      selectedContainerColor = scheme.primaryContainer,
      selectedLabelColor = scheme.onPrimaryContainer,
      selectedLeadingIconColor = scheme.primary,
    ),
    border = BorderStroke(
      width = if (selected) AppStroke.selected else AppStroke.default,
      color = if (selected) scheme.primary else scheme.outlineVariant,
    ),
  )
}
