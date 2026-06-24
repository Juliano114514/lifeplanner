package com.example.lifeplanner.ui.home

import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.libroom.repository.PlanRepository
import com.example.libui.feature.home.HomeUiState

class HomeViewModel(
  private val planRepository: PlanRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(HomeUiState())
  val state: StateFlow<HomeUiState> = _state.asStateFlow()

  private val _effect = MutableSharedFlow<HomeEffect>()
  val effect: SharedFlow<HomeEffect> = _effect.asSharedFlow()

  private var incompletePlanId: Long? = null

  init {
    viewModelScope.launch {
      planRepository.observeIncompleteForToday().collect { record ->
        incompletePlanId = record?.id
        _state.update {
          it.copy(hasIncompletePlan = record != null)
        }
      }
    }
  }

  fun onPlanClick() {
    viewModelScope.launch {
      _effect.emit(HomeEffect.NavigateToPlan(incompletePlanId))
    }
  }
}

sealed interface HomeEffect {
  data class NavigateToPlan(val planId: Long?) : HomeEffect
}
