package com.example.libui.theme

import android.content.res.Configuration
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.colorResource
import com.example.libui.R

@Composable
private fun appColorScheme(base: ColorScheme = lightColorScheme()): ColorScheme =
  base.copy(
    primary = colorResource(R.color.theme_primary),
    onPrimary = colorResource(R.color.theme_on_primary),
    primaryContainer = colorResource(R.color.theme_primary_light),
    onPrimaryContainer = colorResource(R.color.theme_primary_dark),
    secondary = colorResource(R.color.theme_primary),
    onSecondary = colorResource(R.color.theme_on_primary),
    secondaryContainer = colorResource(R.color.theme_primary_light),
    onSecondaryContainer = colorResource(R.color.theme_primary_dark),
    background = colorResource(R.color.bg_page),
    onBackground = colorResource(R.color.text_title),
    surface = colorResource(R.color.bg_card),
    onSurface = colorResource(R.color.text_body),
    surfaceVariant = colorResource(R.color.bg_elevated),
    onSurfaceVariant = colorResource(R.color.text_secondary),
    error = colorResource(R.color.status_error),
    onError = colorResource(R.color.theme_on_primary),
    outline = colorResource(R.color.border),
    outlineVariant = colorResource(R.color.divider),
  )

@Composable
fun LifeplannerTheme(
  darkTheme: Boolean = isSystemInDarkTheme(),
  content: @Composable () -> Unit,
) {
  val context = LocalContext.current
  val configuration = LocalConfiguration.current
  val themedConfiguration = remember(darkTheme, configuration) {
    Configuration(configuration).apply {
      uiMode = (uiMode and Configuration.UI_MODE_NIGHT_MASK.inv()) or
        if (darkTheme) Configuration.UI_MODE_NIGHT_YES else Configuration.UI_MODE_NIGHT_NO
    }
  }
  val themedContext = remember(themedConfiguration) {
    context.createConfigurationContext(themedConfiguration)
  }

  CompositionLocalProvider(
    LocalConfiguration provides themedConfiguration,
    LocalContext provides themedContext,
  ) {
    MaterialTheme(
      colorScheme = appColorScheme(),
      typography = Typography,
      content = content,
    )
  }
}
