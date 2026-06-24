package com.example.foundation.domain.plan

import com.example.foundation.domain.model.PlanCardType

object PlanCardCatalog {
  const val TOTAL_COUNT = 8

  private val mealOptions = listOf("自己做", "出去吃", "外卖", "不吃")
  private val cookFollowUp = FollowUp(
    title = "怎么做",
    options = listOf("有菜了", "没菜去买菜", "没菜外卖点菜"),
    triggers = setOf("自己做"),
  )

  val cards: List<PlanCardDefinition> = listOf(
    PlanCardDefinition(
      type = PlanCardType.GO_OUT,
      title = "出门",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早", "下午", "晚上", "不出门"),
      exclusiveOption = "不出门",
    ),
    PlanCardDefinition(
      type = PlanCardType.WORK,
      title = "学习 / 工作",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早", "下午", "晚上", "休息"),
      exclusiveOption = "休息",
    ),
    PlanCardDefinition(
      type = PlanCardType.FITNESS,
      title = "健身",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早", "下午", "晚上", "今日练休"),
      exclusiveOption = "今日练休",
      followUp = FollowUp(
        title = "强度",
        options = listOf("低", "中", "高"),
        skip = setOf("今日练休"),
      ),
    ),
    PlanCardDefinition(
      type = PlanCardType.BREAKFAST,
      title = "早餐",
      interaction = PlanInteraction.SINGLE_TAG,
      options = mealOptions,
      followUp = cookFollowUp,
    ),
    PlanCardDefinition(
      type = PlanCardType.LUNCH,
      title = "午餐",
      interaction = PlanInteraction.SINGLE_TAG,
      options = mealOptions,
      followUp = cookFollowUp,
    ),
    PlanCardDefinition(
      type = PlanCardType.DINNER,
      title = "晚餐",
      interaction = PlanInteraction.SINGLE_TAG,
      options = mealOptions,
      followUp = cookFollowUp,
    ),
    PlanCardDefinition(
      type = PlanCardType.RETURN_HOME,
      title = "晚上回家",
      interaction = PlanInteraction.HOUR_TIME,
    ),
    PlanCardDefinition(
      type = PlanCardType.OTHER,
      title = "其他",
      interaction = PlanInteraction.NOTE,
    ),
  )

  fun definitionAt(index: Int): PlanCardDefinition = cards[index]

  fun indexOf(type: PlanCardType): Int = cards.indexOfFirst { it.type == type }
}
