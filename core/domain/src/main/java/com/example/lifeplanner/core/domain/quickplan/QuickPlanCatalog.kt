package com.example.lifeplanner.core.domain.quickplan

import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.QuickPlanCardDefinition
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanFollowUp
import com.example.lifeplanner.core.domain.model.QuickPlanInteraction
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodConfig

object QuickPlanCatalog {
  private val mealOptions = listOf("自己做", "出去吃", "外卖", "不吃")
  private val mealFollowUp = QuickPlanFollowUp(
    title = "怎么做",
    options = listOf("有菜了", "要去买菜", "要外卖点菜"),
    triggers = setOf("自己做"),
  )
  private val periodLabels = mapOf(
    DayPeriod.MORNING to "早上",
    DayPeriod.AFTERNOON to "下午",
    DayPeriod.EVENING to "晚上",
  )
  private val periodsByLabel =
    periodLabels.entries.associate { (period, label) -> label to period }

  val cards: List<QuickPlanCardDefinition> = listOf(
    QuickPlanCardDefinition(
      type = QuickPlanCardType.WORK,
      title = "事项安排",
      interaction = QuickPlanInteraction.PERIOD_PLAN,
      periodConfig = QuickPlanPeriodConfig(
        activityOptions = listOf("学习", "工作", "实验", "写作", "开会", "休息"),
        locationOptions = listOf("在家", "北区", "研究生部", "出差"),
      ),
    ),
    QuickPlanCardDefinition(
      type = QuickPlanCardType.GO_OUT,
      title = "出门安排",
      interaction = QuickPlanInteraction.PERIOD_PLAN,
      periodConfig = QuickPlanPeriodConfig(
        activityOptions = listOf("出去玩", "出去办事", "出去团建"),
      ),
    ),
    mealCard(QuickPlanCardType.BREAKFAST, "早餐"),
    mealCard(QuickPlanCardType.LUNCH, "午餐"),
    mealCard(QuickPlanCardType.DINNER, "晚餐"),
    QuickPlanCardDefinition(
      QuickPlanCardType.RETURN_HOME,
      "晚上回家",
      QuickPlanInteraction.HOUR_TIME,
    ),
    QuickPlanCardDefinition(
      type = QuickPlanCardType.FITNESS,
      title = "健身",
      interaction = QuickPlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "今日练休"),
      exclusiveOption = "今日练休",
      followUp = QuickPlanFollowUp(
        title = "强度",
        options = listOf("低", "中", "高"),
        skip = setOf("今日练休"),
      ),
    ),
    QuickPlanCardDefinition(
      QuickPlanCardType.OTHER,
      "其他",
      QuickPlanInteraction.TODO_CREATE,
    ),
  )

  private fun mealCard(type: QuickPlanCardType, title: String) = QuickPlanCardDefinition(
    type = type,
    title = title,
    interaction = QuickPlanInteraction.SINGLE_TAG,
    options = mealOptions,
    followUp = mealFollowUp,
  )

  fun periodLabel(period: DayPeriod): String = periodLabels.getValue(period)

  fun periodForLabel(label: String): DayPeriod? = periodsByLabel[label]
}
