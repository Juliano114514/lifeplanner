package com.example.lifeplanner.core.domain.quickplan

import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardDefinition
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodEntry
import java.time.LocalDate

object QuickPlanReducer {
  fun newDraft(date: LocalDate): QuickPlanDraft = QuickPlanDraft(date = date)

  fun toggleOption(draft: QuickPlanDraft, label: String): QuickPlanDraft {
    val definition = currentDefinition(draft)
    val current = answerFor(draft, definition.type)
    val selected = current.selectedOptions.toMutableList()
    if (label == definition.exclusiveOption) {
      selected.clear()
      selected += label
    } else {
      selected.remove(definition.exclusiveOption)
      if (!selected.remove(label)) selected += label
    }
    return putAnswer(draft, sanitize(definition, current.copy(selectedOptions = selected)))
  }

  fun selectSingle(draft: QuickPlanDraft, label: String): QuickPlanDraft {
    val definition = currentDefinition(draft)
    val answer = answerFor(draft, definition.type).copy(selectedOptions = listOf(label))
    return putAnswer(draft, sanitize(definition, answer))
  }

  fun toggleFollowUp(draft: QuickPlanDraft, label: String): QuickPlanDraft {
    val definition = currentDefinition(draft)
    val config = definition.activeFollowUp(answerFor(draft, definition.type).selectedOptions)
      ?: return draft
    val current = answerFor(draft, definition.type)
    val values = if (config.enableMultiSelect) {
      current.subSelections.toMutableList().apply {
        if (!remove(label)) add(label)
      }
    } else {
      listOf(label)
    }
    return putAnswer(draft, current.copy(subSelections = values))
  }

  fun setHour(draft: QuickPlanDraft, hour: Int): QuickPlanDraft =
    putAnswer(draft, answerFor(draft, currentDefinition(draft).type).copy(hour = hour.coerceIn(0, 23)))

  fun setNote(draft: QuickPlanDraft, note: String): QuickPlanDraft =
    putAnswer(draft, answerFor(draft, currentDefinition(draft).type).copy(note = note))

  fun setPeriodTag(draft: QuickPlanDraft, period: DayPeriod, tag: String): QuickPlanDraft =
    updatePeriod(draft, period) { current ->
      val nextTag = tag.takeUnless { it == current.tag }.orEmpty()
      current.copy(
        tag = nextTag,
        location = current.location.takeUnless { nextTag == "休息" }.orEmpty(),
      )
    }

  fun setPeriodText(draft: QuickPlanDraft, period: DayPeriod, text: String): QuickPlanDraft =
    updatePeriod(draft, period) { it.copy(customText = text) }

  fun setPeriodLocation(draft: QuickPlanDraft, period: DayPeriod, location: String): QuickPlanDraft =
    updatePeriod(draft, period) { current ->
      if (current.tag == "休息") current
      else current.copy(location = location.takeUnless { it == current.location }.orEmpty())
    }

  fun clearPeriod(draft: QuickPlanDraft, period: DayPeriod): QuickPlanDraft {
    val definition = currentDefinition(draft)
    val answer = answerFor(draft, definition.type)
    return putAnswer(
      draft,
      answer.copy(periodEntries = answer.periodEntries.filterNot { it.period == period }),
    )
  }

  fun next(draft: QuickPlanDraft): QuickPlanDraft =
    draft.copy(currentIndex = (draft.currentIndex + 1).coerceAtMost(QuickPlanCatalog.cards.lastIndex))

  fun previous(draft: QuickPlanDraft): QuickPlanDraft =
    draft.copy(currentIndex = (draft.currentIndex - 1).coerceAtLeast(0))

  fun skip(draft: QuickPlanDraft): QuickPlanDraft {
    val type = currentDefinition(draft).type
    return next(draft.copy(answers = draft.answers - type))
  }

  private fun sanitize(
    definition: QuickPlanCardDefinition,
    answer: QuickPlanAnswer,
  ): QuickPlanAnswer {
    return if (definition.activeFollowUp(answer.selectedOptions) == null) {
      answer.copy(subSelections = emptyList())
    } else {
      answer
    }
  }

  private fun currentDefinition(draft: QuickPlanDraft): QuickPlanCardDefinition =
    QuickPlanCatalog.cards[draft.currentIndex.coerceIn(0, QuickPlanCatalog.cards.lastIndex)]

  private fun answerFor(draft: QuickPlanDraft, type: QuickPlanCardType): QuickPlanAnswer =
    draft.answers[type] ?: QuickPlanAnswer(type)

  private fun updatePeriod(
    draft: QuickPlanDraft,
    period: DayPeriod,
    update: (QuickPlanPeriodEntry) -> QuickPlanPeriodEntry,
  ): QuickPlanDraft {
    val definition = currentDefinition(draft)
    val answer = answerFor(draft, definition.type)
    val current = answer.periodEntries.firstOrNull { it.period == period }
      ?: QuickPlanPeriodEntry(period)
    val entries = answer.periodEntries.filterNot { it.period == period } + update(current)
    return putAnswer(draft, answer.copy(periodEntries = entries.sortedBy { it.period.ordinal }))
  }

  private fun putAnswer(draft: QuickPlanDraft, answer: QuickPlanAnswer): QuickPlanDraft =
    draft.copy(answers = draft.answers + (answer.cardType to answer))
}
