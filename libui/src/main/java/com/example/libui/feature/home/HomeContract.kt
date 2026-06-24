package com.example.libui.feature.home

data class HomeUiState(
  val hasIncompletePlan: Boolean = false,
)

sealed interface HomeAction {
  data object OnPlanClick : HomeAction
  data object OnInventoryClick : HomeAction
}
