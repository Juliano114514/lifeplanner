package com.example.lifeplanner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import androidx.room.Upsert
import com.example.lifeplanner.core.database.entity.DiaryDayEntity
import com.example.lifeplanner.core.database.entity.DiaryEntryEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface DiaryDao {
  @Query(
    """
    SELECT * FROM diary_entry
    WHERE date = :date
    ORDER BY created_at, id
    """,
  )
  fun observeEntries(date: String): Flow<List<DiaryEntryEntity>>

  @Query("SELECT * FROM diary_day WHERE date = :date")
  fun observeDay(date: String): Flow<DiaryDayEntity?>

  @Query(
    """
    SELECT date FROM diary_entry
    UNION
    SELECT date FROM diary_day
    """,
  )
  fun observeRecordedDates(): Flow<List<String>>

  @Query("SELECT * FROM diary_entry WHERE id = :id")
  suspend fun getEntry(id: Long): DiaryEntryEntity?

  @Insert
  suspend fun insertEntry(entity: DiaryEntryEntity): Long

  @Update
  suspend fun updateEntry(entity: DiaryEntryEntity)

  @Query("DELETE FROM diary_entry WHERE id = :id")
  suspend fun deleteEntry(id: Long)

  @Upsert
  suspend fun upsertDay(entity: DiaryDayEntity)

  @Query("DELETE FROM diary_day WHERE date = :date")
  suspend fun deleteDay(date: String)
}
