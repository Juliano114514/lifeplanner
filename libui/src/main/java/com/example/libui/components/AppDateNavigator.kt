package com.example.libui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import com.example.libui.theme.AppMotion
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing
import com.example.libui.theme.AppStroke
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

@Composable
fun AppDateNavigator(
  date: LocalDate,
  onDateChange: (LocalDate) -> Unit,
  modifier: Modifier = Modifier,
  isDateRecorded: (LocalDate) -> Boolean = { false },
) {
  var showPicker by rememberSaveable { mutableStateOf(false) }
  var windowStart by remember { mutableStateOf(date.minusDays(INITIAL_WINDOW_RADIUS.toLong())) }
  var windowEnd by remember { mutableStateOf(date.plusDays(INITIAL_WINDOW_RADIUS.toLong())) }
  var centerRequest by remember { mutableIntStateOf(0) }
  val today = LocalDate.now()
  val dates = remember(windowStart, windowEnd) {
    val count = ChronoUnit.DAYS.between(windowStart, windowEnd).toInt() + 1
    List(count) { offset -> windowStart.plusDays(offset.toLong()) }
  }
  val listState = rememberLazyListState(
    initialFirstVisibleItemIndex = INITIAL_WINDOW_RADIUS - CENTER_ITEM_INDEX,
  )
  val openPicker = { showPicker = true }

  Column(modifier = modifier.fillMaxWidth()) {
    BoxWithConstraints(Modifier.fillMaxWidth()) {
      val itemWidth = maxWidth / VISIBLE_DATE_COUNT
      LazyRow(
        modifier = Modifier.fillMaxWidth(),
        state = listState,
      ) {
        items(
          items = dates,
          key = { it.toEpochDay() },
        ) { itemDate ->
          AppDateNavigatorItem(
            date = itemDate,
            isSelected = itemDate == date,
            isToday = itemDate == today,
            isRecorded = isDateRecorded(itemDate),
            width = itemWidth,
            onClick = { onDateChange(itemDate) },
            onLongClick = openPicker,
          )
        }
      }

      LaunchedEffect(date, centerRequest) {
        if (date.isBefore(windowStart) || date.isAfter(windowEnd)) {
          windowStart = date.minusDays(INITIAL_WINDOW_RADIUS.toLong())
          windowEnd = date.plusDays(INITIAL_WINDOW_RADIUS.toLong())
          withFrameNanos { }
        }
        val viewportWidth = snapshotFlow { listState.layoutInfo.viewportSize.width }
          .first { it > 0 }
        val itemWidthPx = viewportWidth / VISIBLE_DATE_COUNT
        val centeredOffset = -((viewportWidth - itemWidthPx) / 2)
        val targetIndex = ChronoUnit.DAYS.between(windowStart, date).toInt()
        listState.animateScrollToItem(targetIndex, centeredOffset)
      }

      LaunchedEffect(listState, dates.size) {
        snapshotFlow {
          val visibleItems = listState.layoutInfo.visibleItemsInfo
          when {
            visibleItems.isEmpty() -> NO_EXPANSION
            visibleItems.first().key == windowStart.toEpochDay() -> EXPAND_BEFORE
            visibleItems.last().key == windowEnd.toEpochDay() -> EXPAND_AFTER
            else -> NO_EXPANSION
          }
        }
          .distinctUntilChanged()
          .collect { direction ->
            when (direction) {
              EXPAND_BEFORE -> windowStart = windowStart.minusDays(WINDOW_PAGE_SIZE.toLong())
              EXPAND_AFTER -> windowEnd = windowEnd.plusDays(WINDOW_PAGE_SIZE.toLong())
            }
          }
      }
    }

    AppButton(
      text = "回今天",
      onClick = {
        if (date == today) {
          centerRequest += 1
        } else {
          onDateChange(today)
        }
      },
      modifier = Modifier.align(Alignment.Start),
      variant = AppButtonVariant.Text,
    )
  }

  if (showPicker) {
    AppDatePickerDialog(
      value = date,
      onDismiss = { showPicker = false },
      onValueChange = {
        if (it != null) onDateChange(it)
        showPicker = false
      },
    )
  }
}

