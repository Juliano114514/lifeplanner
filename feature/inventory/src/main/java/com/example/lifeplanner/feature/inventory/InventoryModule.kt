package com.example.lifeplanner.feature.inventory

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val inventoryFeatureModule = module {
  viewModel { InventoryViewModel(get()) }
  viewModel { StockEditorViewModel(get()) }
  viewModel { ShoppingViewModel(get()) }
}
