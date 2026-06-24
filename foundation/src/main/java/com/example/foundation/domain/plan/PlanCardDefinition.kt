package com.example.foundation.domain.plan

import com.example.foundation.domain.model.PlanCardType

data class PlanCardDefinition(
  val type: PlanCardType,
  val title: String,
  val interaction: PlanInteraction,
  val options: List<String> = emptyList(),
  val exclusiveOption: String? = null,
  val followUp: FollowUp? = null,
)

/**
 * 条件二级选单：主选命中触发条件后，在同一张卡内追加一步单选。
 * 健身（任意时段 → 强度）与三餐（自己做 → 怎么做）共用此抽象，无需各自的两步分支。
 */
data class FollowUp(
  val title: String,
  val options: List<String>,
  val triggers: Set<String> = emptySet(), // 空集表示任意主选都触发
  val skip: Set<String> = emptySet(), // 命中则不展示二级
)

/** 给定当前主选项，返回应展示的二级选单；不满足触发条件返回 null。 */
fun PlanCardDefinition.activeFollowUp(selectedOptions: List<String>): FollowUp? {
  val followUp = followUp ?: return null
  if (selectedOptions.isEmpty()) return null
  if (selectedOptions.any { it in followUp.skip }) return null
  val triggered = followUp.triggers.isEmpty() || selectedOptions.any { it in followUp.triggers }
  return followUp.takeIf { triggered }
}
