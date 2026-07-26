package com.example.lifeplanner.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
data object TodoRoute

@Serializable
data class ScheduleRoute(
  val epochDay: Long? = null,
  val taskOccurrenceId: Long? = null,
)

@Serializable
data object DishesRoute

@Serializable
data object InventoryRoute

@Serializable
data class TaskEditorRoute(val taskId: Long? = null)

@Serializable
data class QuickPlanRoute(val epochDay: Long)

@Serializable
data class StockEditorRoute(
  val itemId: Long? = null,
  val kind: String,
)

@Serializable
data object ShoppingListRoute
