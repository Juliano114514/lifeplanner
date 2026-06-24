package com.example.libroom.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.libroom.local.entity.PlanCardAnswerEntity
import com.example.libroom.local.entity.PlanRecordEntity
import com.example.libroom.local.entity.PlanRecordWithAnswers
import kotlinx.coroutines.flow.Flow

@Dao
interface PlanDao {
  @Transaction
  @Query(
    """
    SELECT * FROM plan_record
    WHERE date = :date AND completed_at IS NULL
    ORDER BY created_at DESC
    LIMIT 1
    """,
  )
  fun observeIncompleteByDate(date: String): Flow<PlanRecordWithAnswers?>

  @Insert
  suspend fun insertRecord(entity: PlanRecordEntity): Long

  @Update
  suspend fun updateRecord(entity: PlanRecordEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAnswers(answers: List<PlanCardAnswerEntity>)

  @Transaction
  @Query("SELECT * FROM plan_record WHERE id = :id LIMIT 1")
  suspend fun getRecordWithAnswers(id: Long): PlanRecordWithAnswers?

  @Query("DELETE FROM plan_card_answer WHERE plan_record_id = :planId AND card_type = :cardType")
  suspend fun deleteAnswer(planId: Long, cardType: String)
}
