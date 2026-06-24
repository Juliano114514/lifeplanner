package com.example.lifeplanner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foundation.util.PlanTextExporter
import com.example.libroom.repository.PlanRepository
import com.example.libui.feature.plan.PlanResultAction
import com.example.libui.feature.plan.PlanResultUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanResultViewModel(
  private val repository: PlanRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(PlanResultUiState())
  val state = _state.asStateFlow()

  private val _effect = MutableSharedFlow<PlanResultEffect>()
  val effect: SharedFlow<PlanResultEffect> = _effect.asSharedFlow()

  fun load(planId: Long) {
    viewModelScope.launch {
      val record = repository.getPlan(planId)
      if (record == null) {
        _state.update { it.copy(isLoading = false, summaryText = "未找到规划记录") }
        return@launch
      }
      val summary = record.exportText ?: PlanTextExporter.buildSummary(
        date = record.date,
        answers = record.answers,
      )
      _state.update {
        PlanResultUiState(
          date = record.date,
          summaryText = summary,
          isLoading = false,
        )
      }
    }
  }

  fun onAction(action: PlanResultAction) {
    when (action) {
      PlanResultAction.Copy -> {
        viewModelScope.launch {
          _effect.emit(PlanResultEffect.CopyToClipboard(_state.value.summaryText))
        }
      }
      PlanResultAction.Done -> {
        viewModelScope.launch {
          _effect.emit(PlanResultEffect.NavigateHome)
        }
      }
    }
  }
}

sealed interface PlanResultEffect {
  data class CopyToClipboard(val text: String) : PlanResultEffect
  data object NavigateHome : PlanResultEffect
}
