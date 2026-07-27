package com.example.lifeplanner.feature.diary

import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.EditNote
import androidx.compose.material.icons.rounded.SentimentDissatisfied
import androidx.compose.material.icons.rounded.SentimentSatisfied
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeplanner.core.domain.model.DiaryEntry
import com.example.lifeplanner.core.domain.model.DiaryEntryType
import com.example.libui.components.AppButton
import com.example.libui.components.AppButtonVariant
import com.example.libui.components.AppCard
import com.example.libui.components.AppCardStyle
import com.example.libui.components.AppChoiceChip
import com.example.libui.components.AppDateNavigator
import com.example.libui.components.AppFab
import com.example.libui.components.AppLoadingState
import com.example.libui.components.AppSectionHeader
import com.example.libui.components.AppTopBar
import com.example.libui.theme.AppSize
import com.example.libui.theme.AppSpacing
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import org.koin.androidx.compose.koinViewModel

@Composable
fun DiaryRoute(
  onOpenEditor: (epochDay: Long, entryId: Long?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DiaryViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedEntry by remember { mutableStateOf<DiaryEntry?>(null) }
  LaunchedEffect(Unit) { viewModel.initialize() }
  DiaryErrorEffect(state.errorMessage, snackbarHostState, viewModel::clearError)

  Scaffold(
    modifier = modifier,
    topBar = { AppTopBar("日记") },
    snackbarHost = { SnackbarHost(snackbarHostState) },
    floatingActionButton = {
      AppFab(
        icon = Icons.Rounded.EditNote,
        contentDescription = "编辑日记",
        onClick = { onOpenEditor(state.selectedDate.toEpochDay(), null) },
      )
    },
  ) { padding ->
    if (state.isLoading) {
      AppLoadingState(Modifier.padding(padding))
    } else {
      DiaryTabContent(
        state = state,
        onDateChange = viewModel::selectDate,
        onEntryLongClick = { selectedEntry = it },
        modifier = Modifier.padding(padding),
      )
    }
  }

  selectedEntry?.let { entry ->
    DiaryEntryDialog(
      entry = entry,
      initiallyEditing = false,
      allowTypeChange = false,
      onDismiss = { selectedEntry = null },
      onSave = { content, type ->
        viewModel.updateEntry(entry.id, type, content)
        selectedEntry = null
      },
      onDelete = {
        viewModel.deleteEntry(entry.id)
        selectedEntry = null
      },
      onOpenFullEditor = {
        selectedEntry = null
        onOpenEditor(entry.date.toEpochDay(), entry.id)
      },
    )
  }
}

@Composable
fun DiaryEditorRoute(
  epochDay: Long,
  entryId: Long?,
  onBack: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DiaryViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val snackbarHostState = remember { SnackbarHostState() }
  var selectedEntry by remember { mutableStateOf<DiaryEntry?>(null) }
  var openedEntryId by rememberSaveable(entryId) { mutableStateOf<Long?>(null) }
  LaunchedEffect(epochDay) { viewModel.initialize(epochDay) }
  LaunchedEffect(entryId, state.isLoading, state.day.entries) {
    if (!state.isLoading && entryId != null && openedEntryId != entryId) {
      state.day.entries.firstOrNull { it.id == entryId }?.let {
        selectedEntry = it
        openedEntryId = entryId
      }
    }
  }
  DiaryErrorEffect(state.errorMessage, snackbarHostState, viewModel::clearError)

  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(
        title = state.selectedDate.editorTitle(),
        onBack = onBack,
      )
    },
    snackbarHost = { SnackbarHost(snackbarHostState) },
  ) { padding ->
    if (state.isLoading) {
      AppLoadingState(Modifier.padding(padding))
    } else {
      DiaryEditorContent(
        state = state,
        onDraftChange = viewModel::updateEntryDraft,
        onCreateEntry = viewModel::createEntry,
        onEntryLongClick = { selectedEntry = it },
        onDayTextChange = viewModel::updateDayText,
        onSaveDayText = viewModel::saveDayText,
        modifier = Modifier.padding(padding),
      )
    }
  }

  selectedEntry?.let { entry ->
    DiaryEntryDialog(
      entry = entry,
      initiallyEditing = true,
      allowTypeChange = true,
      onDismiss = { selectedEntry = null },
      onSave = { content, type ->
        viewModel.updateEntry(entry.id, type, content)
        selectedEntry = null
      },
      onDelete = {
        viewModel.deleteEntry(entry.id)
        selectedEntry = null
      },
    )
  }
}

