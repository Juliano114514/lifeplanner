package com.example.lifeplanner.ui.home

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.libui.feature.home.HomeContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun HomeScreen(
  onNavigateToPlan: (Long?) -> Unit,
  modifier: Modifier = Modifier,
  viewModel: HomeViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()

  LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is HomeEffect.NavigateToPlan -> onNavigateToPlan(effect.planId)
      }
    }
  }

  HomeContent(
    state = state,
    onAction = { action ->
      when (action) {
        com.example.libui.feature.home.HomeAction.OnPlanClick -> viewModel.onPlanClick()
        com.example.libui.feature.home.HomeAction.OnInventoryClick -> Unit
      }
    },
    modifier = modifier,
  )
}
