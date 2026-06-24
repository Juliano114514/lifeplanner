package com.example.lifeplanner.ui.plan

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foundation.domain.plan.PlanCardCatalog
import com.example.foundation.domain.plan.PlanFlowReducer
import com.example.foundation.domain.plan.PlanFlowState
import com.example.foundation.util.PlanTextExporter
import com.example.libroom.repository.PlanRepository
import com.example.libui.feature.plan.PlanAction
import com.example.libui.feature.plan.PlanUiState
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class PlanViewModel(
  private val repository: PlanRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(PlanUiState())
  val state = _state.asStateFlow()

  private val _effect = MutableSharedFlow<PlanEffect>()
  val effect: SharedFlow<PlanEffect> = _effect.asSharedFlow()

  private var planId: Long = 0
  private var flowState = PlanFlowState()
  private var initialized = false

  /** 换卡节流：抑制快速连点导致的动画叠帧与越界。 */
  private var lastNavAt = 0L

  fun initialize(planIdArg: Long?) {
    if (initialized) return
    initialized = true
    viewModelScope.launch {
      val existing = when {
        planIdArg != null -> repository.getPlan(planIdArg)
        else -> repository.observeIncompleteForToday().first()
      }

      if (existing != null && existing.completedAt == null) {
        planId = existing.id
        flowState = PlanFlowReducer.restore(existing.currentIndex, existing.answers)
      } else {
        planId = repository.createTodayPlan()
        flowState = PlanFlowState()
      }
      publishFlowState(isLoading = false)
    }
  }

  fun onAction(action: PlanAction) {
    viewModelScope.launch {
      when (action) {
        is PlanAction.ToggleTag -> editAnswer { PlanFlowReducer.toggleTag(it, action.label) }
        is PlanAction.SelectSingle -> editAnswer { PlanFlowReducer.selectSingle(it, action.label) }
        is PlanAction.ToggleFollowUp -> editAnswer { PlanFlowReducer.toggleFollowUp(it, action.label) }
        is PlanAction.SetHour -> editAnswer { PlanFlowReducer.setHour(it, action.hour) }
        is PlanAction.SetNote -> editAnswer { PlanFlowReducer.setNote(it, action.text) }
        PlanAction.AddExtraNote -> editAnswer { PlanFlowReducer.addExtraNote(it) }
        is PlanAction.UpdateExtraNote ->
          editAnswer { PlanFlowReducer.updateExtraNote(it, action.index, action.text) }
        PlanAction.Next -> navigate { handleNext() }
        PlanAction.Previous -> navigate {
          flowState = PlanFlowReducer.previous(flowState)
          persistProgress()
          publishFlowState()
        }
        PlanAction.Skip -> navigate { handleSkip() }
      }
    }
  }

  private suspend fun editAnswer(reduce: (PlanFlowState) -> PlanFlowState) {
    flowState = reduce(flowState)
    persistCurrentAnswer()
    publishFlowState()
  }

  private suspend fun navigate(block: suspend () -> Unit) {
    val now = System.currentTimeMillis()
    if (now - lastNavAt < NAV_DEBOUNCE_MS) return
    lastNavAt = now
    block()
  }

  private suspend fun handleNext() {
    if (flowState.isLastCard) {
      persistCurrentAnswer()
      completeAndNavigate()
      return
    }
    flowState = PlanFlowReducer.next(flowState)
    persistProgress()
    publishFlowState()
  }

  private suspend fun handleSkip() {
    val wasLastCard = flowState.isLastCard
    flowState = PlanFlowReducer.skip(flowState)
    persistCurrentAnswer()
    if (wasLastCard) {
      completeAndNavigate()
      return
    }
    persistProgress()
    publishFlowState()
  }

  private suspend fun completeAndNavigate() {
    val answers = flowState.answers.values.sortedBy { it.cardIndex }
    val record = repository.getPlan(planId)
    val exportText = PlanTextExporter.buildSummary(
      date = record?.date.orEmpty(),
      answers = answers,
    )
    repository.completePlan(planId, exportText)
    _effect.emit(PlanEffect.NavigateToResult(planId))
  }

  private suspend fun persistCurrentAnswer() {
    val answer = flowState.currentAnswer() ?: return
    repository.saveAnswer(planId, answer)
  }

  private suspend fun persistProgress() {
    repository.updateProgress(planId = planId, currentIndex = flowState.currentIndex)
  }

  private fun publishFlowState(isLoading: Boolean = false) {
    val definition = flowState.currentDefinition
    _state.update {
      PlanUiState(
        isLoading = isLoading,
        currentIndex = flowState.currentIndex,
        totalCount = PlanCardCatalog.TOTAL_COUNT,
        currentCard = definition,
        currentAnswer = flowState.currentAnswer(),
        canGoPrevious = flowState.currentIndex > 0,
        isLastCard = flowState.isLastCard,
      )
    }
  }

  private companion object {
    const val NAV_DEBOUNCE_MS = 250L
  }
}

sealed interface PlanEffect {
  data class NavigateToResult(val planId: Long) : PlanEffect
}
