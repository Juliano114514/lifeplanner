package com.example.libui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color

internal val LightColorScheme = lightColorScheme(
  primary = Color(0xFF167A45),
  onPrimary = Color.White,
  primaryContainer = Color(0xFFB7F4CF),
  onPrimaryContainer = Color(0xFF073821),
  secondary = Color(0xFF3B6473),
  onSecondary = Color.White,
  secondaryContainer = Color(0xFFD7EDF5),
  onSecondaryContainer = Color(0xFF102F39),
  tertiary = Color(0xFF805600),
  onTertiary = Color.White,
  tertiaryContainer = Color(0xFFFFDEA3),
  onTertiaryContainer = Color(0xFF2A1A00),
  error = Color(0xFFBA1A1A),
  onError = Color.White,
  errorContainer = Color(0xFFFFDAD6),
  onErrorContainer = Color(0xFF410002),
  background = Color(0xFFF7F9F7),
  onBackground = Color(0xFF181D1A),
  surface = Color.White,
  onSurface = Color(0xFF181D1A),
  surfaceVariant = Color(0xFFE1E9E3),
  onSurfaceVariant = Color(0xFF414943),
  outline = Color(0xFF717971),
  outlineVariant = Color(0xFFC1C9C2),
)

internal val DarkColorScheme = darkColorScheme(
  primary = Color(0xFF8CDBAA),
  onPrimary = Color(0xFF00391D),
  primaryContainer = Color(0xFF00522C),
  onPrimaryContainer = Color(0xFFA7F8C5),
  secondary = Color(0xFFB5CBD5),
  onSecondary = Color(0xFF20343C),
  secondaryContainer = Color(0xFF374A53),
  onSecondaryContainer = Color(0xFFD1E7F0),
  tertiary = Color(0xFFFFBA42),
  onTertiary = Color(0xFF452B00),
  tertiaryContainer = Color(0xFF624000),
  onTertiaryContainer = Color(0xFFFFDEA3),
  error = Color(0xFFFFB4AB),
  onError = Color(0xFF690005),
  errorContainer = Color(0xFF93000A),
  onErrorContainer = Color(0xFFFFDAD6),
  background = Color(0xFF101512),
  onBackground = Color(0xFFDFE4DF),
  surface = Color(0xFF171C19),
  onSurface = Color(0xFFDFE4DF),
  surfaceVariant = Color(0xFF414943),
  onSurfaceVariant = Color(0xFFC1C9C2),
  outline = Color(0xFF8B938C),
  outlineVariant = Color(0xFF414943),
)

@Immutable
data class ExtendedColors(
  val success: Color,
  val onSuccess: Color,
  val successContainer: Color,
  val onSuccessContainer: Color,
  val warning: Color,
  val warningContainer: Color,
  val onWarningContainer: Color,
  val primaryDepth: Color,
  val errorDepth: Color,
  val cardBorder: Color,
)

internal val LightExtendedColors = ExtendedColors(
  success = Color(0xFF167A45),
  onSuccess = Color.White,
  successContainer = Color(0xFFB7F4CF),
  onSuccessContainer = Color(0xFF073821),
  warning = Color(0xFF805600),
  warningContainer = Color(0xFFFFDEA3),
  onWarningContainer = Color(0xFF2A1A00),
  primaryDepth = Color(0xFF0D5630),
  errorDepth = Color(0xFF7D1010),
  cardBorder = Color(0xFFD9E1DA),
)

internal val DarkExtendedColors = ExtendedColors(
  success = Color(0xFF8CDBAA),
  onSuccess = Color(0xFF00391D),
  successContainer = Color(0xFF00522C),
  onSuccessContainer = Color(0xFFA7F8C5),
  warning = Color(0xFFFFBA42),
  warningContainer = Color(0xFF624000),
  onWarningContainer = Color(0xFFFFDEA3),
  primaryDepth = Color(0xFF003E20),
  errorDepth = Color(0xFF690005),
  cardBorder = Color(0xFF364039),
)
