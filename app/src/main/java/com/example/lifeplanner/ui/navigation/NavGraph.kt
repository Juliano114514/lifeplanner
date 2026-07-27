package com.example.lifeplanner.ui.navigation

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.MenuBook
import androidx.compose.material.icons.rounded.CalendarMonth
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Inventory2
import androidx.compose.material.icons.rounded.Restaurant
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavDestination
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.feature.dishes.DishesRoute as DishesScreen
import com.example.lifeplanner.feature.diary.DiaryEditorActivity
import com.example.lifeplanner.feature.diary.DiaryRoute as DiaryScreen
import com.example.lifeplanner.feature.inventory.InventoryRoute as InventoryScreen
import com.example.lifeplanner.feature.inventory.ShoppingListRoute as ShoppingListScreen
import com.example.lifeplanner.feature.inventory.StockEditorRoute as StockEditorScreen
import com.example.lifeplanner.feature.schedule.QuickPlanRoute as QuickPlanScreen
import com.example.lifeplanner.feature.schedule.ScheduleRoute as ScheduleScreen
import com.example.lifeplanner.feature.todo.TodoRoute as TodoScreen
import com.example.libui.theme.AppElevation
import com.example.libui.theme.AppMotion
import java.time.LocalDate

private data class TopLevelDestination(
  val route: Any,
  val label: String,
  val icon: ImageVector,
  val selected: (NavDestination?) -> Boolean,
)

private val topLevelDestinations = listOf(
  TopLevelDestination(TodoRoute, "任务", Icons.Rounded.CheckCircle) { it.hasRouteName<TodoRoute>() },
  TopLevelDestination(ScheduleRoute(), "日程", Icons.Rounded.CalendarMonth) { it.hasRouteName<ScheduleRoute>() },
  TopLevelDestination(DiaryRoute, "日记", Icons.AutoMirrored.Rounded.MenuBook) {
    it.hasRouteName<DiaryRoute>()
  },
  TopLevelDestination(DishesRoute, "菜品", Icons.Rounded.Restaurant) { it.hasRouteName<DishesRoute>() },
  TopLevelDestination(InventoryRoute, "库存", Icons.Rounded.Inventory2) { it.hasRouteName<InventoryRoute>() },
)

