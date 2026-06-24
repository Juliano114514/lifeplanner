package com.example.lifeplanner.ui.navigation

object Routes {
  const val HOME = "home"
  const val PLAN = "plan?planId={planId}"
  const val PLAN_RESULT = "plan_result/{planId}"

  fun planRoute(planId: Long? = null): String {
    return if (planId == null) "plan" else "plan?planId=$planId"
  }

  fun planResultRoute(planId: Long): String = "plan_result/$planId"
}
