package com.example.foundation.domain.plan

import com.example.foundation.domain.model.PlanCardType

object PlanCardCatalog {
  const val TOTAL_COUNT = 8

  private val mealOptions = listOf("自己做", "出去吃", "外卖", "不吃")
  private val cookFollowUp = FollowUp(
    title = "怎么做",
    options = listOf("有菜了", "要去买菜", "要外卖点菜"),
    triggers = setOf("自己做"),
  )
  private val goOutFollowUp = FollowUp(
    title = "出门做什么",
    options = listOf("出去玩", "出去办事", "出去团建"),
    skip = setOf("不出门"),
  )
  private val workFollowUp = FollowUp(
    title = "在哪里",
    options = listOf("在家", "北区", "研究生部", "出差"),
    skip = setOf("休息"),
    enableMultiSelect = true,
  )

  val cards: List<PlanCardDefinition> = listOf(
    PlanCardDefinition(
      type = PlanCardType.WORK,
      title = "学习 / 工作",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "休息"),
      exclusiveOption = "休息",
      followUp = workFollowUp,
    ),
    PlanCardDefinition(
      type = PlanCardType.GO_OUT,
      title = "出门",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "不出门"),
      exclusiveOption = "不出门",
      followUp = goOutFollowUp,
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
      type = PlanCardType.FITNESS,
      title = "健身",
      interaction = PlanInteraction.MULTI_TAG,
      options = listOf("早上", "下午", "晚上", "今日练休"),
      exclusiveOption = "今日练休",
      followUp = FollowUp(
        title = "强度",
        options = listOf("低", "中", "高"),
        skip = setOf("今日练休"),
      ),
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