@Composable
private fun AppDateNavigatorItem(
  date: LocalDate,
  isSelected: Boolean,
  isToday: Boolean,
  isRecorded: Boolean,
  width: Dp,
  onClick: () -> Unit,
  onLongClick: () -> Unit,
) {
  val selectedColor by animateColorAsState(
    targetValue = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
    animationSpec = tween(
      durationMillis = AppMotion.durationShort,
      easing = AppMotion.standard,
    ),
    label = "日期选中颜色",
  )
  val contentColor = if (isSelected) {
    MaterialTheme.colorScheme.onPrimary
  } else {
    MaterialTheme.colorScheme.onSurface
  }
  val todayBorder = if (isToday && !isSelected) {
    Modifier.border(AppStroke.today, MaterialTheme.colorScheme.outlineVariant, CircleShape)
  } else {
    Modifier
  }

  Column(
    modifier = Modifier
      .width(width)
      .combinedClickable(
        onClick = onClick,
        onLongClick = onLongClick,
        onLongClickLabel = "打开日期选择器",
      )
      .semantics(mergeDescendants = true) {
        contentDescription = date.accessibilityLabel(
          isSelected = isSelected,
          isToday = isToday,
          isRecorded = isRecorded,
        )
        selected = isSelected
      },
    horizontalAlignment = Alignment.CenterHorizontally,
    verticalArrangement = Arrangement.spacedBy(AppSpacing.xs),
  ) {
    Text(
      text = "${date.monthValue}月",
      style = MaterialTheme.typography.labelMedium,
      color = MaterialTheme.colorScheme.onSurfaceVariant,
      fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
    )
    Box(
      modifier = Modifier
        .size(AppSize.calendarCell)
        .then(todayBorder)
        .clip(CircleShape)
        .background(selectedColor),
      contentAlignment = Alignment.Center,
    ) {
      Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(AppSpacing.xxs),
      ) {
        Text(
          text = date.dayOfMonth.toString(),
          style = MaterialTheme.typography.labelLarge,
          color = contentColor,
          fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
        )
        Text(
          text = date.dayOfWeek.shortLabel(),
          style = MaterialTheme.typography.bodySmall,
          color = if (isSelected) {
            MaterialTheme.colorScheme.onPrimary
          } else {
            MaterialTheme.colorScheme.onSurfaceVariant
          },
        )
        Box(
          modifier = Modifier
            .size(AppSize.calendarRecordIndicator)
            .background(
              color = when {
                isSelected -> Color.Transparent
                isRecorded -> MaterialTheme.colorScheme.primary
                else -> MaterialTheme.colorScheme.outlineVariant
              },
              shape = CircleShape,
            ),
        )
      }
    }
  }
}

private fun LocalDate.accessibilityLabel(
  isSelected: Boolean,
  isToday: Boolean,
  isRecorded: Boolean,
): String = buildString {
  append(monthValue)
  append("月")
  append(dayOfMonth)
  append("日，")
  append(dayOfWeek.shortLabel())
  if (isToday) append("，今天")
  if (isRecorded) append("，有记录")
  if (isSelected) append("，已选择")
}

private fun DayOfWeek.shortLabel(): String = when (this) {
  DayOfWeek.MONDAY -> "周一"
  DayOfWeek.TUESDAY -> "周二"
  DayOfWeek.WEDNESDAY -> "周三"
  DayOfWeek.THURSDAY -> "周四"
  DayOfWeek.FRIDAY -> "周五"
  DayOfWeek.SATURDAY -> "周六"
  DayOfWeek.SUNDAY -> "周日"
}

private const val VISIBLE_DATE_COUNT = 5
private const val CENTER_ITEM_INDEX = VISIBLE_DATE_COUNT / 2
private const val INITIAL_WINDOW_RADIUS = 10
private const val WINDOW_PAGE_SIZE = 10
private const val NO_EXPANSION = 0
private const val EXPAND_BEFORE = -1
private const val EXPAND_AFTER = 1
