package com.example.lifeplanner.core.database.repository

import com.example.lifeplanner.core.database.AppDatabase
import com.example.lifeplanner.core.database.dao.DiaryDao
import com.example.lifeplanner.core.database.entity.DiaryDayEntity
import com.example.lifeplanner.core.database.entity.DiaryEntryEntity
import com.example.lifeplanner.core.database.mapper.toDomain
import com.example.lifeplanner.core.domain.model.DiaryDay
import com.example.lifeplanner.core.domain.model.DiaryEntry
import com.example.lifeplanner.core.domain.model.DiaryEntryDraft
import com.example.lifeplanner.core.domain.repository.DiaryRepository
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine

class DiaryRepositoryImpl(
  database: AppDatabase,
  private val dao: DiaryDao = database.diaryDao(),
) : DiaryRepository {
  override fun observeDay(date: LocalDate): Flow<DiaryDay> {
    val storedDate = date.toString()
    return combine(
      dao.observeEntries(storedDate),
      dao.observeDay(storedDate),
    ) { entries, day ->
      DiaryDay(
        date = date,
        entries = entries.map { it.toDomain() },
        text = day?.content.orEmpty(),
      )
    }
  }

  override suspend fun getEntry(id: Long): DiaryEntry? = dao.getEntry(id)?.toDomain()

  override suspend fun saveEntry(draft: DiaryEntryDraft): Long {
    val content = draft.content.trim()
    require(content.isNotEmpty()) { "条目内容不能为空" }
    val now = System.currentTimeMillis()
    val existing = draft.id?.let { id ->
      requireNotNull(dao.getEntry(id)) { "日记条目不存在" }
    }
    val entity = DiaryEntryEntity(
      id = existing?.id ?: 0,
      date = draft.date.toString(),
      type = draft.type.name,
      content = content,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )
    return if (existing == null) {
      dao.insertEntry(entity)
    } else {
      dao.updateEntry(entity)
      entity.id
    }
  }

  override suspend fun deleteEntry(id: Long) {
    dao.deleteEntry(id)
  }

  override suspend fun saveDayText(date: LocalDate, text: String) {
    if (text.isBlank()) {
      dao.deleteDay(date.toString())
    } else {
      dao.upsertDay(
        DiaryDayEntity(
          date = date.toString(),
          content = text,
          updatedAt = System.currentTimeMillis(),
        ),
      )
    }
  }
}
