package com.example.lifeplanner.feature.schedule

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanCardDefinition
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.quickplan.QuickPlanCatalog
import com.example.lifeplanner.core.domain.quickplan.QuickPlanReducer
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.TaskRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import com.example.lifeplanner.core.domain.rules.StockRules
import java.time.LocalDate
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ScheduleUiState(
  val selectedDate: LocalDate = LocalDate.now(),
  val daySchedule: DaySchedule = DaySchedule(LocalDate.now()),
  val schedulable: List<TodoItem> = emptyList(),
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
)

@OptIn(ExperimentalCoroutinesApi::class)
class ScheduleViewModel(
  private val scheduleRepository: ScheduleRepository,
  private val taskRepository: TaskRepository,
) : ViewModel() {
  private val selectedDate = MutableStateFlow(LocalDate.now())
  private val _state = MutableStateFlow(ScheduleUiState())
  val state: StateFlow<ScheduleUiState> = _state.asStateFlow()
  private var initialized = false

  init {
    viewModelScope.launch {
      selectedDate.flatMapLatest { date ->
        combine(
          scheduleRepository.observeDay(date),
          taskRepository.observeSchedulable(date),
        ) { schedule, tasks -> Triple(date, schedule, tasks) }
      }.collect { (date, schedule, tasks) ->
        _state.update {
          it.copy(
            selectedDate = date,
            daySchedule = schedule,
            schedulable = tasks,
            isLoading = false,
          )
        }
      }
    }
  }

  fun initialize(epochDay: Long?) {
    if (initialized) return
    initialized = true
    epochDay?.let { selectDate(LocalDate.ofEpochDay(it)) }
  }

  fun selectDate(date: LocalDate) {
    selectedDate.value = date
  }

  fun saveBlock(
    id: Long?,
    title: String,
    note: String,
    startMinute: Int,
    endMinute: Int,
    occurrenceId: Long? = null,
  ) {
    viewModelScope.launch {
      runCatching {
        if (occurrenceId != null && id == null) {
          scheduleRepository.scheduleTask(
            occurrenceId,
            selectedDate.value,
            startMinute,
            endMinute,
          )
        } else {
          scheduleRepository.saveBlock(
            ScheduleBlockDraft(
              id = id,
              date = selectedDate.value,
              startMinute = startMinute,
              endMinute = endMinute,
              title = title,
              note = note,
              taskOccurrenceId = occurrenceId,
            ),
          )
        }
      }.onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
    }
  }

  fun archiveBlock(id: Long) {
    viewModelScope.launch { scheduleRepository.archiveBlock(id) }
  }

  fun completeTask(block: ScheduleBlock) {
    val occurrenceId = block.taskOccurrenceId ?: return
    val next = if (block.taskStatus == OccurrenceStatus.COMPLETED) {
      OccurrenceStatus.PENDING
    } else {
      OccurrenceStatus.COMPLETED
    }
    viewModelScope.launch { taskRepository.setOccurrenceStatus(occurrenceId, next) }
  }
}

data class QuickPlanUiState(
  val isLoading: Boolean = true,
  val draft: QuickPlanDraft = QuickPlanReducer.newDraft(LocalDate.now()),
  val availableFoods: List<StockItemDetails> = emptyList(),
  val errorMessage: String? = null,
) {
  val currentCard: QuickPlanCardDefinition
    get() = QuickPlanCatalog.cards[draft.currentIndex]
  val isLast: Boolean
    get() = draft.currentIndex == QuickPlanCatalog.cards.lastIndex
}

sealed interface QuickPlanEffect {
  data object Completed : QuickPlanEffect
}

class QuickPlanViewModel(
  private val repository: ScheduleRepository,
  private val stockRepository: StockRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(QuickPlanUiState())
  val state: StateFlow<QuickPlanUiState> = _state.asStateFlow()
  private val _effect = MutableSharedFlow<QuickPlanEffect>()
  val effect: SharedFlow<QuickPlanEffect> = _effect.asSharedFlow()
  private var baselineDraft: QuickPlanDraft? = null
  private var loadJob: Job? = null
  private var latestFoods: List<StockItemDetails> = emptyList()

  init {
    viewModelScope.launch {
      stockRepository.observeStock(StockKind.FOOD)
        .catch { error ->
          _state.update { it.copy(errorMessage = error.message) }
        }
        .collect { foods ->
          latestFoods = foods
          _state.update {
            it.copy(availableFoods = StockRules.availableFoods(foods, it.draft.date))
          }
        }
    }
  }

  fun load(epochDay: Long) {
    val date = LocalDate.ofEpochDay(epochDay)
    loadJob?.cancel()
    baselineDraft = null
    _state.update { it.copy(isLoading = true, errorMessage = null) }
    loadJob = viewModelScope.launch {
      try {
        val restored = repository.startQuickPlan(date)
        val draft = restored.copy(currentIndex = 0, completedAt = null)
        baselineDraft = draft
        _state.value = QuickPlanUiState(
          isLoading = false,
          draft = draft,
          availableFoods = StockRules.availableFoods(latestFoods, date),
        )
      } catch (error: CancellationException) {
        throw error
      } catch (error: Throwable) {
        _state.update { it.copy(isLoading = false, errorMessage = error.message) }
      }
    }
  }

  fun toggleOption(label: String) = reduce { QuickPlanReducer.toggleOption(it, label) }
  fun selectSingle(label: String) = reduce { QuickPlanReducer.selectSingle(it, label) }
  fun toggleFollowUp(label: String) = reduce { QuickPlanReducer.toggleFollowUp(it, label) }
  fun setHour(hour: Int) = reduce { QuickPlanReducer.setHour(it, hour) }
  fun setNote(note: String) = reduce { QuickPlanReducer.setNote(it, note) }
  fun setPeriodIncluded(period: DayPeriod, isIncluded: Boolean) =
    reduce { QuickPlanReducer.setPeriodIncluded(it, period, isIncluded) }
  fun setPeriodTag(period: DayPeriod, tag: String) =
    reduce { QuickPlanReducer.setPeriodTag(it, period, tag) }
  fun setPeriodText(period: DayPeriod, text: String) =
    reduce { QuickPlanReducer.setPeriodText(it, period, text) }
  fun setPeriodLocation(period: DayPeriod, location: String) =
    reduce { QuickPlanReducer.setPeriodLocation(it, period, location) }
  fun clearPeriod(period: DayPeriod) = reduce { QuickPlanReducer.clearPeriod(it, period) }
  fun previous() = reduce { QuickPlanReducer.previous(it) }
  fun skip() = reduce { QuickPlanReducer.skip(it) }
  fun next() = reduce { QuickPlanReducer.next(it) }

  fun complete() {
    val baseline = baselineDraft ?: return
    viewModelScope.launch {
      runCatching { repository.applyQuickPlan(_state.value.draft, baseline) }
        .onSuccess { _effect.emit(QuickPlanEffect.Completed) }
        .onFailure { error -> _state.update { it.copy(errorMessage = error.message) } }
    }
  }

  private fun reduce(block: (QuickPlanDraft) -> QuickPlanDraft) {
    val draft = block(_state.value.draft)
    _state.update { it.copy(draft = draft) }
  }
}
