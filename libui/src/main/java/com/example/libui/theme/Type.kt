package com.example.libui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp

/** 基于 Material 3 命名的精简字体阶梯，使用系统字体并支持系统字号缩放。 */
val AppTypography = Typography(
  displaySmall = appTextStyle(FontWeight.ExtraBold, 36.sp, 44.sp),
  headlineLarge = appTextStyle(FontWeight.Bold, 32.sp, 40.sp),
  headlineMedium = appTextStyle(FontWeight.Bold, 28.sp, 36.sp),
  headlineSmall = appTextStyle(FontWeight.Bold, 24.sp, 32.sp),
  titleLarge = appTextStyle(FontWeight.Bold, 22.sp, 28.sp),
  titleMedium = appTextStyle(FontWeight.SemiBold, 16.sp, 24.sp),
  titleSmall = appTextStyle(FontWeight.SemiBold, 14.sp, 20.sp),
  bodyLarge = appTextStyle(FontWeight.Normal, 16.sp, 24.sp),
  bodyMedium = appTextStyle(FontWeight.Normal, 14.sp, 20.sp),
  bodySmall = appTextStyle(FontWeight.Normal, 12.sp, 16.sp),
  labelLarge = appTextStyle(FontWeight.Bold, 14.sp, 20.sp),
  labelMedium = appTextStyle(FontWeight.SemiBold, 12.sp, 16.sp),
)

private fun appTextStyle(
  weight: FontWeight,
  size: TextUnit,
  lineHeight: TextUnit,
) = TextStyle(
  fontFamily = FontFamily.Default,
  fontWeight = weight,
  fontSize = size,
  lineHeight = lineHeight,
)
