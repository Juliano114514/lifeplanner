package com.example.lifeplanner.feature.todo

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Checklist
import androidx.compose.material.icons.rounded.Event
import androidx.compose.material.icons.rounded.PushPin
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.libui.components.AppButton
import com.example.libui.components.AppCard
import com.example.libui.components.AppChoiceChip
import com.example.libui.components.AppDatePickerField
import com.example.libui.components.AppEmptyState
import com.example.libui.components.AppErrorState
import com.example.libui.components.AppFab
import com.example.libui.components.AppLoadingState
import com.example.libui.components.AppScheduleEditorDialog
import com.example.libui.components.AppSectionHeader
import com.example.libui.components.AppStatusBadge
import com.example.libui.components.AppStatusTone
import com.example.libui.components.AppTimePickerField
import com.example.libui.components.AppTopBar
import com.example.libui.theme.AppSpacing
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodoRoute(
  onAddTask: () -> Unit,
  onEditTask: (Long) -> Unit,
  onScheduleTask: (Long) -> Unit,
  onOpenSchedule: (Long) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TodoViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var editingScheduleBlock by remember { mutableStateOf<ScheduleBlock?>(null) }
  Scaffold(
    modifier = modifier,
    topBar = { AppTopBar("任务计划") },
    floatingActionButton = { AppFab(Icons.Rounded.Add, "新增任务", onAddTask) },
  ) { padding ->
    when {
      state.isLoading -> AppLoadingState(Modifier.padding(padding))
      state.errorMessage != null -> AppErrorState(
        message = state.errorMessage.orEmpty(),
        modifier = Modifier.padding(padding),
      )
      else -> TodoContent(
        state = state,
        onEditTask = onEditTask,
        onScheduleTask = onScheduleTask,
        onOpenSchedule = onOpenSchedule,
        onEditSchedule = { editingScheduleBlock = it },
        onToggleComplete = { item ->
          item.occurrence?.let {
            val next = if (it.status == OccurrenceStatus.COMPLETED) {
              OccurrenceStatus.PENDING
            } else {
              OccurrenceStatus.COMPLETED
            }
            viewModel.setStatus(it.id, next)
          }
        },
        onTogglePin = { item -> viewModel.setPinned(item.task.id, !item.task.isPinned) },
        onSkip = { item ->
          item.occurrence?.let { viewModel.setStatus(it.id, OccurrenceStatus.SKIPPED) }
        },
        onArchive = { viewModel.archive(it.task.id) },
        modifier = Modifier.padding(padding),
      )
    }
  }

  editingScheduleBlock?.let { block ->
    AppScheduleEditorDialog(
      dialogTitle = "编辑日程",
      initialTitle = block.title,
      initialNote = block.note,
      initialStartMinute = block.startMinute,
      initialEndMinute = block.endMinute,
      detailsEditable = block.taskOccurrenceId == null,
      lockedDetailsMessage = "任务标题和备注请在任务中修改，此处可调整安排时间。",
      onDismiss = { editingScheduleBlock = null },
      onSave = { title, note, start, end ->
        viewModel.updateScheduleBlock(block, title, note, start, end)
        editingScheduleBlock = null
      },
      onDelete = {
        viewModel.archiveScheduleBlock(block.id)
        editingScheduleBlock = null
      },
    )
  }
}

