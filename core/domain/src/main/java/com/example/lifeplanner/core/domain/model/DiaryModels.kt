package com.example.lifeplanner.core.domain.model

import java.time.LocalDate

enum class DiaryEntryType {
  HAPPY,
  UNHAPPY,
}

data class DiaryEntry(
  val id: Long = 0,
  val date: LocalDate,
  val type: DiaryEntryType,
  val content: String,
  val createdAt: Long,
  val updatedAt: Long,
)

data class DiaryDay(
  val date: LocalDate,
  val entries: List<DiaryEntry> = emptyList(),
  val text: String = "",
) {
  fun entriesOf(type: DiaryEntryType): List<DiaryEntry> = entries.filter { it.type == type }
}

data class DiaryEntryDraft(
  val id: Long? = null,
  val date: LocalDate,
  val type: DiaryEntryType,
  val content: String,
)

data class DiaryDayDraft(
  val date: LocalDate,
  val entries: List<DiaryEntryDraft>,
  val text: String,
)
