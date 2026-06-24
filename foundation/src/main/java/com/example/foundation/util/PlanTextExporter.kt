package com.example.foundation.util

import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.model.PlanCardType

object PlanTextExporter {
  private val morningSlots = setOf("早上")
  private val afternoonSlots = setOf("下午")
  private val eveningSlots = setOf("晚上")

  fun buildSummary(date: String, answers: List<PlanCardAnswer>): String {
    val answerMap = answers.associateBy { it.cardType }
    val lines = mutableListOf<String>()
    lines += "【${DateTimeUtil.formatDate(date)} 日程规划】"
    lines += ""

    appendSection(lines, "早晨", buildMorningLines(answerMap))
    appendSection(lines, "下午", buildAfternoonLines(answerMap))
    appendSection(lines, "晚上", buildEveningLines(answerMap))
    appendSection(lines, "其他", buildOtherLines(answerMap))

    return lines.joinToString("\n").trimEnd()
  }

  private fun appendSection(lines: MutableList<String>, title: String, sectionLines: List<String>) {
    if (sectionLines.isEmpty()) return
    lines += "— $title —"
    lines.addAll(sectionLines)
    lines += ""
  }

  private fun buildMorningLines(answers: Map<PlanCardType, PlanCardAnswer>): List<String> {
    val lines = mutableListOf<String>()
    appendSlot(lines, "出门", answers[PlanCardType.GO_OUT], morningSlots)
    appendSlot(lines, "学习/工作", answers[PlanCardType.WORK], morningSlots)
    appendSlot(lines, "健身", answers[PlanCardType.FITNESS], morningSlots)
    appendMeal(lines, "早餐", answers[PlanCardType.BREAKFAST])
    return lines
  }

  private fun buildAfternoonLines(answers: Map<PlanCardType, PlanCardAnswer>): List<String> {
    val lines = mutableListOf<String>()
    appendSlot(lines, "出门", answers[PlanCardType.GO_OUT], afternoonSlots)
    appendSlot(lines, "学习/工作", answers[PlanCardType.WORK], afternoonSlots)
    appendSlot(lines, "健身", answers[PlanCardType.FITNESS], afternoonSlots)
    appendMeal(lines, "午餐", answers[PlanCardType.LUNCH])
    return lines
  }

  private fun buildEveningLines(answers: Map<PlanCardType, PlanCardAnswer>): List<String> {
    val lines = mutableListOf<String>()
    appendSlot(lines, "出门", answers[PlanCardType.GO_OUT], eveningSlots)
    appendSlot(lines, "学习/工作", answers[PlanCardType.WORK], eveningSlots)
    appendSlot(lines, "健身", answers[PlanCardType.FITNESS], eveningSlots)
    appendMeal(lines, "晚餐", answers[PlanCardType.DINNER])
    answers[PlanCardType.RETURN_HOME]?.timeValue?.let { time ->
      lines += "预计归家：$time"
    }
    return lines
  }

  private fun buildOtherLines(answers: Map<PlanCardType, PlanCardAnswer>): List<String> {
    val other = answers[PlanCardType.OTHER] ?: return emptyList()
    val lines = mutableListOf<String>()
    other.noteText?.takeIf { it.isNotBlank() }?.let { lines += it }
    other.extraNotes.filter { it.isNotBlank() }.forEach { lines += it }
    return lines
  }

  private fun appendSlot(
    lines: MutableList<String>,
    label: String,
    answer: PlanCardAnswer?,
    slots: Set<String>,
  ) {
    if (answer == null) return
    val matched = answer.selectedOptions.filter { it in slots }
    if (matched.isEmpty()) return
    val detail = formatSubDetail(answer)
    lines += if (detail != null) {
      "$label（${matched.joinToString("、")}）：$detail"
    } else {
      "$label：${matched.joinToString("、")}"
    }
  }

  private fun appendMeal(lines: MutableList<String>, label: String, answer: PlanCardAnswer?) {
    val option = answer?.selectedOptions?.firstOrNull()?.takeIf { it.isNotBlank() } ?: return
    if (option == "不吃") return
    val detail = formatSubDetail(answer)
    lines += if (detail != null) "$label：$option · $detail" else "$label：$option"
  }

  private fun formatSubDetail(answer: PlanCardAnswer): String? {
    if (answer.selectedOptions.contains("今日练休")) return "今日练休"
    val subs = answer.subSelections.filter { it.isNotBlank() }
    return subs.takeIf { it.isNotEmpty() }?.joinToString("、")
  }
}
