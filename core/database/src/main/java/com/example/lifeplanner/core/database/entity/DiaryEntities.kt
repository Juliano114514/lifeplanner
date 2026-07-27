package com.example.lifeplanner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "diary_entry",
  indices = [
    Index(value = ["date", "type", "created_at"]),
  ],
)
data class DiaryEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String,
  val type: String,
  val content: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(tableName = "diary_day")
data class DiaryDayEntity(
  @PrimaryKey val date: String,
  val content: String,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
)