@Composable
private fun TodoContent(
  state: TodoUiState,
  onEditTask: (Long) -> Unit,
  onScheduleTask: (Long) -> Unit,
  onOpenSchedule: (Long) -> Unit,
  onEditSchedule: (ScheduleBlock) -> Unit,
  onToggleComplete: (TodoItem) -> Unit,
  onTogglePin: (TodoItem) -> Unit,
  onSkip: (TodoItem) -> Unit,
  onArchive: (TodoItem) -> Unit,
  modifier: Modifier = Modifier,
) {
  val overview = state.overview
  val empty = overview.urgent.isEmpty() && overview.todayPending.isEmpty() &&
    overview.todayCompleted.isEmpty() && overview.others.isEmpty() &&
    state.daySchedule.blocks.isEmpty()
  if (empty) {
    AppEmptyState(
      title = "今天还没有任务",
      message = "点右下角的 + 添加第一项任务",
      icon = Icons.Rounded.Checklist,
      modifier = modifier,
    )
    return
  }

  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
  ) {
    todoSection("置顶 / 临近 DDL", overview.urgent, true, onEditTask, onScheduleTask, onToggleComplete, onTogglePin, onSkip, onArchive)
    scheduleSection(state.daySchedule.blocks, onOpenSchedule, onEditSchedule)
    todoSection("今日未完成", overview.todayPending, false, onEditTask, onScheduleTask, onToggleComplete, onTogglePin, onSkip, onArchive)
    todoSection("今日已完成", overview.todayCompleted, false, onEditTask, onScheduleTask, onToggleComplete, onTogglePin, onSkip, onArchive)
    todoSection("其他任务", overview.others, false, onEditTask, onScheduleTask, onToggleComplete, onTogglePin, onSkip, onArchive)
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.scheduleSection(
  blocks: List<ScheduleBlock>,
  onOpenSchedule: (Long) -> Unit,
  onEditSchedule: (ScheduleBlock) -> Unit,
) {
  if (blocks.isEmpty()) return
  item(key = "header-today-schedule") {
    AppSectionHeader(
      title = "今日日程",
      count = blocks.size,
      modifier = Modifier.padding(top = AppSpacing.sm),
    )
  }
  items(blocks, key = { "schedule-${it.id}" }) { block ->
    TodayScheduleCard(block, onOpenSchedule, onEditSchedule)
  }
}

@Composable
private fun TodayScheduleCard(
  block: ScheduleBlock,
  onOpenSchedule: (Long) -> Unit,
  onEditSchedule: (ScheduleBlock) -> Unit,
) {
  AppCard(
    onClick = { onOpenSchedule(block.date.toEpochDay()) },
    onLongClick = { onEditSchedule(block) },
    modifier = Modifier
      .fillMaxWidth()
      .testTag("today-schedule-${block.id}"),
  ) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(block.title, style = MaterialTheme.typography.titleMedium)
          Text(
            text = "${formatScheduleMinute(block.startMinute)}–${formatScheduleMinute(block.endMinute)}",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
        }
        AppStatusBadge(
          text = when {
            block.taskOccurrenceId != null -> "任务"
            block.source == ScheduleSource.QUICK_PLAN -> "快速安排"
            else -> "手动"
          },
          tone = AppStatusTone.Neutral,
        )
      }
      if (block.note.isNotBlank()) {
        Text(block.note, style = MaterialTheme.typography.bodyMedium)
      }
      if (block.taskStatus == OccurrenceStatus.COMPLETED) {
        AppStatusBadge("已完成", AppStatusTone.Success)
      }
    }
  }
}

private fun androidx.compose.foundation.lazy.LazyListScope.todoSection(
  title: String,
  values: List<TodoItem>,
  urgent: Boolean,
  onEditTask: (Long) -> Unit,
  onScheduleTask: (Long) -> Unit,
  onToggleComplete: (TodoItem) -> Unit,
  onTogglePin: (TodoItem) -> Unit,
  onSkip: (TodoItem) -> Unit,
  onArchive: (TodoItem) -> Unit,
) {
  if (values.isEmpty()) return
  item(key = "header-$title") {
    AppSectionHeader(
      title = title,
      count = values.size,
      modifier = Modifier.padding(top = AppSpacing.sm),
    )
  }
  items(values, key = { "${it.task.id}:${it.occurrence?.id ?: 0}" }) { item ->
    TodoCard(item, urgent, onEditTask, onScheduleTask, onToggleComplete, onTogglePin, onSkip, onArchive)
  }
}

private fun formatScheduleMinute(value: Int): String =
  "%02d:%02d".format(value / 60, value % 60)

