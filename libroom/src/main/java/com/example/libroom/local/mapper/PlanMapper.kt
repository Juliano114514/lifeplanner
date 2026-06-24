package com.example.libroom.local.mapper

import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.model.PlanCardType
import com.example.foundation.domain.model.PlanRecord
import com.example.libroom.local.entity.PlanCardAnswerEntity
import com.example.libroom.local.entity.PlanRecordEntity
import com.example.libroom.local.entity.PlanRecordWithAnswers
import org.json.JSONArray

object PlanMapper {
  fun toDomain(entity: PlanRecordWithAnswers): PlanRecord {
    return PlanRecord(
      id = entity.record.id,
      date = entity.record.date,
      createdAt = entity.record.created_at,
      completedAt = entity.record.completed_at,
      currentIndex = entity.record.current_index,
      exportText = entity.record.export_text,
      answers = entity.answers.map(::toDomainAnswer),
    )
  }

  fun toRecordEntity(record: PlanRecord): PlanRecordEntity {
    return PlanRecordEntity(
      id = record.id,
      date = record.date,
      created_at = record.createdAt,
      completed_at = record.completedAt,
      current_index = record.currentIndex,
      export_text = record.exportText,
    )
  }

  fun toAnswerEntity(planId: Long, answer: PlanCardAnswer): PlanCardAnswerEntity {
    return PlanCardAnswerEntity(
      plan_record_id = planId,
      card_type = answer.cardType.name,
      card_index = answer.cardIndex,
      selected_options = encodeList(answer.selectedOptions),
      sub_selection = encodeSubSelections(answer.subSelections),
      time_value = answer.timeValue,
      note_text = answer.noteText,
      extra_notes = encodeList(answer.extraNotes).takeIf { answer.extraNotes.isNotEmpty() },
    )
  }

  fun toDomainAnswer(entity: PlanCardAnswerEntity): PlanCardAnswer {
    return PlanCardAnswer(
      cardType = PlanCardType.valueOf(entity.card_type),
      cardIndex = entity.card_index,
      selectedOptions = decodeList(entity.selected_options),
      subSelections = decodeSubSelections(entity.sub_selection),
      timeValue = entity.time_value,
      noteText = entity.note_text,
      extraNotes = entity.extra_notes?.let(::decodeList).orEmpty(),
    )
  }

  private fun encodeSubSelections(values: List<String>): String? {
    if (values.isEmpty()) return null
    return encodeList(values)
  }

  private fun decodeSubSelections(raw: String?): List<String> {
    if (raw.isNullOrBlank()) return emptyList()
    if (raw.trimStart().startsWith("[")) return decodeList(raw)
    return listOf(raw)
  }

  private fun encodeList(values: List<String>): String {
    val array = JSONArray()
    values.forEach { array.put(it) }
    return array.toString()
  }

  private fun decodeList(raw: String): List<String> {
    if (raw.isBlank()) return emptyList()
    val array = JSONArray(raw)
    return buildList {
      for (index in 0 until array.length()) {
        add(array.optString(index))
      }
    }
  }
}
