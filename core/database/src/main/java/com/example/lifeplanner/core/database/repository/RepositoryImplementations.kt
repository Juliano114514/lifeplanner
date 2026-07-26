package com.example.lifeplanner.core.database.repository

import androidx.room.withTransaction
import com.example.lifeplanner.core.database.AppDatabase
import com.example.lifeplanner.core.database.dao.QuickPlanDao
import com.example.lifeplanner.core.database.dao.ScheduleDao
import com.example.lifeplanner.core.database.dao.ShoppingDao
import com.example.lifeplanner.core.database.dao.StockDao
import com.example.lifeplanner.core.database.dao.TaskDao
import com.example.lifeplanner.core.database.entity.FoodDetailsEntity
import com.example.lifeplanner.core.database.entity.QuickPlanDraftEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockEntity
import com.example.lifeplanner.core.database.entity.ShoppingEntryEntity
import com.example.lifeplanner.core.database.entity.StockItemEntity
import com.example.lifeplanner.core.database.entity.StockSnapshotEntity
import com.example.lifeplanner.core.database.entity.TaskEntity
import com.example.lifeplanner.core.database.entity.TaskOccurrenceEntity
import com.example.lifeplanner.core.database.mapper.toDomain
import com.example.lifeplanner.core.database.mapper.toEntity
import com.example.lifeplanner.core.database.mapper.toEpochMillis
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.ShoppingEntry
import com.example.lifeplanner.core.domain.model.ShoppingSource
import com.example.lifeplanner.core.domain.model.ShoppingStatus
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockItemDraft
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskDraft
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.ShoppingRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import com.example.lifeplanner.core.domain.repository.TaskRepository
import com.example.lifeplanner.core.domain.rules.QuickPlanGenerator
import com.example.lifeplanner.core.domain.rules.RecurrenceGenerator
import com.example.lifeplanner.core.domain.rules.ScheduleRules
import com.example.lifeplanner.core.domain.rules.StockRules
import com.example.lifeplanner.core.domain.rules.TodoOrganizer
import com.example.lifeplanner.core.domain.rules.withTaskDue
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

class TaskRepositoryImpl(
  private val database: AppDatabase,
  private val dao: TaskDao = database.taskDao(),
) : TaskRepository {
  override fun observeTodo(date: LocalDate): Flow<TodoOverview> = flow {
    ensureOccurrences(date.minusMonths(1), date.plusMonths(1))
    emitAll(
      combine(dao.observeTasks(), dao.observeOccurrences()) { tasks, occurrences ->
        TodoOrganizer.organize(tasks.map { it.toDomain() }, occurrences.map { it.toDomain() }, date)
      },
    )
  }

  override fun observeSchedulable(date: LocalDate): Flow<List<TodoItem>> = flow {
    ensureOccurrences(date, date)
    emitAll(
      combine(dao.observeTasks(), dao.observeOccurrencesByDate(date.toString())) { tasks, occurrences ->
        val tasksById = tasks.associateBy { it.id }
        occurrences.mapNotNull { occurrence ->
          tasksById[occurrence.taskId]?.let { TodoItem(it.toDomain(), occurrence.toDomain()) }
        }.filter { it.occurrence?.status == OccurrenceStatus.PENDING }
      },
    )
  }

  override suspend fun getTask(id: Long): Task? = dao.getTask(id)?.toDomain()

  override suspend fun saveTask(draft: TaskDraft): Long = database.withTransaction {
    require(draft.title.isNotBlank()) { "任务标题不能为空" }
    val now = System.currentTimeMillis()
    val existing = draft.id?.let { dao.getTask(it) }
    val entity = TaskEntity(
      id = existing?.id ?: 0,
      title = draft.title.trim(),
      note = draft.note.trim(),
      dueAt = draft.dueAt?.toEpochMillis(),
      isPinned = draft.isPinned,
      recurrenceFrequency = draft.recurrence?.frequency?.name,
      recurrenceStart = draft.recurrenceStart.toString(),
      isArchived = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )
    val id = if (existing == null) dao.insertTask(entity) else {
      dao.updateTask(entity)
      entity.id
    }
    if (existing != null) dao.deleteFuturePending(id, LocalDate.now().toString())
    ensureOccurrencesInternal(draft.recurrenceStart, draft.recurrenceStart.plusMonths(1), id)
    id
  }

  override suspend fun setPinned(taskId: Long, pinned: Boolean) {
    dao.setPinned(taskId, pinned, System.currentTimeMillis())
  }

  override suspend fun setOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus) {
    dao.setOccurrenceStatus(
      occurrenceId,
      status.name,
      if (status == OccurrenceStatus.COMPLETED) System.currentTimeMillis() else null,
    )
  }

  override suspend fun ensureOccurrences(start: LocalDate, endInclusive: LocalDate) {
    database.withTransaction { ensureOccurrencesInternal(start, endInclusive) }
  }

  override suspend fun archiveTask(taskId: Long) {
    dao.archiveTask(taskId, System.currentTimeMillis())
  }

  private suspend fun ensureOccurrencesInternal(
    start: LocalDate,
    endInclusive: LocalDate,
    onlyTaskId: Long? = null,
  ) {
    dao.getActiveTasks().filter { onlyTaskId == null || it.id == onlyTaskId }.forEach { entity ->
      val task = entity.toDomain()
      val occurrences = RecurrenceGenerator.dates(task, start, endInclusive).map { date ->
        TaskOccurrence(
          taskId = task.id,
          plannedDate = date,
          status = OccurrenceStatus.PENDING,
        ).withTaskDue(task).toEntity()
      }
      if (occurrences.isNotEmpty()) dao.insertOccurrences(occurrences)
    }
  }

  private fun TaskOccurrence.toEntity(): TaskOccurrenceEntity = TaskOccurrenceEntity(
    id = id,
    taskId = taskId,
    plannedDate = plannedDate.toString(),
    dueAt = dueAt?.toEpochMillis(),
    status = status.name,
    completedAt = completedAt,
  )
}