@Composable
private fun TodoCard(
  item: TodoItem,
  urgent: Boolean,
  onEditTask: (Long) -> Unit,
  onScheduleTask: (Long) -> Unit,
  onToggleComplete: (TodoItem) -> Unit,
  onTogglePin: (TodoItem) -> Unit,
  onSkip: (TodoItem) -> Unit,
  onArchive: (TodoItem) -> Unit,
) {
  val overdue = (item.occurrence?.dueAt ?: item.task.dueAt)?.isBefore(LocalDateTime.now()) == true &&
    item.occurrence?.status == OccurrenceStatus.PENDING
  AppCard(onClick = { onEditTask(item.task.id) }, modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Checkbox(
          checked = item.occurrence?.status == OccurrenceStatus.COMPLETED,
          onCheckedChange = { onToggleComplete(item) },
          enabled = item.occurrence != null,
        )
        Column(Modifier.weight(1f)) {
          Text(item.task.title, style = MaterialTheme.typography.titleMedium)
          (item.occurrence?.dueAt ?: item.task.dueAt)?.let {
            Text(
              "DDL ${it.format(DateTimeFormatter.ofPattern("MM-dd HH:mm"))}",
              color = if (overdue) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            )
          }
          if (urgent && overdue) AppStatusBadge("已逾期", AppStatusTone.Error)
        }
        IconButton(onClick = { onTogglePin(item) }) {
          Icon(
            Icons.Rounded.PushPin,
            contentDescription = if (item.task.isPinned) "取消置顶" else "置顶",
            tint = if (item.task.isPinned) MaterialTheme.colorScheme.primary else Color.Gray,
          )
        }
      }
      Row(horizontalArrangement = Arrangement.End, modifier = Modifier.fillMaxWidth()) {
        item.occurrence?.let { occurrence ->
          TextButton(onClick = { onScheduleTask(occurrence.id) }) {
            Icon(Icons.Rounded.Event, null)
            Text("安排")
          }
          if (occurrence.status == OccurrenceStatus.PENDING) {
            TextButton(onClick = { onSkip(item) }) { Text("跳过") }
          }
        }
        TextButton(onClick = { onArchive(item) }) {
          Icon(Icons.Rounded.Archive, null)
          Text("归档")
        }
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskEditorRoute(
  taskId: Long?,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: TaskEditorViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(taskId) { viewModel.load(taskId) }
  LaunchedEffect(viewModel) {
    viewModel.effect.collect { if (it == TaskEditorEffect.Saved) onDone() }
  }

  val task = state.task
  var title by remember(task?.id) { mutableStateOf(task?.title.orEmpty()) }
  var note by remember(task?.id) { mutableStateOf(task?.note.orEmpty()) }
  var dueDate by remember(task?.id) { mutableStateOf(task?.dueAt?.toLocalDate()) }
  var dueTime by remember(task?.id) {
    mutableStateOf(task?.dueAt?.toLocalTime()?.let { it.hour * 60 + it.minute })
  }
  var pinned by remember(task?.id) { mutableStateOf(task?.isPinned ?: false) }
  var recurrence by remember(task?.id) { mutableStateOf(task?.recurrence?.frequency) }

  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(
        title = if (taskId == null) "新增任务" else "编辑任务",
        onBack = onDone,
      )
    },
  ) { padding ->
    Column(
      Modifier
        .padding(padding)
        .verticalScroll(rememberScrollState())
        .padding(horizontal = AppSpacing.lg, vertical = AppSpacing.md),
      verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
      OutlinedTextField(title, { title = it }, label = { Text("任务标题") }, modifier = Modifier.fillMaxWidth())
      OutlinedTextField(note, { note = it }, label = { Text("备注") }, modifier = Modifier.fillMaxWidth(), minLines = 3)
      Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        AppDatePickerField(
          value = dueDate,
          onValueChange = { dueDate = it },
          label = "DDL 日期",
          modifier = Modifier.weight(1f),
          optional = true,
        )
        AppTimePickerField(
          valueMinutes = dueTime,
          onValueChange = { dueTime = it },
          label = "时间",
          modifier = Modifier.weight(1f),
          optional = true,
        )
      }
      Row(verticalAlignment = Alignment.CenterVertically) {
        Text("置顶", modifier = Modifier.weight(1f))
        Switch(pinned, { pinned = it })
      }
      Text("重复")
      Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        listOf(
          null to "不重复",
          RecurrenceFrequency.DAILY to "每天",
          RecurrenceFrequency.WEEKLY to "每周",
          RecurrenceFrequency.MONTHLY to "每月",
        ).forEach { (value, label) ->
          AppChoiceChip(label, recurrence == value, { recurrence = value }, modifier = Modifier.weight(1f))
        }
      }
      state.errorMessage?.let {
        Text(it, color = MaterialTheme.colorScheme.error)
      }
      Spacer(Modifier.height(AppSpacing.sm))
      AppButton(
        text = "保存任务",
        onClick = {
          viewModel.save(
            title = title,
            note = note,
            dueAt = dueDate?.let {
              val time = dueTime?.let { minute -> LocalTime.of(minute / 60, minute % 60) }
              LocalDateTime.of(it, time ?: LocalTime.of(23, 59))
            },
            pinned = pinned,
            frequency = recurrence,
            recurrenceStart = dueDate ?: task?.recurrenceStart ?: LocalDate.now(),
          )
        },
        modifier = Modifier.fillMaxWidth(),
      )
      HorizontalDivider()
      Text("重复任务的修改只影响未来待处理实例，历史完成记录会保留。", style = MaterialTheme.typography.bodySmall)
    }
  }
}
