package com.example.lifeplanner.core.domain.quickplan

import com.example.lifeplanner.core.domain.model.QuickPlanCardDefinition
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanFollowUp
import com.example.lifeplanner.core.domain.model.QuickPlanInteraction

object QuickPlanCatalog {
  private val mealOptions = listOf("自己做", "出去吃", "外卖", "不吃")
  private val mealFollowUp = QuickPlanFollowUp(
    title = "怎么做",
    options = listOf("有菜了", "要去买菜", "要外卖点菜"),
    triggers = setOf("自己做"),
  )

  val cards: List<QuickPlanCardDefinition> = listOf(
    QuickPlanCardDefinition(
      type = QuickPlanCardType.WORK,
      title = "学习 / 工作",
      interaction = QuickPlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "休息"),
      exclusiveOption = "休息",
      followUp = QuickPlanFollowUp(
        title = "在哪里",
        options = listOf("在家", "北区", "研究生部", "出差"),
        skip = setOf("休息"),
        enableMultiSelect = true,
      ),
    ),
    QuickPlanCardDefinition(
      type = QuickPlanCardType.GO_OUT,
      title = "出门",
      interaction = QuickPlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "不出门"),
      exclusiveOption = "不出门",
      followUp = QuickPlanFollowUp(
        title = "出门做什么",
        options = listOf("出去玩", "出去办事", "出去团建"),
        skip = setOf("不出门"),
        enableMultiSelect = true,
      ),
    ),
    QuickPlanCardDefinition(
      QuickPlanCardType.BREAKFAST,
      "早餐",
      QuickPlanInteraction.SINGLE_TAG,
      mealOptions,
      followUp = mealFollowUp,
    ),
    QuickPlanCardDefinition(
      QuickPlanCardType.LUNCH,
      "午餐",
      QuickPlanInteraction.SINGLE_TAG,
      mealOptions,
      followUp = mealFollowUp,
    ),
    QuickPlanCardDefinition(
      QuickPlanCardType.DINNER,
      "晚餐",
      QuickPlanInteraction.SINGLE_TAG,
      mealOptions,
      followUp = mealFollowUp,
    ),
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
      QuickPlanInteraction.NOTE,
    ),
  )
}
