package com.example.lifeplanner.feature.dishes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Archive
import androidx.compose.material.icons.rounded.Kitchen
import androidx.compose.material.icons.rounded.ShoppingCart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.repository.StockRepository
import com.example.lifeplanner.core.domain.rules.StockRules
import com.example.libui.components.AppCard
import com.example.libui.components.AppChoiceChip
import com.example.libui.components.AppEmptyState
import com.example.libui.components.AppErrorState
import com.example.libui.components.AppFab
import com.example.libui.components.AppLoadingState
import com.example.libui.components.AppStatusBadge
import com.example.libui.components.AppStatusTone
import com.example.libui.components.AppTopBar
import com.example.libui.theme.AppSpacing
import java.time.LocalDate
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.androidx.compose.koinViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

data class DishesUiState(
  val isLoading: Boolean = true,
  val items: List<StockItemDetails> = emptyList(),
  val errorMessage: String? = null,
)

class DishesViewModel(
  private val repository: StockRepository,
) : ViewModel() {
  private val _state = MutableStateFlow(DishesUiState())
  val state: StateFlow<DishesUiState> = _state.asStateFlow()

  init {
    viewModelScope.launch {
      repository.observeStock(StockKind.FOOD)
        .catch { error -> _state.update { it.copy(isLoading = false, errorMessage = error.message) } }
        .collect { _state.value = DishesUiState(isLoading = false, items = it) }
    }
  }

  fun update(item: StockItemDetails, amount: Double?, status: StockLevel?) {
    viewModelScope.launch { repository.updateStock(item.item.id, amount, status) }
  }

  fun archive(id: Long) {
    viewModelScope.launch { repository.archiveStockItem(id) }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DishesRoute(
  onAddFood: () -> Unit,
  onEditFood: (Long) -> Unit,
  onOpenShopping: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: DishesViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var updating by remember { mutableStateOf<StockItemDetails?>(null) }
  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(
        title = "菜品记录",
        actions = {
          IconButton(onClick = onOpenShopping) {
            Icon(Icons.Rounded.ShoppingCart, contentDescription = "采购清单")
          }
        },
      )
    },
    floatingActionButton = { AppFab(Icons.Rounded.Add, "新增菜品", onAddFood) },
  ) { padding ->
    when {
      state.isLoading -> AppLoadingState(Modifier.padding(padding))
      state.errorMessage != null -> AppErrorState(
        message = state.errorMessage.orEmpty(),
        modifier = Modifier.padding(padding),
      )
      state.items.isEmpty() -> AppEmptyState(
        title = "冰箱里还没有记录",
        message = "添加剩菜或食材，及时查看余量与保质期",
        icon = Icons.Rounded.Kitchen,
        modifier = Modifier.padding(padding),
      )
      else -> LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
      ) {
        items(state.items, key = { it.item.id }) { details ->
          FoodCard(
            details,
            onClick = { updating = details },
            onEdit = { onEditFood(details.item.id) },
            onArchive = { viewModel.archive(details.item.id) },
          )
        }
      }
    }
  }
  updating?.let { details ->
    StockUpdateDialog(
      details = details,
      onDismiss = { updating = null },
      onSave = { amount, status ->
        viewModel.update(details, amount, status)
        updating = null
      },
    )
  }
}

@Composable
private fun FoodCard(
  details: StockItemDetails,
  onClick: () -> Unit,
  onEdit: () -> Unit,
  onArchive: () -> Unit,
) {
  val item = details.item
  val foodDetails = details.foodDetails
  val expiry = foodDetails?.expiryDate
  val expiring = expiry != null &&
    !expiry.isAfter(LocalDate.now().plusDays(foodDetails.expiryWarningDays.toLong()))
  AppCard(onClick = onClick, modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(item.name, style = MaterialTheme.typography.titleMedium)
          Text("${item.category} · ${stockLabel(details)}")
        }
        IconButton(onClick = onArchive) { Icon(Icons.Rounded.Archive, "归档") }
      }
      if (expiring) {
        AppStatusBadge(
          text = if (expiry!!.isBefore(LocalDate.now())) "已过期 · $expiry" else "临期 · $expiry",
          tone = AppStatusTone.Error,
        )
      }
      if (StockRules.needsRestock(item)) AppStatusBadge("需要补货", AppStatusTone.Warning)
      TextButton(onClick = onEdit) { Text("编辑详情") }
    }
  }
}

@Composable
private fun StockUpdateDialog(
  details: StockItemDetails,
  onDismiss: () -> Unit,
  onSave: (Double?, StockLevel?) -> Unit,
) {
  val item = details.item
  var value by remember { mutableStateOf(item.currentAmount?.toString().orEmpty()) }
  var status by remember { mutableStateOf(item.currentStatus ?: StockLevel.ENOUGH) }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("更新 ${item.name}") },
    text = {
      Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
        if (item.trackingMode == TrackingMode.STATUS) {
          StockLevel.entries.forEach { level ->
            AppChoiceChip(
              text = level.label(),
              selected = status == level,
              onClick = { status = level },
              modifier = Modifier.fillMaxWidth(),
            )
          }
        } else {
          OutlinedTextField(
            value,
            { value = it },
            label = { Text(if (item.trackingMode == TrackingMode.PERCENT) "剩余百分比" else "剩余数量 ${item.unit}") },
          )
        }
      }
    },
    confirmButton = {
      TextButton(onClick = {
        if (item.trackingMode == TrackingMode.STATUS) onSave(null, status)
        else value.toDoubleOrNull()?.let { onSave(it, null) }
      }) { Text("保存") }
    },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}

private fun stockLabel(details: StockItemDetails): String = when (details.item.trackingMode) {
  TrackingMode.QUANTITY -> "${details.item.currentAmount ?: 0.0} ${details.item.unit}"
  TrackingMode.PERCENT -> "${details.item.currentAmount ?: 0.0}%"
  TrackingMode.STATUS -> details.item.currentStatus?.label().orEmpty()
}

private fun StockLevel.label(): String = when (this) {
  StockLevel.MISSING -> "缺"
  StockLevel.LOW -> "较少"
  StockLevel.ENOUGH -> "足够"
  StockLevel.EXCESS -> "过多"
}

val dishesFeatureModule = module {
  viewModel { DishesViewModel(get()) }
}
