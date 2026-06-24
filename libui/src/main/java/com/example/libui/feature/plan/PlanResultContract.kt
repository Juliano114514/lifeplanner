package com.example.libui.feature.plan

data class PlanResultUiState(
  val date: String = "",
  val summaryText: String = "",
  val isLoading: Boolean = true,
)

sealed interface PlanResultAction {
  data object Copy : PlanResultAction
  data object Done : PlanResultAction
}
