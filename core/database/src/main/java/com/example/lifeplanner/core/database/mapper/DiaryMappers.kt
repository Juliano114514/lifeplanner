package com.example.lifeplanner.core.database.mapper

import com.example.lifeplanner.core.database.entity.DiaryEntryEntity
import com.example.lifeplanner.core.domain.model.DiaryEntry
import com.example.lifeplanner.core.domain.model.DiaryEntryType
import java.time.LocalDate

internal fun DiaryEntryEntity.toDomain(): DiaryEntry = DiaryEntry(
  id = id,
  date = LocalDate.parse(date),
  type = DiaryEntryType.valueOf(type),
  content = content,
  createdAt = createdAt,
  updatedAt = updatedAt,
)
