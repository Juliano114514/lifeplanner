package com.example.lifeplanner.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.lifeplanner.ui.home.HomeScreen
import com.example.lifeplanner.ui.plan.PlanResultScreen
import com.example.lifeplanner.ui.plan.PlanScreen

@Composable
fun LifePlannerNavGraph(
  navController: NavHostController,
  modifier: Modifier = Modifier,
) {
  NavHost(
    navController = navController,
    startDestination = Routes.HOME,
    modifier = modifier,
  ) {
    composable(Routes.HOME) {
      HomeScreen(
        onNavigateToPlan = { planId ->
          navController.navigate(Routes.planRoute(planId)) {
            launchSingleTop = true
          }
        },
      )
    }
    composable(
      route = Routes.PLAN,
      arguments = listOf(
        navArgument("planId") {
          type = NavType.LongType
          defaultValue = -1L
        },
      ),
    ) { entry ->
      val planId = entry.arguments?.getLong("planId")?.takeIf { it > 0 }
      PlanScreen(
        planId = planId,
        onNavigateToResult = { completedPlanId ->
          navController.navigate(Routes.planResultRoute(completedPlanId)) {
            popUpTo(Routes.HOME)
            launchSingleTop = true
          }
        },
      )
    }
    composable(
      route = Routes.PLAN_RESULT,
      arguments = listOf(navArgument("planId") { type = NavType.LongType }),
    ) { entry ->
      val planId = entry.arguments?.getLong("planId") ?: return@composable
      PlanResultScreen(
        planId = planId,
        onDone = {
          navController.navigate(Routes.HOME) {
            popUpTo(Routes.HOME) { inclusive = true }
            launchSingleTop = true
          }
        },
      )
    }
  }
}
