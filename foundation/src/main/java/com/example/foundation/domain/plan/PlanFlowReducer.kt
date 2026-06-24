package com.example.foundation.domain.plan

import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.model.PlanCardType

data class PlanFlowState(
  val currentIndex: Int = 0,
  val answers: Map<PlanCardType, PlanCardAnswer> = emptyMap(),
) {
  val currentDefinition: PlanCardDefinition
    get() = PlanCardCatalog.definitionAt(currentIndex)

  val isLastCard: Boolean
    get() = currentIndex == PlanCardCatalog.TOTAL_COUNT - 1

  fun currentAnswer(): PlanCardAnswer? = answers[currentDefinition.type]
}

object PlanFlowReducer {
  fun toggleTag(state: PlanFlowState, label: String): PlanFlowState {
    val definition = state.currentDefinition
    val selected = state.currentAnswer()?.selectedOptions.orEmpty().toMutableList()
    when {
      label == definition.exclusiveOption -> {
        selected.clear()
        selected.add(label)
      }
      else -> {
        definition.exclusiveOption?.let(selected::remove)
        if (!selected.remove(label)) selected.add(label)
      }
    }
    return updateAnswer(state, selectedOptions = selected)
  }

  fun selectSingle(state: PlanFlowState, label: String): PlanFlowState =
    updateAnswer(state, selectedOptions = listOf(label))

  fun selectFollowUp(state: PlanFlowState, label: String): PlanFlowState =
    updateAnswer(state, subSelection = label)

  fun setHour(state: PlanFlowState, hour: Int): PlanFlowState =
    updateAnswer(state, timeValue = "%02d:00".format(hour))

  fun setNote(state: PlanFlowState, text: String): PlanFlowState =
    updateAnswer(state, noteText = text)

  fun addExtraNote(state: PlanFlowState): PlanFlowState =
    updateAnswer(state, extraNotes = state.currentAnswer()?.extraNotes.orEmpty() + "")

  fun updateExtraNote(state: PlanFlowState, index: Int, text: String): PlanFlowState {
    val extras = state.currentAnswer()?.extraNotes.orEmpty().toMutableList()
    if (index in extras.indices) extras[index] = text
    return updateAnswer(state, extraNotes = extras)
  }

  fun next(state: PlanFlowState): PlanFlowState {
    if (state.isLastCard) return state
    return state.copy(currentIndex = state.currentIndex + 1)
  }

  fun previous(state: PlanFlowState): PlanFlowState {
    if (state.currentIndex == 0) return state
    return state.copy(currentIndex = state.currentIndex - 1)
  }

  fun skip(state: PlanFlowState): PlanFlowState {
    val cleared = state.copy(
      answers = state.answers + (state.currentDefinition.type to emptyAnswer(state)),
    )
    return next(cleared)
  }

  fun restore(currentIndex: Int, answers: List<PlanCardAnswer>): PlanFlowState =
    PlanFlowState(
      currentIndex = currentIndex.coerceIn(0, PlanCardCatalog.TOTAL_COUNT - 1),
      answers = answers.associateBy { it.cardType },
    )

  private fun emptyAnswer(state: PlanFlowState): PlanCardAnswer =
    PlanCardAnswer(cardType = state.currentDefinition.type, cardIndex = state.currentIndex)

  private fun updateAnswer(
    state: PlanFlowState,
    selectedOptions: List<String> = state.currentAnswer()?.selectedOptions.orEmpty(),
    subSelection: String? = state.currentAnswer()?.subSelection,
    timeValue: String? = state.currentAnswer()?.timeValue,
    noteText: String? = state.currentAnswer()?.noteText,
    extraNotes: List<String> = state.currentAnswer()?.extraNotes.orEmpty(),
  ): PlanFlowState {
    val definition = state.currentDefinition
    // 主选变化后，若二级不再适用或选项失效，则清除旧的二级选择，避免脏数据。
    val activeOptions = definition.activeFollowUp(selectedOptions)?.options
    val resolvedSub = subSelection?.takeIf { activeOptions?.contains(it) == true }
    val answer = PlanCardAnswer(
      cardType = definition.type,
      cardIndex = state.currentIndex,
      selectedOptions = selectedOptions,
      subSelection = resolvedSub,
      timeValue = timeValue,
      noteText = noteText,
      extraNotes = extraNotes,
    )
    return state.copy(answers = state.answers + (definition.type to answer))
  }
}
