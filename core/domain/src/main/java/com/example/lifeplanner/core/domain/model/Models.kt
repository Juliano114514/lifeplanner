package com.example.lifeplanner.core.domain.model

import java.time.LocalDate
import java.time.LocalDateTime

enum class RecurrenceFrequency { DAILY, WEEKLY, MONTHLY }

data class RecurrenceRule(
  val frequency: RecurrenceFrequency,
)

data class Task(
  val id: Long = 0,
  val title: String,
  val note: String = "",
  val dueAt: LocalDateTime? = null,
  val isPinned: Boolean = false,
  val recurrence: RecurrenceRule? = null,
  val recurrenceStart: LocalDate,
  val isArchived: Boolean = false,
  val createdAt: Long,
  val updatedAt: Long,
)

enum class OccurrenceStatus { PENDING, COMPLETED, SKIPPED }

abstract class DailyPlanItem {
  abstract val id: Long
  abstract val title: String
  abstract val note: String
  abstract val plannedDate: LocalDate
  abstract val completionStatus: OccurrenceStatus
  abstract val completedAt: Long?
}

data class TaskOccurrence(
  val id: Long = 0,
  val taskId: Long,
  val plannedDate: LocalDate,
  val dueAt: LocalDateTime? = null,
  val status: OccurrenceStatus = OccurrenceStatus.PENDING,
  val completedAt: Long? = null,
)

data class TodoItem(
  val task: Task,
  val occurrence: TaskOccurrence?,
) : DailyPlanItem() {
  override val id: Long
    get() = occurrence?.id ?: task.id
  override val title: String
    get() = task.title
  override val note: String
    get() = task.note
  override val plannedDate: LocalDate
    get() = occurrence?.plannedDate ?: task.recurrenceStart
  override val completionStatus: OccurrenceStatus
    get() = occurrence?.status ?: OccurrenceStatus.PENDING
  override val completedAt: Long?
    get() = occurrence?.completedAt
}

data class TodoOverview(
  val urgent: List<TodoItem> = emptyList(),
  val todayPending: List<TodoItem> = emptyList(),
  val todayCompleted: List<TodoItem> = emptyList(),
  val others: List<TodoItem> = emptyList(),
)

enum class ScheduleSource { MANUAL, QUICK_PLAN }

data class QuickPlanEntryRef(
  val cardType: QuickPlanCardType,
  val slotKey: String,
)

data class ScheduleBlock(
  override val id: Long = 0,
  val date: LocalDate,
  val startMinute: Int,
  val endMinute: Int,
  override val title: String,
  override val note: String = "",
  val taskOccurrenceId: Long? = null,
  val scheduleStatus: OccurrenceStatus = OccurrenceStatus.PENDING,
  val scheduleCompletedAt: Long? = null,
  val taskStatus: OccurrenceStatus? = null,
  val taskCompletedAt: Long? = null,
  val source: ScheduleSource = ScheduleSource.MANUAL,
  val isUserModified: Boolean = false,
  val isArchived: Boolean = false,
  val quickPlanEntryRef: QuickPlanEntryRef? = null,
) : DailyPlanItem() {
  override val plannedDate: LocalDate
    get() = date
  override val completionStatus: OccurrenceStatus
    get() = taskStatus ?: scheduleStatus
  override val completedAt: Long?
    get() = taskCompletedAt ?: scheduleCompletedAt
}

data class DaySchedule(
  val date: LocalDate,
  val blocks: List<ScheduleBlock> = emptyList(),
  val conflictingBlockIds: Set<Long> = emptySet(),
)

enum class QuickPlanCardType {
  WORK,
  GO_OUT,
  BREAKFAST,
  LUNCH,
  DINNER,
  RETURN_HOME,
  FITNESS,
  OTHER,
}

enum class DayPeriod { MORNING, AFTERNOON, EVENING }

data class QuickPlanPeriodEntry(
  val period: DayPeriod,
  val isIncluded: Boolean = true,
  val tag: String = "",
  val customText: String = "",
  val location: String = "",
) {
  val hasActivity: Boolean
    get() = tag.isNotBlank() || customText.isNotBlank()
}

data class QuickPlanPeriodConfig(
  val activityOptions: List<String>,
  val locationOptions: List<String> = emptyList(),
)

