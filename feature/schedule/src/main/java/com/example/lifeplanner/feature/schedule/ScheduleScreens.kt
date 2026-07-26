package com.example.lifeplanner.feature.schedule

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.ChevronLeft
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanInteraction
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.libui.components.AppButton
import com.example.libui.components.AppButtonVariant
import com.example.libui.components.AppCard
import com.example.libui.components.AppCardStyle
import com.example.libui.components.AppChoiceChip
import com.example.libui.components.AppFab
import com.example.libui.components.AppLoadingState
import com.example.libui.components.AppStatusBadge
import com.example.libui.components.AppStatusTone
import com.example.libui.components.AppTopBar
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle
import java.util.Locale
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScheduleRoute(
  initialEpochDay: Long?,
  taskOccurrenceId: Long?,
  onOpenQuickPlan: (Long) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ScheduleViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var editor by remember { mutableStateOf<EditorTarget?>(null) }
  LaunchedEffect(initialEpochDay, taskOccurrenceId) {
    viewModel.initialize(initialEpochDay)
    if (taskOccurrenceId != null) editor = EditorTarget(occurrenceId = taskOccurrenceId)
  }

  Scaffold(
    modifier = modifier,
    topBar = { AppTopBar("一日安排") },
    floatingActionButton = {
      AppFab(
        icon = Icons.Rounded.Add,
        contentDescription = "新增日程",
        onClick = { editor = EditorTarget() },
      )
    },
  ) { padding ->
    if (state.isLoading) {
      AppLoadingState(Modifier.padding(padding))
    } else {
      Column(Modifier.fillMaxSize().padding(padding)) {
        MonthCalendar(
          month = state.visibleMonth,
          selected = state.selectedDate,
          onSelect = viewModel::selectDate,
          onPrevious = { viewModel.changeMonth(-1) },
          onNext = { viewModel.changeMonth(1) },
        )
        Row(
          Modifier.fillMaxWidth().padding(horizontal = AppSpacing.lg, vertical = AppSpacing.sm),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically,
        ) {
          Text(
            state.selectedDate.format(DateTimeFormatter.ofPattern("M 月 d 日")),
            style = MaterialTheme.typography.titleLarge,
          )
          AppButton(
            text = "快速向导",
            onClick = { onOpenQuickPlan(state.selectedDate.toEpochDay()) },
            variant = AppButtonVariant.Secondary,
            leadingIcon = Icons.Rounded.AutoAwesome,
          )
        }
        Timeline(
          blocks = state.daySchedule.blocks,
          conflicts = state.daySchedule.conflictingBlockIds,
          onClick = { editor = EditorTarget(block = it, occurrenceId = it.taskOccurrenceId) },
          onComplete = viewModel::completeTask,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }

  editor?.let { target ->
    ScheduleEditorDialog(
      target = target,
      onDismiss = { editor = null },
      onDelete = {
        target.block?.id?.let(viewModel::archiveBlock)
        editor = null
      },
      onSave = { title, note, start, end ->
        viewModel.saveBlock(
          id = target.block?.id,
          title = title,
          note = note,
          startMinute = start,
          endMinute = end,
          occurrenceId = target.occurrenceId,
        )
        editor = null
      },
    )
  }
}

@Composable
private fun MonthCalendar(
  month: YearMonth,
  selected: LocalDate,
  onSelect: (LocalDate) -> Unit,
  onPrevious: () -> Unit,
  onNext: () -> Unit,
) {
  val firstOffset = (month.atDay(1).dayOfWeek.value - DayOfWeek.MONDAY.value + 7) % 7
  val cells = List(firstOffset) { null } + (1..month.lengthOfMonth()).map(month::atDay)
  val rowCount = (cells.size + 6) / 7
  AppCard(
    modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.lg),
    style = AppCardStyle.Tonal,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.md),
  ) {
    Row(
      Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically,
    ) {
      IconButton(onClick = onPrevious) { Icon(Icons.Rounded.ChevronLeft, "上个月") }
      Text("${month.year} 年 ${month.monthValue} 月", fontWeight = FontWeight.Bold)
      IconButton(onClick = onNext) { Icon(Icons.Rounded.ChevronRight, "下个月") }
    }
    Row(Modifier.fillMaxWidth()) {
      DayOfWeek.entries.forEach {
        Text(
          it.getDisplayName(TextStyle.NARROW, Locale.SIMPLIFIED_CHINESE),
          modifier = Modifier.weight(1f),
          textAlign = androidx.compose.ui.text.style.TextAlign.Center,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
      }
    }
    LazyVerticalGrid(
      columns = GridCells.Fixed(7),
      modifier = Modifier.fillMaxWidth().height(AppSize.calendarCell * rowCount),
      userScrollEnabled = false,
    ) {
      items(cells) { date ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .height(AppSize.calendarCell)
            .clickable(enabled = date != null) { date?.let(onSelect) },
          contentAlignment = Alignment.Center,
        ) {
          if (date != null) {
            val selectedDay = date == selected
            Text(
              date.dayOfMonth.toString(),
              modifier = Modifier
                .clip(CircleShape)
                .background(if (selectedDay) MaterialTheme.colorScheme.primary else Color.Transparent)
                .padding(AppSpacing.md),
              color = if (selectedDay) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
            )
          }
        }
      }
    }
  }
}

@Composable
private fun Timeline(
  blocks: List<ScheduleBlock>,
  conflicts: Set<Long>,
  onClick: (ScheduleBlock) -> Unit,
  onComplete: (ScheduleBlock) -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier,
    contentPadding = androidx.compose.foundation.layout.PaddingValues(
      start = AppSpacing.md,
      end = AppSpacing.md,
      bottom = AppSize.fab + AppSpacing.xl,
    ),
  ) {
    items((0..23).toList()) { hour ->
      val hourBlocks = blocks.filter { it.startMinute / 60 == hour }
      Row(
        Modifier.fillMaxWidth().padding(vertical = AppSpacing.xs),
        verticalAlignment = Alignment.Top,
      ) {
        Text(
          text = "%02d:00".format(hour),
          style = MaterialTheme.typography.labelMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(top = AppSpacing.md).size(width = AppSize.timelineHourLabel, height = AppSize.icon),
        )
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
          if (hourBlocks.isEmpty()) {
            Spacer(
              Modifier.fillMaxWidth().height(AppSize.touchTarget)
                .clip(MaterialTheme.shapes.medium)
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.25f)),
            )
          } else {
            hourBlocks.forEach { block ->
              AppCard(
                onClick = { onClick(block) },
                modifier = Modifier.fillMaxWidth(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.md),
              ) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.xs)) {
                  Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(block.title, fontWeight = FontWeight.Bold, modifier = Modifier.weight(1f))
                    if (block.taskOccurrenceId != null) {
                      TextButton(onClick = { onComplete(block) }) {
                        Text(if (block.taskStatus == com.example.lifeplanner.core.domain.model.OccurrenceStatus.COMPLETED) "已完成" else "完成")
                      }
                    }
                  }
                  Text("${formatMinute(block.startMinute)}–${formatMinute(block.endMinute)}")
                  if (block.note.isNotBlank()) Text(block.note, color = MaterialTheme.colorScheme.onSurfaceVariant)
                  if (block.id in conflicts) AppStatusBadge("时间冲突", AppStatusTone.Error)
                }
              }
            }
          }
        }
      }
    }
  }
}