class ScheduleRepositoryImpl(
  private val database: AppDatabase,
  private val scheduleDao: ScheduleDao = database.scheduleDao(),
  private val taskDao: TaskDao = database.taskDao(),
  private val quickPlanDao: QuickPlanDao = database.quickPlanDao(),
) : ScheduleRepository {
  override fun observeDay(date: LocalDate): Flow<DaySchedule> =
    scheduleDao.observeDay(date.toString()).map { rows ->
      ScheduleRules.withConflicts(date, rows.map { it.toDomain() })
    }

  override suspend fun getBlock(id: Long): ScheduleBlock? = scheduleDao.getBlock(id)?.toDomain()

  override suspend fun saveBlock(draft: ScheduleBlockDraft): Long {
    ScheduleRules.validate(draft.startMinute, draft.endMinute)
    val existing = draft.id?.let { scheduleDao.getBlock(it) }
    val entity = ScheduleBlockEntity(
      id = existing?.id ?: 0,
      date = draft.date.toString(),
      startMinute = draft.startMinute,
      endMinute = draft.endMinute,
      title = draft.title.trim(),
      note = draft.note.trim(),
      taskOccurrenceId = draft.taskOccurrenceId,
      source = existing?.source ?: ScheduleSource.MANUAL.name,
      isUserModified = existing != null,
      isArchived = false,
    )
    return if (existing == null) scheduleDao.insertBlock(entity) else {
      scheduleDao.updateBlock(entity)
      entity.id
    }
  }

  override suspend fun scheduleTask(
    occurrenceId: Long,
    date: LocalDate,
    startMinute: Int,
    endMinute: Int,
  ): Long = database.withTransaction {
    ScheduleRules.validate(startMinute, endMinute)
    val occurrence = requireNotNull(taskDao.getOccurrence(occurrenceId)) { "任务实例不存在" }
    val task = requireNotNull(taskDao.getTask(occurrence.taskId)) { "任务不存在" }.toDomain()
    val dueAt = task.dueAt?.toLocalTime()?.let { LocalDateTime.of(date, it).toEpochMillis() }
    taskDao.moveOccurrence(occurrenceId, date.toString(), dueAt)
    val existing = scheduleDao.getBlockForOccurrence(occurrenceId)
    val entity = ScheduleBlockEntity(
      id = existing?.id ?: 0,
      date = date.toString(),
      startMinute = startMinute,
      endMinute = endMinute,
      title = task.title,
      note = task.note,
      taskOccurrenceId = occurrenceId,
      source = ScheduleSource.MANUAL.name,
      isUserModified = true,
      isArchived = false,
    )
    if (existing == null) scheduleDao.insertBlock(entity) else {
      scheduleDao.updateBlock(entity)
      entity.id
    }
  }

  override suspend fun archiveBlock(id: Long) = scheduleDao.archiveBlock(id)

  override suspend fun getQuickPlanDraft(date: LocalDate): QuickPlanDraft? {
    val draft = quickPlanDao.getDraft(date.toString()) ?: return null
    return draft.toDomain(quickPlanDao.getAnswers(date.toString()))
  }

  override suspend fun saveQuickPlanDraft(draft: QuickPlanDraft) {
    database.withTransaction {
      quickPlanDao.upsertDraft(
        QuickPlanDraftEntity(draft.date.toString(), draft.currentIndex, draft.completedAt),
      )
      quickPlanDao.deleteAnswers(draft.date.toString())
      val answers = draft.answers.values.map { it.toEntity(draft.date) }
      if (answers.isNotEmpty()) quickPlanDao.upsertAnswers(answers)
    }
  }

  override suspend fun applyQuickPlan(draft: QuickPlanDraft) {
    database.withTransaction {
      saveQuickPlanDraft(draft.copy(completedAt = System.currentTimeMillis()))
      scheduleDao.deleteReplaceableQuickPlanBlocks(draft.date.toString())
      val blocks = QuickPlanGenerator.blocks(draft).map { block ->
        ScheduleBlockEntity(
          date = block.date.toString(),
          startMinute = block.startMinute,
          endMinute = block.endMinute,
          title = block.title,
          note = block.note,
          taskOccurrenceId = null,
          source = ScheduleSource.QUICK_PLAN.name,
          isUserModified = false,
          isArchived = false,
        )
      }
      if (blocks.isNotEmpty()) scheduleDao.insertBlocks(blocks)
    }
  }
}

