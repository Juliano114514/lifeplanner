package com.example.lifeplanner.feature.diary

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeplanner.core.domain.model.DiaryDay
import com.example.lifeplanner.core.domain.model.DiaryDayDraft
import com.example.lifeplanner.core.domain.model.DiaryEntryDraft
import com.example.lifeplanner.core.domain.model.DiaryEntryType
import com.example.lifeplanner.core.domain.repository.DiaryRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class DiaryUiState(
  val selectedDate: LocalDate = LocalDate.now(),
  val day: DiaryDay = DiaryDay(LocalDate.now()),
  val happyDraft: String = "",
  val unhappyDraft: String = "",
  val dayTextDraft: String = "",
  val recordedDates: Set<LocalDate> = emptySet(),
  val savingEntryType: DiaryEntryType? = null,
  val isSaving: Boolean = false,
  val isLoading: Boolean = true,
  val errorMessage: String? = null,
) {
  fun draftFor(type: DiaryEntryType): String = when (type) {
    DiaryEntryType.HAPPY -> happyDraft
    DiaryEntryType.UNHAPPY -> unhappyDraft
  }
}

sealed interface DiaryEffect {
  data object Saved : DiaryEffect
}

@OptIn(ExperimentalCoroutinesApi::class)
class DiaryViewModel(
  private val repository: DiaryRepository,
) : ViewModel() {
  private val selectedDate = MutableStateFlow(LocalDate.now())
  private val _state = MutableStateFlow(DiaryUiState())
  val state: StateFlow<DiaryUiState> = _state.asStateFlow()
  private val _effect = MutableSharedFlow<DiaryEffect>()
  val effect: SharedFlow<DiaryEffect> = _effect.asSharedFlow()
  private var initialized = false
  private var dayTextDirty = false

  init {
    viewModelScope.launch {
      repository.observeRecordedDates()
        .catch { error ->
          _state.update { it.copy(errorMessage = error.message ?: "日记日期加载失败") }
        }
        .collect { dates -> _state.update { it.copy(recordedDates = dates) } }
    }
    viewModelScope.launch {
      selectedDate
        .flatMapLatest { date ->
          repository.observeDay(date)
            .catch { error ->
              _state.update {
                it.copy(
                  selectedDate = date,
                  isLoading = false,
                  errorMessage = error.message ?: "日记加载失败",
                )
              }
            }
        }
        .collect { day ->
          _state.update { current ->
            val dateChanged = current.selectedDate != day.date
            current.copy(
              selectedDate = day.date,
              day = day,
              dayTextDraft = if (dateChanged || !dayTextDirty) day.text else current.dayTextDraft,
              isLoading = false,
            )
          }
        }
    }
  }

  fun initialize(epochDay: Long? = null) {
    if (initialized) return
    initialized = true
    epochDay?.let { selectDate(LocalDate.ofEpochDay(it)) }
  }

  fun selectDate(date: LocalDate) {
    if (selectedDate.value == date && !state.value.isLoading) return
    dayTextDirty = false
    _state.update {
      it.copy(
        selectedDate = date,
        happyDraft = "",
        unhappyDraft = "",
        dayTextDraft = "",
        isLoading = true,
        errorMessage = null,
      )
    }
    selectedDate.value = date
  }

  fun updateEntryDraft(type: DiaryEntryType, value: String) {
    _state.update {
      when (type) {
        DiaryEntryType.HAPPY -> it.copy(happyDraft = value)
        DiaryEntryType.UNHAPPY -> it.copy(unhappyDraft = value)
      }
    }
  }

  fun createEntry(type: DiaryEntryType) {
    val current = state.value
    if (current.isSaving || current.savingEntryType != null) return
    val content = current.draftFor(type).trim()
    if (content.isEmpty()) {
      _state.update { it.copy(errorMessage = "请输入条目内容") }
      return
    }
    saveEntry(
      draft = DiaryEntryDraft(
        date = selectedDate.value,
        type = type,
        content = content,
      ),
      clearDraft = true,
    )
  }

  fun updateEntry(id: Long, type: DiaryEntryType, content: String) {
    saveEntry(
      DiaryEntryDraft(
        id = id,
        date = selectedDate.value,
        type = type,
        content = content,
      ),
    )
  }

  fun deleteEntry(id: Long) {
    viewModelScope.launch {
      runCatching { repository.deleteEntry(id) }
        .onFailure { error ->
          _state.update { it.copy(errorMessage = error.message ?: "删除失败") }
        }
    }
  }

  fun updateDayText(value: String) {
    dayTextDirty = true
    _state.update { it.copy(dayTextDraft = value) }
  }

  fun saveAll() {
    val current = state.value
    if (current.isSaving || current.savingEntryType != null) return
    val entries = buildList {
      current.happyDraft.takeIf(String::isNotBlank)?.let { content ->
        add(DiaryEntryDraft(date = current.selectedDate, type = DiaryEntryType.HAPPY, content = content))
      }
      current.unhappyDraft.takeIf(String::isNotBlank)?.let { content ->
        add(
          DiaryEntryDraft(
            date = current.selectedDate,
            type = DiaryEntryType.UNHAPPY,
            content = content,
          ),
        )
      }
    }
    viewModelScope.launch {
      _state.update { it.copy(isSaving = true, errorMessage = null) }
      runCatching {
        repository.saveDay(
          DiaryDayDraft(
            date = current.selectedDate,
            entries = entries,
            text = current.dayTextDraft,
          ),
        )
      }
        .onSuccess {
          dayTextDirty = false
          _state.update {
            it.copy(
              happyDraft = "",
              unhappyDraft = "",
              isSaving = false,
            )
          }
          _effect.emit(DiaryEffect.Saved)
        }
        .onFailure { error ->
          _state.update {
            it.copy(
              isSaving = false,
              errorMessage = error.message ?: "日记保存失败",
            )
          }
        }
    }
  }

  fun clearError() {
    _state.update { it.copy(errorMessage = null) }
  }

  private fun saveEntry(
    draft: DiaryEntryDraft,
    clearDraft: Boolean = false,
  ) {
    if (draft.content.isBlank()) {
      _state.update { it.copy(errorMessage = "条目内容不能为空") }
      return
    }
    viewModelScope.launch {
      _state.update { it.copy(savingEntryType = draft.type, errorMessage = null) }
      runCatching { repository.saveEntry(draft) }
        .onSuccess {
          _state.update { current ->
            val cleared = if (clearDraft) {
              when (draft.type) {
                DiaryEntryType.HAPPY -> current.copy(happyDraft = "")
                DiaryEntryType.UNHAPPY -> current.copy(unhappyDraft = "")
              }
            } else {
              current
            }
            cleared.copy(savingEntryType = null)
          }
        }
        .onFailure { error ->
          _state.update {
            it.copy(
              savingEntryType = null,
              errorMessage = error.message ?: "条目保存失败",
            )
          }
        }
    }
  }
}
