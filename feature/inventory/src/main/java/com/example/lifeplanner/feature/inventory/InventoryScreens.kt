package com.example.lifeplanner.feature.inventory

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
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Inventory2
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.ShoppingEntry
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.StorageLocation
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.rules.StockRules
import com.example.libui.components.AppButton
import com.example.libui.components.AppCard
import com.example.libui.components.AppChoiceChip
import com.example.libui.components.AppDatePickerField
import com.example.libui.components.AppEmptyState
import com.example.libui.components.AppErrorState
import com.example.libui.components.AppFab
import com.example.libui.components.AppLoadingState
import com.example.libui.components.AppStatusBadge
import com.example.libui.components.AppStatusTone
import com.example.libui.components.AppTopBar
import com.example.libui.theme.AppSpacing
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InventoryRoute(
  onAddItem: () -> Unit,
  onEditItem: (Long) -> Unit,
  onOpenShopping: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: InventoryViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var updating by remember { mutableStateOf<StockItemDetails?>(null) }
  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(
        title = "库存记录",
        actions = {
          IconButton(onClick = onOpenShopping) {
            Icon(Icons.Rounded.ShoppingCart, "采购清单")
          }
        },
      )
    },
    floatingActionButton = { AppFab(Icons.Rounded.Add, "新增物品", onAddItem) },
  ) { padding ->
    when {
      state.isLoading -> AppLoadingState(Modifier.padding(padding))
      state.errorMessage != null -> AppErrorState(
        message = state.errorMessage.orEmpty(),
        modifier = Modifier.padding(padding),
      )
      state.items.isEmpty() -> AppEmptyState(
        title = "还没有库存记录",
        message = "添加日用品或化妆品，低库存会自动进入采购清单",
        icon = Icons.Rounded.Inventory2,
        modifier = Modifier.padding(padding),
      )
      else -> LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
      ) {
        items(state.items, key = { it.item.id }) { details ->
          StockCard(
            details,
            onUpdate = { updating = details },
            onEdit = { onEditItem(details.item.id) },
            onArchive = { viewModel.archive(details.item.id) },
          )
        }
      }
    }
  }
  updating?.let { details ->
    InventoryUpdateDialog(
      details,
      onDismiss = { updating = null },
      onSave = { amount, status ->
        viewModel.update(details.item.id, amount, status)
        updating = null
      },
    )
  }
}

@Composable
private fun StockCard(
  details: StockItemDetails,
  onUpdate: () -> Unit,
  onEdit: () -> Unit,
  onArchive: () -> Unit,
) {
  val item = details.item
  AppCard(onClick = onUpdate, modifier = Modifier.fillMaxWidth()) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
          Text(item.name, style = MaterialTheme.typography.titleMedium)
          Text("${item.category} · ${stockValue(details)}")
        }
        IconButton(onClick = onArchive) { Icon(Icons.Rounded.Archive, "归档") }
      }
      if (StockRules.needsRestock(item)) AppStatusBadge("需要补货", AppStatusTone.Warning)
      TextButton(onClick = onEdit) { Text("编辑详情") }
    }
  }
}