class StockRepositoryImpl(
  private val database: AppDatabase,
  private val stockDao: StockDao = database.stockDao(),
  private val shoppingDao: ShoppingDao = database.shoppingDao(),
) : StockRepository {
  override fun observeStock(kind: StockKind): Flow<List<StockItemDetails>> =
    stockDao.observeByKind(kind.name).map { rows -> rows.map { it.toDomain() } }

  override suspend fun getStockItem(id: Long): StockItemDetails? = stockDao.getWithFood(id)?.toDomain()

  override suspend fun saveStockItem(draft: StockItemDraft): Long = database.withTransaction {
    require(draft.name.isNotBlank()) { "物品名称不能为空" }
    validateStock(draft.trackingMode, draft.currentAmount, draft.currentStatus)
    val now = System.currentTimeMillis()
    val existing = draft.id?.let { stockDao.getItem(it) }
    val entity = StockItemEntity(
      id = existing?.id ?: 0,
      name = draft.name.trim(),
      category = draft.category.trim().ifBlank { if (draft.kind == StockKind.FOOD) "食品" else "其他" },
      kind = draft.kind.name,
      unit = draft.unit.trim(),
      trackingMode = draft.trackingMode.name,
      currentAmount = draft.currentAmount,
      currentStatus = draft.currentStatus?.name,
      replenishThreshold = draft.replenishThreshold,
      isArchived = false,
      createdAt = existing?.createdAt ?: now,
      updatedAt = now,
    )
    val id = if (existing == null) stockDao.insertItem(entity) else {
      stockDao.updateItem(entity)
      entity.id
    }
    saveFoodDetails(id, draft.foodDetails)
    stockDao.insertSnapshot(
      StockSnapshotEntity(stockItemId = id, amount = draft.currentAmount, status = draft.currentStatus?.name, recordedAt = now),
    )
    reconcileAuto(entity.copy(id = id))
    id
  }

  override suspend fun updateStock(id: Long, amount: Double?, status: StockLevel?) {
    database.withTransaction {
      val current = requireNotNull(stockDao.getItem(id)) { "库存物品不存在" }
      val mode = TrackingMode.valueOf(current.trackingMode)
      validateStock(mode, amount, status)
      val now = System.currentTimeMillis()
      val updated = current.copy(
        currentAmount = amount,
        currentStatus = status?.name,
        updatedAt = now,
      )
      stockDao.updateItem(updated)
      stockDao.insertSnapshot(
        StockSnapshotEntity(stockItemId = id, amount = amount, status = status?.name, recordedAt = now),
      )
      reconcileAuto(updated)
    }
  }

  override suspend fun archiveStockItem(id: Long) {
    database.withTransaction {
      stockDao.archiveItem(id, System.currentTimeMillis())
      shoppingDao.dismissActiveAuto(id)
    }
  }

  private suspend fun saveFoodDetails(id: Long, details: FoodDetails?) {
    if (details == null) {
      stockDao.deleteFoodDetails(id)
    } else {
      stockDao.upsertFoodDetails(
        FoodDetailsEntity(
          stockItemId = id,
          foodKind = details.foodKind.name,
          storageLocation = details.storageLocation.name,
          expiryDate = details.expiryDate?.toString(),
          expiryWarningDays = details.expiryWarningDays,
        ),
      )
    }
  }

  private suspend fun reconcileAuto(entity: StockItemEntity) {
    val item = entity.toDomain()
    val existing = shoppingDao.getActiveAuto(item.id)
    if (StockRules.needsRestock(item) && existing == null) {
      shoppingDao.insert(
        ShoppingEntryEntity(
          stockItemId = item.id,
          name = item.name,
          unit = item.unit,
          desiredAmount = null,
          source = ShoppingSource.AUTO.name,
          status = ShoppingStatus.ACTIVE.name,
          createdAt = System.currentTimeMillis(),
          purchasedAt = null,
        ),
      )
    } else if (!StockRules.needsRestock(item) && existing != null) {
      shoppingDao.dismissActiveAuto(item.id)
    }
  }

  private fun validateStock(mode: TrackingMode, amount: Double?, status: StockLevel?) {
    when (mode) {
      TrackingMode.QUANTITY -> require(amount != null && amount >= 0) { "数量不能小于 0" }
      TrackingMode.PERCENT -> require(amount != null && amount in 0.0..100.0) { "百分比必须为 0 到 100" }
      TrackingMode.STATUS -> require(status != null) { "请选择库存状态" }
    }
  }
}

