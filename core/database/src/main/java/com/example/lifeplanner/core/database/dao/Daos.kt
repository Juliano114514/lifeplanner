package com.example.lifeplanner.core.database.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.example.lifeplanner.core.database.entity.FoodDetailsEntity
import com.example.lifeplanner.core.database.entity.QuickPlanAnswerEntity
import com.example.lifeplanner.core.database.entity.QuickPlanDraftEntity
import com.example.lifeplanner.core.database.entity.QuickPlanPeriodEntryEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockRow
import com.example.lifeplanner.core.database.entity.ShoppingEntryEntity
import com.example.lifeplanner.core.database.entity.StockItemEntity
import com.example.lifeplanner.core.database.entity.StockItemWithFood
import com.example.lifeplanner.core.database.entity.StockSnapshotEntity
import com.example.lifeplanner.core.database.entity.TaskEntity
import com.example.lifeplanner.core.database.entity.TaskOccurrenceEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
  @Query("SELECT * FROM task WHERE is_archived = 0 ORDER BY created_at DESC")
  fun observeTasks(): Flow<List<TaskEntity>>

  @Query("SELECT * FROM task WHERE is_archived = 0 ORDER BY created_at DESC")
  suspend fun getActiveTasks(): List<TaskEntity>

  @Query("SELECT * FROM task WHERE id = :id LIMIT 1")
  suspend fun getTask(id: Long): TaskEntity?

  @Insert
  suspend fun insertTask(entity: TaskEntity): Long

  @Update
  suspend fun updateTask(entity: TaskEntity)

  @Query("UPDATE task SET is_pinned = :pinned, updated_at = :updatedAt WHERE id = :id")
  suspend fun setPinned(id: Long, pinned: Boolean, updatedAt: Long)

  @Query("UPDATE task SET is_archived = 1, updated_at = :updatedAt WHERE id = :id")
  suspend fun archiveTask(id: Long, updatedAt: Long)

  @Query("SELECT * FROM task_occurrence ORDER BY planned_date")
  fun observeOccurrences(): Flow<List<TaskOccurrenceEntity>>

  @Query("SELECT * FROM task_occurrence WHERE planned_date = :date ORDER BY due_at")
  fun observeOccurrencesByDate(date: String): Flow<List<TaskOccurrenceEntity>>

  @Query("SELECT * FROM task_occurrence WHERE id = :id LIMIT 1")
  suspend fun getOccurrence(id: Long): TaskOccurrenceEntity?

  @Insert(onConflict = OnConflictStrategy.IGNORE)
  suspend fun insertOccurrences(entities: List<TaskOccurrenceEntity>)

  @Query(
    "UPDATE task_occurrence SET status = :status, completed_at = :completedAt WHERE id = :id",
  )
  suspend fun setOccurrenceStatus(id: Long, status: String, completedAt: Long?)

  @Query("UPDATE task_occurrence SET planned_date = :date, due_at = :dueAt WHERE id = :id")
  suspend fun moveOccurrence(id: Long, date: String, dueAt: Long?)

  @Query(
    """
    DELETE FROM task_occurrence
    WHERE task_id = :taskId AND planned_date >= :fromDate AND status = 'PENDING'
    """,
  )
  suspend fun deleteFuturePending(taskId: Long, fromDate: String)
}

@Dao
interface ScheduleDao {
  @Query(
    """
    SELECT schedule_block.*, task_occurrence.status AS task_status
    FROM schedule_block
    LEFT JOIN task_occurrence ON task_occurrence.id = schedule_block.task_occurrence_id
    WHERE schedule_block.date = :date AND schedule_block.is_archived = 0
    ORDER BY schedule_block.start_minute, schedule_block.id
    """,
  )
  fun observeDay(date: String): Flow<List<ScheduleBlockRow>>

  @Query("SELECT * FROM schedule_block WHERE id = :id LIMIT 1")
  suspend fun getBlock(id: Long): ScheduleBlockEntity?

  @Query(
    "SELECT * FROM schedule_block WHERE task_occurrence_id = :occurrenceId AND is_archived = 0 LIMIT 1",
  )
  suspend fun getBlockForOccurrence(occurrenceId: Long): ScheduleBlockEntity?