@Composable
private fun InventoryUpdateDialog(
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
          OutlinedTextField(value, { value = it }, label = { Text("当前余量 ${item.unit}") })
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StockEditorRoute(
  itemId: Long?,
  kind: StockKind,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: StockEditorViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  LaunchedEffect(itemId) { viewModel.load(itemId) }
  LaunchedEffect(viewModel) {
    viewModel.effect.collect { if (it == StockEditorEffect.Saved) onDone() }
  }
  val details = state.details
  var name by remember(details?.item?.id) { mutableStateOf(details?.item?.name.orEmpty()) }
  var category by remember(details?.item?.id) { mutableStateOf(details?.item?.category.orEmpty()) }
  var unit by remember(details?.item?.id) { mutableStateOf(details?.item?.unit.orEmpty()) }
  var mode by remember(details?.item?.id) { mutableStateOf(details?.item?.trackingMode ?: TrackingMode.STATUS) }
  var amount by remember(details?.item?.id) { mutableStateOf(details?.item?.currentAmount?.toString() ?: "0") }
  var threshold by remember(details?.item?.id) { mutableStateOf(details?.item?.replenishThreshold?.toString() ?: "0") }
  var status by remember(details?.item?.id) { mutableStateOf(details?.item?.currentStatus ?: StockLevel.ENOUGH) }
  var foodKind by remember(details?.item?.id) { mutableStateOf(details?.foodDetails?.foodKind ?: FoodKind.INGREDIENT) }
  var storage by remember(details?.item?.id) { mutableStateOf(details?.foodDetails?.storageLocation ?: StorageLocation.REFRIGERATED) }
  var expiry by remember(details?.item?.id) { mutableStateOf(details?.foodDetails?.expiryDate) }

  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(
        title = if (itemId == null) "新增物品" else "编辑物品",
        onBack = onDone,
      )
    },
  ) { padding ->
    LazyColumn(
      Modifier.fillMaxSize().padding(padding),
      contentPadding = androidx.compose.foundation.layout.PaddingValues(
        horizontal = AppSpacing.lg,
        vertical = AppSpacing.md,
      ),
      verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
    ) {
      item { OutlinedTextField(name, { name = it }, label = { Text("名称") }, modifier = Modifier.fillMaxWidth()) }
      item { OutlinedTextField(category, { category = it }, label = { Text("分类") }, modifier = Modifier.fillMaxWidth()) }
      item { OutlinedTextField(unit, { unit = it }, label = { Text("单位") }, modifier = Modifier.fillMaxWidth()) }
      item {
        Text("记录方式")
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
          TrackingMode.entries.forEach { value ->
            AppChoiceChip(value.modeLabel(), mode == value, { mode = value }, modifier = Modifier.weight(1f))
          }
        }
      }
      if (mode == TrackingMode.STATUS) {
        item {
          Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            StockLevel.entries.forEach { value ->
              AppChoiceChip(value.label(), status == value, { status = value }, modifier = Modifier.weight(1f))
            }
          }
        }
      } else {
        item { OutlinedTextField(amount, { amount = it }, label = { Text("当前余量") }, modifier = Modifier.fillMaxWidth()) }
        item { OutlinedTextField(threshold, { threshold = it }, label = { Text("补货阈值") }, modifier = Modifier.fillMaxWidth()) }
      }
      if (kind == StockKind.FOOD) {
        item {
          Text("食物类型")
          Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            FoodKind.entries.forEach { value ->
              AppChoiceChip(
                text = if (value == FoodKind.PREPARED) "熟食" else "食材",
                selected = foodKind == value,
                onClick = { foodKind = value },
              )
            }
          }
        }
        item {
          Text("存放位置")
          Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.sm)) {
            StorageLocation.entries.forEach { value ->
              AppChoiceChip(value.storageLabel(), storage == value, { storage = value }, modifier = Modifier.weight(1f))
            }
          }
        }
        item {
          AppDatePickerField(
            value = expiry,
            onValueChange = { expiry = it },
            label = "保质期",
            modifier = Modifier.fillMaxWidth(),
            optional = true,
          )
        }
      }
      item {
        state.errorMessage?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        AppButton(
          text = "保存",
          onClick = {
            viewModel.save(
              kind = kind,
              name = name,
              category = category,
              unit = unit,
              trackingMode = mode,
              amount = if (mode == TrackingMode.STATUS) null else amount.toDoubleOrNull(),
              status = if (mode == TrackingMode.STATUS) status else null,
              threshold = if (mode == TrackingMode.STATUS) null else threshold.toDoubleOrNull(),
              foodKind = foodKind,
              storage = storage,
              expiry = expiry,
            )
          },
          modifier = Modifier.fillMaxWidth(),
        )
      }
    }
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListRoute(
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: ShoppingViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  var adding by remember { mutableStateOf(false) }
  var purchasing by remember { mutableStateOf<ShoppingEntry?>(null) }
  Scaffold(
    modifier = modifier,
    topBar = {
      AppTopBar(title = "统一采购清单", onBack = onDone)
    },
    floatingActionButton = {
      AppFab(
        icon = Icons.Rounded.Add,
        contentDescription = "手动添加",
        onClick = { adding = true },
      )
    },
  ) { padding ->
    if (state.entries.isEmpty()) {
      AppEmptyState(
        title = "采购清单是空的",
        message = "低库存物品会自动出现，也可以手动添加",
        icon = Icons.Rounded.ShoppingCart,
        modifier = Modifier.padding(padding),
      )
    } else {
      LazyColumn(
        Modifier.fillMaxSize().padding(padding),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(AppSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.md),
      ) {
        items(state.entries, key = { it.id }) { entry ->
          AppCard(onClick = { purchasing = entry }, modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Column(Modifier.weight(1f)) {
                Text(entry.name, style = MaterialTheme.typography.titleMedium)
                Text(if (entry.source == com.example.lifeplanner.core.domain.model.ShoppingSource.AUTO) "库存自动加入" else "手动添加")
              }
              IconButton(onClick = { viewModel.dismiss(entry.id) }) {
                Icon(Icons.Rounded.Delete, "移除")
              }
            }
          }
        }
      }
    }
  }
  if (adding) {
    ManualShoppingDialog(
      onDismiss = { adding = false },
      onSave = { name, unit, amount ->
        viewModel.add(name, unit, amount)
        adding = false
      },
    )
  }
  purchasing?.let { entry ->
    PurchaseDialog(
      entry,
      onDismiss = { purchasing = null },
      onPurchased = { amount ->
        viewModel.purchased(entry.id, amount)
        purchasing = null
      },
    )
  }
}