enum class QuickPlanInteraction { MULTI_TAG, SINGLE_TAG, PERIOD_PLAN, HOUR_TIME, TODO_CREATE }

data class QuickPlanFollowUp(
  val title: String,
  val options: List<String>,
  val triggers: Set<String> = emptySet(),
  val skip: Set<String> = emptySet(),
  val enableMultiSelect: Boolean = false,
)

data class QuickPlanCardDefinition(
  val type: QuickPlanCardType,
  val title: String,
  val interaction: QuickPlanInteraction,
  val options: List<String> = emptyList(),
  val exclusiveOption: String? = null,
  val followUp: QuickPlanFollowUp? = null,
  val periodConfig: QuickPlanPeriodConfig? = null,
) {
  fun activeFollowUp(selected: List<String>): QuickPlanFollowUp? {
    val config = followUp ?: return null
    return config.takeIf {
      selected.none(config.skip::contains) &&
        (config.triggers.isEmpty() || selected.any(config.triggers::contains))
    }
  }
}

data class QuickPlanAnswer(
  val cardType: QuickPlanCardType,
  val selectedOptions: List<String> = emptyList(),
  val subSelections: List<String> = emptyList(),
  val hour: Int? = null,
  val note: String = "",
  val extraNotes: List<String> = emptyList(),
  val periodEntries: List<QuickPlanPeriodEntry> = emptyList(),
)

data class QuickPlanDraft(
  val date: LocalDate,
  val currentIndex: Int = 0,
  val answers: Map<QuickPlanCardType, QuickPlanAnswer> = emptyMap(),
  val completedAt: Long? = null,
)

enum class StockKind { FOOD, HOUSEHOLD }

enum class TrackingMode { QUANTITY, PERCENT, STATUS }

enum class StockLevel { MISSING, LOW, ENOUGH, EXCESS }

data class StockItem(
  val id: Long = 0,
  val name: String,
  val category: String,
  val kind: StockKind,
  val unit: String = "",
  val trackingMode: TrackingMode,
  val currentAmount: Double? = null,
  val currentStatus: StockLevel? = null,
  val replenishThreshold: Double? = null,
  val isArchived: Boolean = false,
  val createdAt: Long,
  val updatedAt: Long,
)

enum class FoodKind { PREPARED, INGREDIENT }

enum class StorageLocation { REFRIGERATED, FROZEN, ROOM_TEMPERATURE, OTHER }

data class FoodDetails(
  val stockItemId: Long,
  val foodKind: FoodKind,
  val storageLocation: StorageLocation,
  val expiryDate: LocalDate? = null,
  val expiryWarningDays: Int = 3,
)

data class StockItemDetails(
  val item: StockItem,
  val foodDetails: FoodDetails? = null,
)

data class StockSnapshot(
  val id: Long = 0,
  val stockItemId: Long,
  val amount: Double? = null,
  val status: StockLevel? = null,
  val recordedAt: Long,
)

enum class ShoppingSource { AUTO, MANUAL }

enum class ShoppingStatus { ACTIVE, PURCHASED, DISMISSED }

data class ShoppingEntry(
  val id: Long = 0,
  val stockItemId: Long? = null,
  val name: String,
  val unit: String = "",
  val desiredAmount: Double? = null,
  val source: ShoppingSource,
  val status: ShoppingStatus = ShoppingStatus.ACTIVE,
  val createdAt: Long,
  val purchasedAt: Long? = null,
)

data class TaskDraft(
  val id: Long? = null,
  val title: String,
  val note: String = "",
  val dueAt: LocalDateTime? = null,
  val isPinned: Boolean = false,
  val recurrence: RecurrenceRule? = null,
  val recurrenceStart: LocalDate,
)

data class ScheduleBlockDraft(
  val id: Long? = null,
  val date: LocalDate,
  val startMinute: Int,
  val endMinute: Int,
  val title: String,
  val note: String = "",
  val taskOccurrenceId: Long? = null,
)

data class StockItemDraft(
  val id: Long? = null,
  val name: String,
  val category: String,
  val kind: StockKind,
  val unit: String,
  val trackingMode: TrackingMode,
  val currentAmount: Double?,
  val currentStatus: StockLevel?,
  val replenishThreshold: Double?,
  val foodDetails: FoodDetails? = null,
)
