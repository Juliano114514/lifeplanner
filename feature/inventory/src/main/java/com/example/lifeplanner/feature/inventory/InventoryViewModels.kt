package com.example.lifeplanner.feature.inventory

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.ShoppingEntry
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockItemDraft
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.StorageLocation
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.repository.ShoppingRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class InventoryUiState(
  val isLoading: Boolean = true,
  val items: List<StockItemDetails> = emptyList(),
  val errorMessage: String? = null,
)

class InventoryViewModel(
  private val repository: StockRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(InventoryUiState())
  val state: StateFlow<InventoryUiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      repository.observeStock(StockKind.HOUSEHOLD)
        .catch { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } }
        .collect { _state.value = InventoryUiState(isLoading = false, items = it) }
    }
  }

  fun update(id: Long, amount: Double?, status: StockLevel?) {
    viewModelScope.launch { repository.updateStock(id, amount, status) }
  }

  fun archive(id: Long) {
    viewModelScope.launch { repository.archiveStockItem(id) }
  }
}

data class StockEditorUiState(
  val isLoading: Boolean = false,
  val details: StockItemDetails? = null,
  val errorMessage: String? = null,
)

sealed interface StockEditorEffect {
  data object Saved : StockEditorEffect
}

class StockEditorViewModel(
  private val repository: StockRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(StockEditorUiState())
  val state: StateFlow<StockEditorUiState> = _state.asStateFlow()
  private val _effect = MutableSharedFlow<StockEditorEffect>()
  val effect: SharedFlow<StockEditorEffect> = _effect.asSharedFlow()
  private var loadedId: Long? = null

  fun load(id: Long?) {
    if (id == null || id == loadedId) return
    loadedId = id
    viewModelScope.launch {
      _state.value = StockEditorUiState(isLoading = true)
      _state.value = StockEditorUiState(details = repository.getStockItem(id))
    }
  }

  fun save(
    kind: StockKind,
    name: String,
    category: String,
    unit: String,
    trackingMode: TrackingMode,
    amount: Double?,
    status: StockLevel?,
    threshold: Double?,
    foodKind: FoodKind,
    storage: StorageLocation,
    expiry: LocalDate?,
  ) {
    viewModelScope.launch {
      runCatching {
        repository.saveStockItem(
          StockItemDraft(
            id = loadedId,
            name = name,
            category = category,
            kind = kind,
            unit = unit,
            trackingMode = trackingMode,
            currentAmount = amount,
            currentStatus = status,
            replenishThreshold = threshold,
            foodDetails = if (kind == StockKind.FOOD) {
              FoodDetails(
                stockItemId = loadedId ?: 0,
                foodKind = foodKind,
                storageLocation = storage,
                expiryDate = expiry,
              )
            } else {
              null
            },
          ),
        )
      }.onSuccess {
        _effect.emit(StockEditorEffect.Saved)
      }.onFailure { error ->
        _state.update { it.copy(errorMessage = error.message) }
      }
    }
  }
}

data class ShoppingUiState(
  val isLoading: Boolean = true,
  val entries: List<ShoppingEntry> = emptyList(),
  val errorMessage: String? = null,
)

class ShoppingViewModel(
  private val repository: ShoppingRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(ShoppingUiState())
  val state: StateFlow<ShoppingUiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      repository.observeActive()
        .catch { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } }
        .collect { _state.value = ShoppingUiState(isLoading = false, entries = it) }
    }
  }

  fun add(name: String, unit: String, amount: Double?) {
    viewModelScope.launch { repository.addManual(name, unit, amount) }
  }

  fun purchased(id: Long, amount: Double?) {
    viewModelScope.launch { repository.markPurchased(id, amount) }
  }

  fun dismiss(id: Long) {
    viewModelScope.launch { repository.dismiss(id) }
  }
}