@Composable
private fun ManualShoppingDialog(
  onDismiss: () -> Unit,
  onSave: (String, String, Double?) -> Unit,
) {
  var name by remember { mutableStateOf("") }
  var unit by remember { mutableStateOf("") }
  var amount by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("手动添加采购项") },
    text = {
      Column {
        OutlinedTextField(name, { name = it }, label = { Text("名称") })
        OutlinedTextField(unit, { unit = it }, label = { Text("单位") })
        OutlinedTextField(amount, { amount = it }, label = { Text("期望数量（可选）") })
      }
    },
    confirmButton = { TextButton(onClick = { if (name.isNotBlank()) onSave(name, unit, amount.toDoubleOrNull()) }) { Text("添加") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}

@Composable
private fun PurchaseDialog(
  entry: ShoppingEntry,
  onDismiss: () -> Unit,
  onPurchased: (Double?) -> Unit,
) {
  var amount by remember { mutableStateOf("") }
  AlertDialog(
    onDismissRequest = onDismiss,
    title = { Text("已购买 ${entry.name}") },
    text = {
      Column {
        Text("填写购入量可同时回写库存；留空则只完成采购项。")
        OutlinedTextField(amount, { amount = it }, label = { Text("购入量（可选）") })
      }
    },
    confirmButton = { TextButton(onClick = { onPurchased(amount.toDoubleOrNull()) }) { Text("完成") } },
    dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
  )
}

private fun stockValue(details: StockItemDetails): String = when (details.item.trackingMode) {
  TrackingMode.QUANTITY -> "${details.item.currentAmount ?: 0.0} ${details.item.unit}"
  TrackingMode.PERCENT -> "${details.item.currentAmount ?: 0.0}%"
  TrackingMode.STATUS -> details.item.currentStatus?.label().orEmpty()
}

private fun TrackingMode.modeLabel(): String = when (this) {
  TrackingMode.QUANTITY -> "数量"
  TrackingMode.PERCENT -> "百分比"
  TrackingMode.STATUS -> "状态"
}

private fun StockLevel.label(): String = when (this) {
  StockLevel.MISSING -> "缺"
  StockLevel.LOW -> "较少"
  StockLevel.ENOUGH -> "足够"
  StockLevel.EXCESS -> "过多"
}

private fun StorageLocation.storageLabel(): String = when (this) {
  StorageLocation.REFRIGERATED -> "冷藏"
  StorageLocation.FROZEN -> "冷冻"
  StorageLocation.ROOM_TEMPERATURE -> "常温"
  StorageLocation.OTHER -> "其他"
}
