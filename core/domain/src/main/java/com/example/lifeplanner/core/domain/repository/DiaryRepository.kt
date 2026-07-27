package com.example.lifeplanner.core.domain.repository

import com.example.lifeplanner.core.domain.model.DiaryDay
import com.example.lifeplanner.core.domain.model.DiaryDayDraft
import com.example.lifeplanner.core.domain.model.DiaryEntry
import com.example.lifeplanner.core.domain.model.DiaryEntryDraft
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface DiaryRepository {
  fun observeDay(date: LocalDate): Flow<DiaryDay>

  fun observeRecordedDates(): Flow<Set<LocalDate>>

  suspend fun getEntry(id: Long): DiaryEntry?

  suspend fun saveEntry(draft: DiaryEntryDraft): Long

  suspend fun deleteEntry(id: Long)

  suspend fun saveDayText(date: LocalDate, text: String)

  suspend fun saveDay(draft: DiaryDayDraft)
}
