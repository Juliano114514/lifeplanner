package com.example.foundation.domain.plan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanFlowReducerTest {

  @Test
  fun goOut_followUp_singleSelect_replacesPreviousChoice() {
    var state = PlanFlowState(currentIndex = PlanCardCatalog.indexOf(com.example.foundation.domain.model.PlanCardType.GO_OUT))
    assertNull(state.currentDefinition.activeFollowUp(emptyList()))

    state = PlanFlowReducer.toggleTag(state, "早上")
    assertEquals("出门做什么", state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions)?.title)

    state = PlanFlowReducer.toggleFollowUp(state, "出去玩")
    state = PlanFlowReducer.toggleFollowUp(state, "出去办事")
    assertEquals(listOf("出去办事"), state.currentAnswer()?.subSelections)

    state = PlanFlowReducer.toggleTag(state, "不出门")
    assertNull(state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions))
    assertTrue(state.currentAnswer()?.subSelections.orEmpty().isEmpty())
  }

  @Test
  fun work_followUp_supportsMultiSelect() {
    var state = PlanFlowState(currentIndex = PlanCardCatalog.indexOf(com.example.foundation.domain.model.PlanCardType.WORK))

    state = PlanFlowReducer.toggleTag(state, "下午")
    assertEquals("在哪里", state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions)?.title)

    state = PlanFlowReducer.toggleFollowUp(state, "在家")
    state = PlanFlowReducer.toggleFollowUp(state, "北区")
    assertEquals(listOf("在家", "北区"), state.currentAnswer()?.subSelections)

    state = PlanFlowReducer.toggleTag(state, "休息")
    assertNull(state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions))
    assertTrue(state.currentAnswer()?.subSelections.orEmpty().isEmpty())
  }

  @Test
  fun breakfast_followUp_singleSelect_onlyWhenCookAtHome() {
    var state = PlanFlowState(currentIndex = PlanCardCatalog.indexOf(com.example.foundation.domain.model.PlanCardType.BREAKFAST))

    state = PlanFlowReducer.selectSingle(state, "外卖")
    assertNull(state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions))

    state = PlanFlowReducer.selectSingle(state, "自己做")
    val followUp = state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions)
    assertEquals("怎么做", followUp?.title)
    assertEquals(listOf("有菜了", "要去买菜", "要外卖点菜"), followUp?.options)

    state = PlanFlowReducer.toggleFollowUp(state, "有菜了")
    state = PlanFlowReducer.toggleFollowUp(state, "要去买菜")
    assertEquals(listOf("要去买菜"), state.currentAnswer()?.subSelections)

    state = PlanFlowReducer.selectSingle(state, "不吃")
    assertTrue(state.currentAnswer()?.subSelections.orEmpty().isEmpty())
  }

  @Test
  fun fitness_followUp_skippedForRestDay() {
    var state = PlanFlowState(currentIndex = PlanCardCatalog.indexOf(com.example.foundation.domain.model.PlanCardType.FITNESS))

    state = PlanFlowReducer.toggleTag(state, "早上")
    assertEquals("强度", state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions)?.title)

    state = PlanFlowReducer.toggleFollowUp(state, "高")
    assertEquals(listOf("高"), state.currentAnswer()?.subSelections)

    state = PlanFlowReducer.toggleTag(state, "今日练休")
    assertNull(state.currentDefinition.activeFollowUp(state.currentAnswer()!!.selectedOptions))
    assertTrue(state.currentAnswer()?.subSelections.orEmpty().isEmpty())
  }
}