@Composable
private fun DiaryTabContent(
  state: DiaryUiState,
  onDateChange: (LocalDate) -> Unit,
  onEntryLongClick: (DiaryEntry) -> Unit,
  modifier: Modifier = Modifier,
) {
  val happyEntries = state.day.entriesOf(DiaryEntryType.HAPPY)
  val unhappyEntries = state.day.entriesOf(DiaryEntryType.UNHAPPY)
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(AppSpacing.lg),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
  ) {
    item(key = "date-navigator") {
      AppDateNavigator(
        date = state.selectedDate,
        onDateChange = onDateChange,
      )
    }
    diaryEntriesSection(
      key = "happy",
      title = "感到开心的事",
      entries = happyEntries,
      emptyMessage = "还没有记录开心的事",
      onEntryLongClick = onEntryLongClick,
    )
    diaryEntriesSection(
      key = "unhappy",
      title = "感到不开心的事",
      entries = unhappyEntries,
      emptyMessage = "还没有记录不开心的事",
      onEntryLongClick = onEntryLongClick,
    )
    item(key = "day-text-header") {
      AppSectionHeader(
        title = "当日日记",
        modifier = Modifier.padding(top = AppSpacing.lg),
      )
    }
    item(key = "day-text") {
      if (state.day.text.isBlank()) {
        Text(
          text = "还没有完整日记，点击右下角开始记录。",
          style = MaterialTheme.typography.bodyMedium,
          color = MaterialTheme.colorScheme.onSurfaceVariant,
          modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
        )
      } else {
        AppCard(
          modifier = Modifier.fillMaxWidth(),
          style = AppCardStyle.Tonal,
        ) {
          Text(state.day.text, style = MaterialTheme.typography.bodyLarge)
        }
      }
    }
  }
}

@Composable
private fun DiaryEditorContent(
  state: DiaryUiState,
  onDraftChange: (DiaryEntryType, String) -> Unit,
  onCreateEntry: (DiaryEntryType) -> Unit,
  onEntryLongClick: (DiaryEntry) -> Unit,
  onDayTextChange: (String) -> Unit,
  onSaveDayText: () -> Unit,
  modifier: Modifier = Modifier,
) {
  LazyColumn(
    modifier = modifier.fillMaxSize(),
    contentPadding = PaddingValues(AppSpacing.lg),
    verticalArrangement = Arrangement.spacedBy(AppSpacing.sm),
  ) {
    diaryEditorSection(
      key = "happy",
      title = "感到开心的事",
      type = DiaryEntryType.HAPPY,
      entries = state.day.entriesOf(DiaryEntryType.HAPPY),
      draft = state.happyDraft,
      isSaving = state.savingEntryType == DiaryEntryType.HAPPY,
      onDraftChange = onDraftChange,
      onCreateEntry = onCreateEntry,
      onEntryLongClick = onEntryLongClick,
    )
    diaryEditorSection(
      key = "unhappy",
      title = "感到不开心的事",
      type = DiaryEntryType.UNHAPPY,
      entries = state.day.entriesOf(DiaryEntryType.UNHAPPY),
      draft = state.unhappyDraft,
      isSaving = state.savingEntryType == DiaryEntryType.UNHAPPY,
      onDraftChange = onDraftChange,
      onCreateEntry = onCreateEntry,
      onEntryLongClick = onEntryLongClick,
    )
    item(key = "day-text-header") {
      AppSectionHeader(
        title = if (state.selectedDate == LocalDate.now()) "今日日记" else "当日日记",
        modifier = Modifier.padding(top = AppSpacing.lg),
      )
    }
    item(key = "day-text-editor") {
      Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        OutlinedTextField(
          value = state.dayTextDraft,
          onValueChange = onDayTextChange,
          label = { Text("完整记录") },
          modifier = Modifier.fillMaxWidth(),
          minLines = 8,
        )
        AppButton(
          text = "保存日记",
          onClick = onSaveDayText,
          modifier = Modifier.fillMaxWidth(),
          loading = state.isSavingDayText,
        )
        if (state.day.text.isNotBlank()) {
          AppCard(
            modifier = Modifier.fillMaxWidth(),
            style = AppCardStyle.Tonal,
          ) {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
              Text("已保存", style = MaterialTheme.typography.labelLarge)
              Text(state.day.text, style = MaterialTheme.typography.bodyLarge)
            }
          }
        }
      }
    }
  }
}