private data class EditorTarget(
  val block: ScheduleBlock? = null,
  val occurrenceId: Long? = null,
)

@Composable
private fun ScheduleEditorDialog(
  target: EditorTarget,
  onDismiss: () -> Unit,
  onDelete: () -> Unit,
  onSave: (String, String, Int, Int) -> Unit,
) {
  var title by remember { mutableStateOf(target.block?.title ?: if (target.occurrenceId != null) "关联任务" else "") }
  var note by remember { mutableStateOf(target.block?.note.orEmpty()) }
  var start by remember { mutableStateOf(formatMinute(target.block?.startMinute ?: 9 * 60)) }
  var end by remember { mutableStateOf(formatMinute(target.block?.endMinute ?: 10 * 60)) }
  var error by remember { mutableStateOf<String?>(null) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (target.block == null) "新增日程" else "编辑日程") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        if (target.occurrenceId == null) {
          OutlinedTextField(title, { title = it }, label = { Text("标题") }, modifier = Modifier.fillMaxWidth())
          OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth())
        } else {
          Text("保存后会把任务安排到当前日期，并与 TODO 完成状态同步。")
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
          OutlinedTextField(start, { start = it }, label = { Text("开始 HH:mm") }, modifier = Modifier.weight(1f))
          OutlinedTextField(end, { end = it }, label = { Text("结束 HH:mm") }, modifier = Modifier.weight(1f))
        }
        error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        val startMinute = parseMinute(start)
        val endMinute = parseMinute(end)
        if (startMinute == null || endMinute == null || startMinute % 15 != 0 || endMinute % 15 != 0 || endMinute <= startMinute) {
          error = "请输入 15 分钟步长，且结束时间晚于开始时间"
        } else if (target.occurrenceId == null && title.isBlank()) {
          error = "请输入标题"
        } else {
          onSave(title, note, startMinute, endMinute)
        }
      }) { Text("保存") }
    },
    dismissButton = {
      Row {
        if (target.block != null) {
          IconButton(onClick = onDelete) { Icon(Icons.Rounded.Delete, "删除") }
        }
        TextButton(onClick = onDismiss) { Text("取消") }
      }
    },
  )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun QuickPlanRoute(
  epochDay: Long,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: QuickPlanViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(epochDay) { viewModel.load(epochDay) }
  LaunchedEffect(viewModel) {
    viewModel.effect.collect { if (it == QuickPlanEffect.Completed) onDone() }
  }
  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(title = "快速安排", onBack = onDone)
    },
  ) { padding ->
    if (state.isLoading) {
      AppLoadingState(Modifier.padding(padding))
      return@Scaffold
    }
    val card = state.currentCard
    val answer = state.draft.answers[card.type] ?: QuickPlanAnswer(card.type)
    Column(
      Modifier.fillMaxSize().padding(padding).padding(AppSpacing.lg),
      verticalArrangement = Arrangement.spacedBy(AppSpacing.lg),
    ) {
      val total = com.example.lifeplanner.core.domain.quickplan.QuickPlanCatalog.cards.size
      Text(
        text = "第 ${state.draft.currentIndex + 1} 步，共 $total 步",
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
      )
      LinearProgressIndicator(
        progress = { (state.draft.currentIndex + 1).toFloat() / total },
        modifier = Modifier.fillMaxWidth().height(AppSize.progress).clip(CircleShape),
      )
      Text(card.title, style = MaterialTheme.typography.headlineMedium)
      when (card.interaction) {
        QuickPlanInteraction.MULTI_TAG, QuickPlanInteraction.SINGLE_TAG -> {
          FlowRow(
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
          ) {
            card.options.forEach { label ->
              AppChoiceChip(
                text = label,
                selected = label in answer.selectedOptions,
                onClick = {
                  if (card.interaction == QuickPlanInteraction.SINGLE_TAG) viewModel.selectSingle(label)
                  else viewModel.toggleOption(label)
                },
              )
            }
          }
          card.activeFollowUp(answer.selectedOptions)?.let { followUp ->
            Text(followUp.title, style = MaterialTheme.typography.titleMedium)
            FlowRow(
              horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
              verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
            ) {
              followUp.options.forEach { label ->
                AppChoiceChip(label, label in answer.subSelections, { viewModel.toggleFollowUp(label) })
              }
            }
          }
        }
        QuickPlanInteraction.HOUR_TIME -> {
          var slider by remember(answer.hour) { mutableFloatStateOf((answer.hour ?: 20).toFloat()) }
          Text("${slider.toInt()}:00", style = MaterialTheme.typography.displaySmall)
          Slider(
            value = slider,
            onValueChange = { slider = it },
            onValueChangeFinished = { viewModel.setHour(slider.toInt()) },
            valueRange = 0f..23f,
            steps = 22,
          )
        }
        QuickPlanInteraction.NOTE -> {
          OutlinedTextField(
            value = answer.note,
            onValueChange = viewModel::setNote,
            label = { Text("还有什么安排？") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 5,
          )
        }
      }
      Spacer(Modifier.weight(1f))
      state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
      Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
        verticalAlignment = Alignment.CenterVertically,
      ) {
        if (state.draft.currentIndex > 0) {
          AppButton("上一步", viewModel::previous, variant = AppButtonVariant.Text)
        }
        AppButton("跳过", viewModel::skip, variant = AppButtonVariant.Text)
        AppButton(
          text = if (state.isLast) "生成时间轴" else "下一张",
          onClick = if (state.isLast) viewModel::complete else viewModel::next,
          modifier = Modifier.weight(1f),
        )
      }
    }
  }
}

private fun formatMinute(value: Int): String = "%02d:%02d".format(value / 60, value % 60)

private fun parseMinute(value: String): Int? {
  val parts = value.split(":")
  if (parts.size != 2) return null
  val hour = parts[0].toIntOrNull() ?: return null
  val minute = parts[1].toIntOrNull() ?: return null
  if (hour !in 0..24 || minute !in 0..59 || hour == 24 && minute != 0) return null
  return hour * 60 + minute
}