class ShoppingRepositoryImpl(
  private val database: AppDatabase,
  private val shoppingDao: ShoppingDao = database.shoppingDao(),
  private val stockDao: StockDao = database.stockDao(),
) : ShoppingRepository {
  override fun observeActive(): Flow<List<ShoppingEntry>> =
    shoppingDao.observeActive().map { entries -> entries.map { it.toDomain() } }

  override suspend fun addManual(name: String, unit: String, desiredAmount: Double?): Long {
    require(name.isNotBlank()) { "采购项不能为空" }
    return shoppingDao.insert(
      ShoppingEntryEntity(
        stockItemId = null,
        name = name.trim(),
        unit = unit.trim(),
        desiredAmount = desiredAmount,
        source = ShoppingSource.MANUAL.name,
        status = ShoppingStatus.ACTIVE.name,
        createdAt = System.currentTimeMillis(),
        purchasedAt = null,
      ),
    )
  }

  override suspend fun markPurchased(id: Long, purchasedAmount: Double?) {
    database.withTransaction {
      val entry = requireNotNull(shoppingDao.get(id)) { "采购项不存在" }
      val now = System.currentTimeMillis()
      shoppingDao.update(
        entry.copy(status = ShoppingStatus.PURCHASED.name, purchasedAt = now),
      )
      val item = entry.stockItemId?.let { stockDao.getItem(it) } ?: return@withTransaction
      if (purchasedAmount != null) {
        val mode = TrackingMode.valueOf(item.trackingMode)
        val updated = when (mode) {
          TrackingMode.QUANTITY -> item.copy(currentAmount = purchasedAmount.coerceAtLeast(0.0), updatedAt = now)
          TrackingMode.PERCENT -> item.copy(currentAmount = purchasedAmount.coerceIn(0.0, 100.0), updatedAt = now)
          TrackingMode.STATUS -> item.copy(currentStatus = StockLevel.ENOUGH.name, updatedAt = now)
        }
        stockDao.updateItem(updated)
        stockDao.insertSnapshot(
          StockSnapshotEntity(
            stockItemId = updated.id,
            amount = updated.currentAmount,
            status = updated.currentStatus,
            recordedAt = now,
          ),
        )
      }
    }
  }

  override suspend fun dismiss(id: Long) {
    val entry = shoppingDao.get(id) ?: return
    shoppingDao.update(entry.copy(status = ShoppingStatus.DISMISSED.name))
  }
}