private fun LazyListScope.diaryEntriesSection(
  key: String,
  title: String,
  entries: List<DiaryEntry>,
  emptyMessage: String,
  onEntryLongClick: (DiaryEntry) -> Unit,
) {
  item(key = "$key-header") {
    AppSectionHeader(
      title = title,
      count = entries.size,
      modifier = Modifier.padding(top = AppSpacing.lg),
    )
  }
  if (entries.isEmpty()) {
    item(key = "$key-empty") {
      Text(
        text = emptyMessage,
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
      )
    }
  } else {
    items(entries, key = { it.id }) { entry ->
      DiaryEntryRow(entry, onLongClick = { onEntryLongClick(entry) })
    }
  }
}

private fun LazyListScope.diaryEditorSection(
  key: String,
  title: String,
  type: DiaryEntryType,
  entries: List<DiaryEntry>,
  draft: String,
  isSaving: Boolean,
  onDraftChange: (DiaryEntryType, String) -> Unit,
  onCreateEntry: (DiaryEntryType) -> Unit,
  onEntryLongClick: (DiaryEntry) -> Unit,
) {
  item(key = "$key-header") {
    AppSectionHeader(
      title = title,
      count = entries.size,
      modifier = Modifier.padding(top = AppSpacing.lg),
    )
  }
  item(key = "$key-input") {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
      verticalAlignment = Alignment.CenterVertically,
    ) {
      OutlinedTextField(
        value = draft,
        onValueChange = { onDraftChange(type, it) },
        label = { Text("新增条目") },
        modifier = Modifier.weight(1f),
        singleLine = true,
      )
      AppButton(
        text = "添加",
        onClick = { onCreateEntry(type) },
        enabled = draft.isNotBlank(),
        loading = isSaving,
        leadingIcon = Icons.Rounded.Add,
      )
    }
  }
  items(entries, key = { it.id }) { entry ->
    DiaryEntryRow(entry, onLongClick = { onEntryLongClick(entry) })
  }
}

@Composable
private fun DiaryEntryRow(
  entry: DiaryEntry,
  onLongClick: () -> Unit,
) {
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .defaultMinSize(minHeight = AppSize.touchTarget)
      .combinedClickable(
        onClick = {},
        onLongClick = onLongClick,
        onLongClickLabel = "预览和编辑条目",
      )
      .padding(horizontal = AppSpacing.md, vertical = AppSpacing.sm),
    horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm),
    verticalAlignment = Alignment.CenterVertically,
  ) {
    Icon(
      imageVector = entry.type.icon(),
      contentDescription = entry.type.label(),
      tint = if (entry.type == DiaryEntryType.HAPPY) {
        MaterialTheme.colorScheme.primary
      } else {
        MaterialTheme.colorScheme.tertiary
      },
    )
    Text(
      text = entry.content,
      style = MaterialTheme.typography.bodyLarge,
      modifier = Modifier.weight(1f),
      maxLines = 3,
      overflow = TextOverflow.Ellipsis,
    )
  }
  HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
}