@Composable
fun LifePlannerApp(
  modifier: Modifier = Modifier,
  navController: NavHostController = rememberNavController(),
) {
  val backStackEntry by navController.currentBackStackEntryAsState()
  val destination = backStackEntry?.destination
  val showBottomBar = topLevelDestinations.any { it.selected(destination) }

  Scaffold(
    modifier = modifier,
    bottomBar = {
      if (showBottomBar) {
        NavigationBar(
          containerColor = androidx.compose.material3.MaterialTheme.colorScheme.surface,
          tonalElevation = AppElevation.none,
        ) {
          topLevelDestinations.forEach { item ->
            NavigationBarItem(
              selected = item.selected(destination),
              onClick = { navController.navigateTopLevel(item.route) },
              icon = { Icon(item.icon, contentDescription = item.label) },
              label = { Text(item.label) },
              colors = NavigationBarItemDefaults.colors(
                selectedIconColor = androidx.compose.material3.MaterialTheme.colorScheme.onPrimaryContainer,
                selectedTextColor = androidx.compose.material3.MaterialTheme.colorScheme.onSurface,
                indicatorColor = androidx.compose.material3.MaterialTheme.colorScheme.primaryContainer,
              ),
            )
          }
        }
      }
    },
  ) { padding ->
    NavHost(
      navController = navController,
      startDestination = TodoRoute,
      modifier = Modifier.padding(padding),
      enterTransition = {
        if (initialState.destination.isTopLevel() && targetState.destination.isTopLevel()) {
          fadeIn(
            animationSpec = tween(
              durationMillis = AppMotion.durationMedium,
              delayMillis = AppMotion.durationShort,
              easing = AppMotion.standard,
            ),
          )
        } else {
          fadeIn(
            animationSpec = tween(
              durationMillis = AppMotion.durationMedium,
              easing = AppMotion.standard,
            ),
          ) + slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Start,
            animationSpec = tween(
              durationMillis = AppMotion.durationNavigation,
              easing = AppMotion.emphasizedDecelerate,
            ),
            initialOffset = { distance -> distance / 16 },
          )
        }
      },
      exitTransition = {
        fadeOut(
          animationSpec = tween(
            durationMillis = AppMotion.durationShort,
            easing = AppMotion.standardAccelerate,
          ),
        )
      },
      popEnterTransition = {
        fadeIn(
          animationSpec = tween(
            durationMillis = AppMotion.durationMedium,
            easing = AppMotion.standard,
          ),
        ) + slideIntoContainer(
          towards = AnimatedContentTransitionScope.SlideDirection.End,
          animationSpec = tween(
            durationMillis = AppMotion.durationNavigation,
            easing = AppMotion.emphasizedDecelerate,
          ),
          initialOffset = { distance -> distance / 16 },
        )
      },
      popExitTransition = {
        fadeOut(
          animationSpec = tween(
            durationMillis = AppMotion.durationShort,
            easing = AppMotion.standardAccelerate,
          ),
        )
      },
    ) {
      composable<TodoRoute> {
        TodoScreen(
          onScheduleTask = {
            navController.navigateTopLevel(
              ScheduleRoute(
                epochDay = LocalDate.now().toEpochDay(),
                taskOccurrenceId = it,
              ),
            )
          },
          onOpenSchedule = { epochDay ->
            navController.navigateTopLevel(ScheduleRoute(epochDay = epochDay))
          },
        )
      }
      composable<ScheduleRoute> { entry ->
        val route = entry.toRoute<ScheduleRoute>()
        ScheduleScreen(
          initialEpochDay = route.epochDay,
          taskOccurrenceId = route.taskOccurrenceId,
          onOpenQuickPlan = { navController.navigate(QuickPlanRoute(it)) },
        )
      }
      composable<DiaryRoute> {
        val context = LocalContext.current
        DiaryScreen(
          onOpenEditor = { epochDay, entryId ->
            context.startActivity(
              DiaryEditorActivity.createIntent(
                context = context,
                epochDay = epochDay,
                entryId = entryId,
              ),
            )
          },
        )
      }
      composable<DishesRoute> {
        DishesScreen(
          onAddFood = { navController.navigate(StockEditorRoute(kind = StockKind.FOOD.name)) },
          onEditFood = { navController.navigate(StockEditorRoute(it, StockKind.FOOD.name)) },
          onOpenShopping = { navController.navigate(ShoppingListRoute) },
        )
      }
      composable<InventoryRoute> {
        InventoryScreen(
          onAddItem = { navController.navigate(StockEditorRoute(kind = StockKind.HOUSEHOLD.name)) },
          onEditItem = { navController.navigate(StockEditorRoute(it, StockKind.HOUSEHOLD.name)) },
          onOpenShopping = { navController.navigate(ShoppingListRoute) },
        )
      }
      composable<QuickPlanRoute> { entry ->
        QuickPlanScreen(
          epochDay = entry.toRoute<QuickPlanRoute>().epochDay,
          onDone = navController::popBackStack,
        )
      }
      composable<StockEditorRoute> { entry ->
        val route = entry.toRoute<StockEditorRoute>()
        StockEditorScreen(
          itemId = route.itemId,
          kind = StockKind.valueOf(route.kind),
          onDone = navController::popBackStack,
        )
      }
      composable<ShoppingListRoute> {
        ShoppingListScreen(onDone = navController::popBackStack)
      }
    }
  }
}

private fun NavHostController.navigateTopLevel(route: Any) {
  navigate(route) {
    popUpTo<TodoRoute>()
    launchSingleTop = true
  }
}

private inline fun <reified T : Any> NavDestination?.hasRouteName(): Boolean {
  val name = T::class.qualifiedName ?: return false
  return this?.route?.substringBefore("?") == name
}

private fun NavDestination.isTopLevel(): Boolean =
  topLevelDestinations.any { it.selected(this) }
