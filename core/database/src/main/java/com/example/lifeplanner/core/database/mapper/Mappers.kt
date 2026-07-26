package com.example.lifeplanner.core.database.mapper

import com.example.lifeplanner.core.database.entity.FoodDetailsEntity
import com.example.lifeplanner.core.database.entity.QuickPlanAnswerEntity
import com.example.lifeplanner.core.database.entity.QuickPlanDraftEntity
import com.example.lifeplanner.core.database.entity.QuickPlanPeriodEntryEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockRow
import com.example.lifeplanner.core.database.entity.ShoppingEntryEntity
import com.example.lifeplanner.core.database.entity.StockItemEntity
import com.example.lifeplanner.core.database.entity.StockItemWithFood
import com.example.lifeplanner.core.database.entity.TaskEntity
import com.example.lifeplanner.core.database.entity.TaskOccurrenceEntity
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.QuickPlanEntryRef
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodEntry
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.RecurrenceRule
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.ShoppingEntry
import com.example.lifeplanner.core.domain.model.ShoppingSource
import com.example.lifeplanner.core.domain.model.ShoppingStatus
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.StorageLocation
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TrackingMode
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId

private const val LIST_SEPARATOR = "\u001F"

internal fun TaskEntity.toDomain(): Task = Task(
  id = id,
  title = title,
  note = note,
  dueAt = dueAt?.toLocalDateTime(),
  isPinned = isPinned,
  recurrence = recurrenceFrequency?.let { RecurrenceRule(RecurrenceFrequency.valueOf(it)) },
  recurrenceStart = LocalDate.parse(recurrenceStart),
  isArchived = isArchived,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

internal fun TaskOccurrenceEntity.toDomain(): TaskOccurrence = TaskOccurrence(
  id = id,
  taskId = taskId,
  plannedDate = LocalDate.parse(plannedDate),
  dueAt = dueAt?.toLocalDateTime(),
  status = OccurrenceStatus.valueOf(status),
  completedAt = completedAt,
)

internal fun ScheduleBlockRow.toDomain(): ScheduleBlock = block.toDomain(
  taskStatus?.let(OccurrenceStatus::valueOf),
  taskCompletedAt,
)

internal fun ScheduleBlockEntity.toDomain(
  taskStatus: OccurrenceStatus? = null,
  taskCompletedAt: Long? = null,
): ScheduleBlock =
  ScheduleBlock(
    id = id,
    date = LocalDate.parse(date),
    startMinute = startMinute,
    endMinute = endMinute,
    title = title,
    note = note,
    taskOccurrenceId = taskOccurrenceId,
    scheduleStatus = OccurrenceStatus.valueOf(status),
    scheduleCompletedAt = completedAt,
    taskStatus = taskStatus,
    taskCompletedAt = taskCompletedAt,
    source = ScheduleSource.valueOf(source),
    isUserModified = isUserModified,
    isArchived = isArchived,
    quickPlanEntryRef = quickPlanEntryRef(),
  )

internal fun ScheduleBlockEntity.quickPlanEntryRef(): QuickPlanEntryRef? =
  quickPlanCardType?.let { cardType ->
    quickPlanEntryKey?.let { entryKey ->
      QuickPlanEntryRef(QuickPlanCardType.valueOf(cardType), entryKey)
    }
  }

internal fun QuickPlanAnswerEntity.toDomain(
  periodEntries: List<QuickPlanPeriodEntryEntity>,
): QuickPlanAnswer {
  val type = QuickPlanCardType.valueOf(cardType)
  val selected = selectedOptions.decodeList()
  val subSelected = subSelections.decodeList()
  val entries = periodEntries.map(QuickPlanPeriodEntryEntity::toDomain)
    .ifEmpty { legacyPeriodEntries(type, selected, subSelected) }
  return QuickPlanAnswer(
    cardType = type,
    selectedOptions = selected,
    subSelections = subSelected,
    hour = hour,
    note = note,
    extraNotes = extraNotes.decodeList(),
    periodEntries = entries,
  )
}

internal fun QuickPlanDraftEntity.toDomain(
  answers: List<QuickPlanAnswerEntity>,
  periodEntries: List<QuickPlanPeriodEntryEntity>,
): QuickPlanDraft =
  QuickPlanDraft(
    date = LocalDate.parse(date),
    currentIndex = currentIndex,
    answers = answers.associate { answer ->
      val answerEntries = periodEntries.filter {
        it.draftDate == answer.draftDate && it.cardType == answer.cardType
      }
      QuickPlanCardType.valueOf(answer.cardType) to answer.toDomain(answerEntries)
    },
    completedAt = completedAt,
  )

internal fun StockItemWithFood.toDomain(): StockItemDetails = StockItemDetails(
  item = item.toDomain(),
  foodDetails = foodDetails?.toDomain(),
)

internal fun StockItemEntity.toDomain(): StockItem = StockItem(
  id = id,
  name = name,
  category = category,
  kind = StockKind.valueOf(kind),
  unit = unit,
  trackingMode = TrackingMode.valueOf(trackingMode),
  currentAmount = currentAmount,
  currentStatus = currentStatus?.let(StockLevel::valueOf),
  replenishThreshold = replenishThreshold,
  isArchived = isArchived,
  createdAt = createdAt,
  updatedAt = updatedAt,
)

internal fun FoodDetailsEntity.toDomain(): FoodDetails = FoodDetails(
  stockItemId = stockItemId,
  foodKind = FoodKind.valueOf(foodKind),
  storageLocation = StorageLocation.valueOf(storageLocation),
  expiryDate = expiryDate?.let(LocalDate::parse),
  expiryWarningDays = expiryWarningDays,
)

internal fun ShoppingEntryEntity.toDomain(): ShoppingEntry = ShoppingEntry(
  id = id,
  stockItemId = stockItemId,
  name = name,
  unit = unit,
  desiredAmount = desiredAmount,
  source = ShoppingSource.valueOf(source),
  status = ShoppingStatus.valueOf(status),
  createdAt = createdAt,
  purchasedAt = purchasedAt,
)

internal fun QuickPlanAnswer.toEntity(date: LocalDate): QuickPlanAnswerEntity =
  QuickPlanAnswerEntity(
    draftDate = date.toString(),
    cardType = cardType.name,
    selectedOptions = selectedOptions.encodeList(),
    subSelections = subSelections.encodeList(),
    hour = hour,
    note = note,
    extraNotes = extraNotes.encodeList(),
  )

internal fun QuickPlanPeriodEntry.toEntity(
  date: LocalDate,
  cardType: QuickPlanCardType,
): QuickPlanPeriodEntryEntity = QuickPlanPeriodEntryEntity(
  draftDate = date.toString(),
  cardType = cardType.name,
  period = period.name,
  isIncluded = isIncluded,
  tag = tag,
  customText = customText,
  location = location,
)

private fun QuickPlanPeriodEntryEntity.toDomain(): QuickPlanPeriodEntry =
  QuickPlanPeriodEntry(
    period = DayPeriod.valueOf(period),
    isIncluded = isIncluded,
    tag = tag,
    customText = customText,
    location = location,
  )

private fun legacyPeriodEntries(
  type: QuickPlanCardType,
  selected: List<String>,
  subSelected: List<String>,
): List<QuickPlanPeriodEntry> {
  if (type != QuickPlanCardType.WORK && type != QuickPlanCardType.GO_OUT) return emptyList()
  return selected.mapNotNull { label ->
    val period = when (label) {
      "早上" -> DayPeriod.MORNING
      "下午" -> DayPeriod.AFTERNOON
      "晚上" -> DayPeriod.EVENING
      else -> null
    } ?: return@mapNotNull null
    QuickPlanPeriodEntry(
      period = period,
      tag = if (type == QuickPlanCardType.GO_OUT) subSelected.firstOrNull().orEmpty() else "",
      customText = if (type == QuickPlanCardType.WORK) "学习 / 工作" else "出门",
      location = if (type == QuickPlanCardType.WORK) subSelected.firstOrNull().orEmpty() else "",
    )
  }
}

private fun Long.toLocalDateTime(): LocalDateTime =
  LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())

internal fun LocalDateTime.toEpochMillis(): Long =
  atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()

private fun List<String>.encodeList(): String = joinToString(LIST_SEPARATOR)
private fun String.decodeList(): List<String> = if (isBlank()) emptyList() else split(LIST_SEPARATOR)
