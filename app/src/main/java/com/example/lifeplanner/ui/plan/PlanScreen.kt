package com.example.lifeplanner.ui.plan

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.libui.feature.plan.PlanContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlanScreen(
  planId: Long?,
  onNavigateToResult: (Long) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlanViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(planId) {
    viewModel.initialize(planId)
  }

  LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is PlanEffect.NavigateToResult -> onNavigateToResult(effect.planId)
      }
    }
  }

  PlanContent(
    state = state,
    onAction = viewModel::onAction,
    modifier = modifier,
  )
}