@Composable
private fun DiaryEntryDialog(
  entry: DiaryEntry,
  initiallyEditing: Boolean,
  allowTypeChange: Boolean,
  onDismiss: () -> Unit,
  onSave: (content: String, type: DiaryEntryType) -> Unit,
  onDelete: () -> Unit,
  onOpenFullEditor: (() -> Unit)? = null,
) {
  var editing by remember(entry.id, entry.updatedAt, initiallyEditing) {
    mutableStateOf(initiallyEditing)
  }
  var confirmingDelete by remember(entry.id) { mutableStateOf(false) }
  var content by remember(entry.id, entry.updatedAt) { mutableStateOf(entry.content) }
  var type by remember(entry.id, entry.updatedAt) { mutableStateOf(entry.type) }

  if (confirmingDelete) {
    AlertDialog(
      onDismissRequest = { confirmingDelete = false },
      title = { Text("删除这条记录？") },
      text = { Text("删除后无法恢复。") },
      confirmButton = {
        AppButton(
          text = "确认删除",
          onClick = onDelete,
          variant = AppButtonVariant.Danger,
        )
      },
      dismissButton = {
        TextButton(onClick = { confirmingDelete = false }) { Text("取消") }
      },
    )
    return
  }

  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text(if (editing) "编辑条目" else entry.type.sectionTitle()) },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.md)) {
        if (editing) {
          if (allowTypeChange) {
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
              DiaryEntryType.entries.forEach { option ->
                AppChoiceChip(
                  text = option.label(),
                  selected = option == type,
                  onClick = { type = option },
                )
              }
            }
          }
          OutlinedTextField(
            value = content,
            onValueChange = { content = it },
            label = { Text("条目内容") },
            modifier = Modifier.fillMaxWidth(),
            minLines = 3,
          )
        } else {
          Text(
            text = entry.date.format(DATE_FORMATTER),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
          )
          Text(entry.content, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
        ) {
          TextButton(onClick = { confirmingDelete = true }) {
            Text("删除", color = MaterialTheme.colorScheme.error)
          }
          if (!editing && onOpenFullEditor != null) {
            TextButton(onClick = onOpenFullEditor) { Text("完整编辑") }
          }
        }
      }
    },
    confirmButton = {
      if (editing) {
        TextButton(
          onClick = { onSave(content, type) },
          enabled = content.isNotBlank(),
        ) {
          Text("保存")
        }
      } else {
        TextButton(onClick = { editing = true }) { Text("简单修改") }
      }
    },
    dismissButton = {
      TextButton(
        onClick = if (editing && !initiallyEditing) {
          { editing = false }
        } else {
          onDismiss
        },
      ) {
        Text(if (editing && !initiallyEditing) "取消修改" else "关闭")
      }
    },
  )
}

@Composable
private fun DiaryErrorEffect(
  message: String?,
  snackbarHostState: SnackbarHostState,
  onShown: () -> Unit,
) {
  LaunchedEffect(message) {
    message?.let {
      snackbarHostState.showSnackbar(it)
      onShown()
    }
  }
}

private fun DiaryEntryType.label(): String = when (this) {
  DiaryEntryType.HAPPY -> "开心"
  DiaryEntryType.UNHAPPY -> "不开心"
}

private fun DiaryEntryType.sectionTitle(): String = when (this) {
  DiaryEntryType.HAPPY -> "感到开心的事"
  DiaryEntryType.UNHAPPY -> "感到不开心的事"
}

private fun DiaryEntryType.icon(): ImageVector = when (this) {
  DiaryEntryType.HAPPY -> Icons.Rounded.SentimentSatisfied
  DiaryEntryType.UNHAPPY -> Icons.Rounded.SentimentDissatisfied
}

private fun LocalDate.editorTitle(): String = if (this == LocalDate.now()) {
  "今日日记"
} else {
  "${format(DATE_FORMATTER)}日记"
}

private val DATE_FORMATTER: DateTimeFormatter = DateTimeFormatter.ofPattern("M月d日")
