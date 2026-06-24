package com.example.libui.feature.plan

import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.plan.PlanCardCatalog
import com.example.foundation.domain.plan.PlanCardDefinition

data class PlanUiState(
  val isLoading: Boolean = true,
  val currentIndex: Int = 0,
  val totalCount: Int = PlanCardCatalog.TOTAL_COUNT,
  val currentCard: PlanCardDefinition = PlanCardCatalog.cards.first(),
  val currentAnswer: PlanCardAnswer? = null,
  val canGoPrevious: Boolean = false,
  val isLastCard: Boolean = false,
)

sealed interface PlanAction {
  data class ToggleTag(val label: String) : PlanAction
  data class SelectSingle(val label: String) : PlanAction
  data class ToggleFollowUp(val label: String) : PlanAction
  data object Next : PlanAction
  data object Previous : PlanAction
  data object Skip : PlanAction
  data class SetHour(val hour: Int) : PlanAction
  data class SetNote(val text: String) : PlanAction
  data object AddExtraNote : PlanAction
  data class UpdateExtraNote(val index: Int, val text: String) : PlanAction
}
