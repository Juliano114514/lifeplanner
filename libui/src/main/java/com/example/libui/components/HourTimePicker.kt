package com.example.libui.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.roundToInt

/**
 * 归家时间选择：大号时间数字（换值上滑切换）+ 厚滑轨。
 * 保留原吸附 steps 逻辑。
 */
@Composable
fun HourTimePicker(
  selectedHour: Int?,
  onHourSelected: (Int) -> Unit,
  modifier: Modifier = Modifier,
  hourRange: IntRange = 6..23,
) {
  val hours = hourRange.toList()
  val currentHour = selectedHour ?: hours.first()
  val sliderIndex = hours.indexOf(currentHour).coerceAtLeast(0).toFloat()
  val scheme = MaterialTheme.colorScheme

  Column(modifier = modifier.fillMaxWidth()) {
    AnimatedContent(
      targetState = currentHour,
      transitionSpec = {
        (slideInVertically { -it } + fadeIn()) togetherWith
          (slideOutVertically { it } + fadeOut())
      },
      label = "hour",
    ) { hour ->
      Text(
        text = "%02d:00".format(hour),
        color = scheme.primary,
        fontSize = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        modifier = Modifier.padding(bottom = 4.dp),
      )
    }
    Text(
      text = "预计到家时间",
      style = MaterialTheme.typography.bodyMedium,
      color = scheme.onSurfaceVariant,
      modifier = Modifier.padding(bottom = 16.dp),
    )
    Slider(
      value = sliderIndex,
      onValueChange = { value ->
        onHourSelected(hours[value.roundToInt().coerceIn(0, hours.lastIndex)])
      },
      valueRange = 0f..hours.lastIndex.toFloat(),
      steps = (hours.size - 2).coerceAtLeast(0),
      colors = SliderDefaults.colors(
        thumbColor = scheme.primary,
        activeTrackColor = scheme.primary,
        inactiveTrackColor = scheme.surfaceVariant,
      ),
      modifier = Modifier.fillMaxWidth(),
    )
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      Text(
        text = "%02d:00".format(hours.first()),
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant,
      )
      Text(
        text = "%02d:00".format(hours.last()),
        style = MaterialTheme.typography.labelSmall,
        color = scheme.onSurfaceVariant,
      )
    }
  }
}
