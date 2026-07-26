package com.example.lifeplanner.feature.todo

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.DailyPlanItem
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.RecurrenceRule
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskDraft
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.repository.TaskRepository
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import java.time.LocalDate
import java.time.LocalDateTime
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class TodoUiState(
  val isLoading: Boolean = true,
  val overview: TodoOverview = TodoOverview(),
  val daySchedule: DaySchedule = DaySchedule(LocalDate.now()),
  val errorMessage: String? = null,
) {
  val pendingScheduleBlocks: List<ScheduleBlock>
    get() = daySchedule.blocks.filter {
      it.completionStatus != OccurrenceStatus.COMPLETED
    }

  val completedItems: List<DailyPlanItem>
    get() {
      val completedOccurrenceIds = overview.todayCompleted
        .mapNotNull { it.occurrence?.id }
        .toSet()
      val completedSchedules = daySchedule.blocks.filter { block ->
        block.completionStatus == OccurrenceStatus.COMPLETED &&
          block.taskOccurrenceId !in completedOccurrenceIds
      }
      return (overview.todayCompleted + completedSchedules)
        .sortedWith(
          compareByDescending<DailyPlanItem> { it.completedAt ?: Long.MIN_VALUE }
            .thenBy(DailyPlanItem::title),
        )
    }
}

class TodoViewModel(
  private val repository: TaskRepository,
  private val scheduleRepository: ScheduleRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(TodoUiState())
  val state: StateFlow<TodoUiState> = _state.asStateFlow()

  init {
    val today = LocalDate.now()
    viewModelScope.launch {
      combine(
        repository.observeTodo(today),
        scheduleRepository.observeDay(today),
      ) { overview, schedule -> overview to schedule }
        .catch { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } }
        .collect { (overview, schedule) ->
          _state.value = TodoUiState(
            isLoading = false,
            overview = overview,
            daySchedule = schedule,
          )
        }
    }
  }

  fun setPinned(taskId: Long, pinned: Boolean) {
    viewModelScope.launch { repository.setPinned(taskId, pinned) }
  }

  fun setStatus(occurrenceId: Long, status: OccurrenceStatus) {
    viewModelScope.launch { repository.setOccurrenceStatus(occurrenceId, status) }
  }

  fun setScheduleStatus(blockId: Long, status: OccurrenceStatus) {
    viewModelScope.launch { scheduleRepository.setBlockStatus(blockId, status) }
  }

  fun archive(taskId: Long) {
    viewModelScope.launch { repository.archiveTask(taskId) }
  }

  fun updateScheduleBlock(
    block: ScheduleBlock,
    title: String,
    note: String,
    startMinute: Int,
    endMinute: Int,
  ) {
    viewModelScope.launch {
      scheduleRepository.saveBlock(
        ScheduleBlockDraft(
          id = block.id,
          date = block.date,
          startMinute = startMinute,
          endMinute = endMinute,
          title = title,
          note = note,
          taskOccurrenceId = block.taskOccurrenceId,
        ),
      )
    }
  }

  fun archiveScheduleBlock(id: Long) {
    viewModelScope.launch { scheduleRepository.archiveBlock(id) }
  }
}

data class TaskEditorUiState(
  val isLoading: Boolean = false,
  val task: Task? = null,
  val errorMessage: String? = null,
)

sealed interface TaskEditorEffect {
  data object Saved : TaskEditorEffect
}

class TaskEditorViewModel(
  private val repository: TaskRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(TaskEditorUiState())
  val state: StateFlow<TaskEditorUiState> = _state.asStateFlow()

  private val _effect = MutableSharedFlow<TaskEditorEffect>()
  val effect: SharedFlow<TaskEditorEffect> = _effect.asSharedFlow()

  private var loadedId: Long? = null

  fun startCreating() {
    loadedId = null
    _state.value = TaskEditorUiState()
  }

  fun load(taskId: Long?) {
    if (taskId == null || taskId == loadedId) return
    loadedId = taskId
    _state.value = TaskEditorUiState(isLoading = true)
    viewModelScope.launch {
      runCatching {
        requireNotNull(repository.getTask(taskId)) { "任务不存在" }
      }.onSuccess { task ->
        if (loadedId == taskId) {
          _state.value = TaskEditorUiState(task = task)
        }
      }.onFailure { error ->
        if (loadedId == taskId) {
          loadedId = null
          _state.value = TaskEditorUiState(errorMessage = error.message)
        }
      }
    }
  }

  fun save(
    title: String,
    note: String,
    dueAt: LocalDateTime?,
    pinned: Boolean,
    frequency: RecurrenceFrequency?,
    recurrenceStart: LocalDate,
  ) {
    viewModelScope.launch {
      _state.update { it.copy(isLoading = true, errorMessage = null) }
      runCatching {
        repository.saveTask(
          TaskDraft(
            id = loadedId,
            title = title,
            note = note,
            dueAt = dueAt,
            isPinned = pinned,
            recurrence = frequency?.let(::RecurrenceRule),
            recurrenceStart = recurrenceStart,
          ),
        )
      }.onSuccess {
        loadedId = null
        _state.update { it.copy(isLoading = false) }
        _effect.emit(TaskEditorEffect.Saved)
      }.onFailure { error ->
        _state.update { it.copy(isLoading = false, errorMessage = error.message) }
      }
    }
  }
}