  @Insert
  suspend fun insertBlock(entity: ScheduleBlockEntity): Long

  @Insert
  suspend fun insertBlocks(entities: List<ScheduleBlockEntity>)

  @Update
  suspend fun updateBlock(entity: ScheduleBlockEntity)

  @Query("UPDATE schedule_block SET is_archived = 1 WHERE id = :id")
  suspend fun archiveBlock(id: Long)

  @Query(
    """
    SELECT * FROM schedule_block
    WHERE date = :date AND source = 'QUICK_PLAN' AND is_archived = 0
    ORDER BY id
    """,
  )
  suspend fun getActiveQuickPlanBlocks(date: String): List<ScheduleBlockEntity>
}

@Dao
interface QuickPlanDao {
  @Query("SELECT * FROM quick_plan_draft WHERE date = :date LIMIT 1")
  suspend fun getDraft(date: String): QuickPlanDraftEntity?

  @Query("SELECT * FROM quick_plan_answer WHERE draft_date = :date")
  suspend fun getAnswers(date: String): List<QuickPlanAnswerEntity>

  @Query("SELECT * FROM quick_plan_period_entry WHERE draft_date = :date")
  suspend fun getPeriodEntries(date: String): List<QuickPlanPeriodEntryEntity>

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertDraft(entity: QuickPlanDraftEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertAnswers(entities: List<QuickPlanAnswerEntity>)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertPeriodEntries(entities: List<QuickPlanPeriodEntryEntity>)

  @Query("DELETE FROM quick_plan_answer WHERE draft_date = :date")
  suspend fun deleteAnswers(date: String)
}

@Dao
interface StockDao {
  @Transaction
  @Query("SELECT * FROM stock_item WHERE kind = :kind AND is_archived = 0 ORDER BY category, name")
  fun observeByKind(kind: String): Flow<List<StockItemWithFood>>

  @Transaction
  @Query("SELECT * FROM stock_item WHERE id = :id LIMIT 1")
  suspend fun getWithFood(id: Long): StockItemWithFood?

  @Query("SELECT * FROM stock_item WHERE id = :id LIMIT 1")
  suspend fun getItem(id: Long): StockItemEntity?

  @Insert
  suspend fun insertItem(entity: StockItemEntity): Long

  @Update
  suspend fun updateItem(entity: StockItemEntity)

  @Insert(onConflict = OnConflictStrategy.REPLACE)
  suspend fun upsertFoodDetails(entity: FoodDetailsEntity)

  @Query("DELETE FROM food_details WHERE stock_item_id = :stockItemId")
  suspend fun deleteFoodDetails(stockItemId: Long)

  @Insert
  suspend fun insertSnapshot(entity: StockSnapshotEntity)

  @Query("UPDATE stock_item SET is_archived = 1, updated_at = :updatedAt WHERE id = :id")
  suspend fun archiveItem(id: Long, updatedAt: Long)
}

@Dao
interface ShoppingDao {
  @Query("SELECT * FROM shopping_entry WHERE status = 'ACTIVE' ORDER BY created_at")
  fun observeActive(): Flow<List<ShoppingEntryEntity>>

  @Query("SELECT * FROM shopping_entry WHERE id = :id LIMIT 1")
  suspend fun get(id: Long): ShoppingEntryEntity?

  @Query(
    """
    SELECT * FROM shopping_entry
    WHERE stock_item_id = :stockItemId AND source = 'AUTO' AND status = 'ACTIVE'
    LIMIT 1
    """,
  )
  suspend fun getActiveAuto(stockItemId: Long): ShoppingEntryEntity?

  @Insert
  suspend fun insert(entity: ShoppingEntryEntity): Long

  @Update
  suspend fun update(entity: ShoppingEntryEntity)

  @Query(
    """
    UPDATE shopping_entry SET status = 'DISMISSED'
    WHERE stock_item_id = :stockItemId AND source = 'AUTO' AND status = 'ACTIVE'
    """,
  )
  suspend fun dismissActiveAuto(stockItemId: Long)
}
