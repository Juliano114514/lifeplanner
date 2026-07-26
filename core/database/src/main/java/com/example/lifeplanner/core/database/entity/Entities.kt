package com.example.lifeplanner.core.database.entity

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import androidx.room.Relation

@Entity(tableName = "task")
data class TaskEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val title: String,
  val note: String,
  @ColumnInfo(name = "due_at") val dueAt: Long?,
  @ColumnInfo(name = "is_pinned") val isPinned: Boolean,
  @ColumnInfo(name = "recurrence_frequency") val recurrenceFrequency: String?,
  @ColumnInfo(name = "recurrence_start") val recurrenceStart: String,
  @ColumnInfo(name = "is_archived") val isArchived: Boolean,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
  tableName = "task_occurrence",
  foreignKeys = [
    ForeignKey(
      entity = TaskEntity::class,
      parentColumns = ["id"],
      childColumns = ["task_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [
    Index("task_id"),
    Index(value = ["task_id", "planned_date"], unique = true),
  ],
)
data class TaskOccurrenceEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "task_id") val taskId: Long,
  @ColumnInfo(name = "planned_date") val plannedDate: String,
  @ColumnInfo(name = "due_at") val dueAt: Long?,
  val status: String,
  @ColumnInfo(name = "completed_at") val completedAt: Long?,
)

@Entity(
  tableName = "schedule_block",
  foreignKeys = [
    ForeignKey(
      entity = TaskOccurrenceEntity::class,
      parentColumns = ["id"],
      childColumns = ["task_occurrence_id"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [
    Index("date"),
    Index("task_occurrence_id"),
    Index(value = ["date", "quick_plan_card_type", "quick_plan_entry_key"]),
  ],
)
data class ScheduleBlockEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String,
  @ColumnInfo(name = "start_minute") val startMinute: Int,
  @ColumnInfo(name = "end_minute") val endMinute: Int,
  val title: String,
  val note: String,
  @ColumnInfo(name = "task_occurrence_id") val taskOccurrenceId: Long?,
  val source: String,
  @ColumnInfo(name = "is_user_modified") val isUserModified: Boolean,
  @ColumnInfo(name = "is_archived") val isArchived: Boolean,
  @ColumnInfo(name = "quick_plan_card_type") val quickPlanCardType: String? = null,
  @ColumnInfo(name = "quick_plan_entry_key") val quickPlanEntryKey: String? = null,
)

data class ScheduleBlockRow(
  @Embedded val block: ScheduleBlockEntity,
  @ColumnInfo(name = "task_status") val taskStatus: String?,
)

@Entity(tableName = "quick_plan_draft")
data class QuickPlanDraftEntity(
  @PrimaryKey val date: String,
  @ColumnInfo(name = "current_index") val currentIndex: Int,
  @ColumnInfo(name = "completed_at") val completedAt: Long?,
)

@Entity(
  tableName = "quick_plan_answer",
  primaryKeys = ["draft_date", "card_type"],
  foreignKeys = [
    ForeignKey(
      entity = QuickPlanDraftEntity::class,
      parentColumns = ["date"],
      childColumns = ["draft_date"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("draft_date")],
)
data class QuickPlanAnswerEntity(
  @ColumnInfo(name = "draft_date") val draftDate: String,
  @ColumnInfo(name = "card_type") val cardType: String,
  @ColumnInfo(name = "selected_options") val selectedOptions: String,
  @ColumnInfo(name = "sub_selections") val subSelections: String,
  val hour: Int?,
  val note: String,
  @ColumnInfo(name = "extra_notes") val extraNotes: String,
)

@Entity(
  tableName = "quick_plan_period_entry",
  primaryKeys = ["draft_date", "card_type", "period"],
  foreignKeys = [
    ForeignKey(
      entity = QuickPlanAnswerEntity::class,
      parentColumns = ["draft_date", "card_type"],
      childColumns = ["draft_date", "card_type"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["draft_date", "card_type"])],
)
data class QuickPlanPeriodEntryEntity(
  @ColumnInfo(name = "draft_date") val draftDate: String,
  @ColumnInfo(name = "card_type") val cardType: String,
  val period: String,
  @ColumnInfo(name = "is_included", defaultValue = "1") val isIncluded: Boolean = true,
  val tag: String,
  @ColumnInfo(name = "custom_text") val customText: String,
  val location: String,
)

@Entity(tableName = "stock_item", indices = [Index("kind"), Index("is_archived")])
data class StockItemEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val name: String,
  val category: String,
  val kind: String,
  val unit: String,
  @ColumnInfo(name = "tracking_mode") val trackingMode: String,
  @ColumnInfo(name = "current_amount") val currentAmount: Double?,
  @ColumnInfo(name = "current_status") val currentStatus: String?,
  @ColumnInfo(name = "replenish_threshold") val replenishThreshold: Double?,
  @ColumnInfo(name = "is_archived") val isArchived: Boolean,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "updated_at") val updatedAt: Long,
)

@Entity(
  tableName = "food_details",
  foreignKeys = [
    ForeignKey(
      entity = StockItemEntity::class,
      parentColumns = ["id"],
      childColumns = ["stock_item_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
)
data class FoodDetailsEntity(
  @PrimaryKey
  @ColumnInfo(name = "stock_item_id")
  val stockItemId: Long,
  @ColumnInfo(name = "food_kind") val foodKind: String,
  @ColumnInfo(name = "storage_location") val storageLocation: String,
  @ColumnInfo(name = "expiry_date") val expiryDate: String?,
  @ColumnInfo(name = "expiry_warning_days") val expiryWarningDays: Int,
)

data class StockItemWithFood(
  @Embedded val item: StockItemEntity,
  @Relation(parentColumn = "id", entityColumn = "stock_item_id")
  val foodDetails: FoodDetailsEntity?,
)

@Entity(
  tableName = "stock_snapshot",
  foreignKeys = [
    ForeignKey(
      entity = StockItemEntity::class,
      parentColumns = ["id"],
      childColumns = ["stock_item_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index("stock_item_id")],
)
data class StockSnapshotEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "stock_item_id") val stockItemId: Long,
  val amount: Double?,
  val status: String?,
  @ColumnInfo(name = "recorded_at") val recordedAt: Long,
)

@Entity(
  tableName = "shopping_entry",
  foreignKeys = [
    ForeignKey(
      entity = StockItemEntity::class,
      parentColumns = ["id"],
      childColumns = ["stock_item_id"],
      onDelete = ForeignKey.SET_NULL,
    ),
  ],
  indices = [Index("stock_item_id"), Index("status")],
)
data class ShoppingEntryEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  @ColumnInfo(name = "stock_item_id") val stockItemId: Long?,
  val name: String,
  val unit: String,
  @ColumnInfo(name = "desired_amount") val desiredAmount: Double?,
  val source: String,
  val status: String,
  @ColumnInfo(name = "created_at") val createdAt: Long,
  @ColumnInfo(name = "purchased_at") val purchasedAt: Long?,
)
