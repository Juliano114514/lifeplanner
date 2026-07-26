package com.example.lifeplanner.core.domain.repository

import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.ShoppingEntry
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockItemDraft
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskDraft
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow

interface TaskRepository {
  fun observeTodo(date: LocalDate): Flow<TodoOverview>
  fun observeSchedulable(date: LocalDate): Flow<List<TodoItem>>
  suspend fun getTask(id: Long): Task?
  suspend fun saveTask(draft: TaskDraft): Long
  suspend fun setPinned(taskId: Long, pinned: Boolean)
  suspend fun setOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus)
  suspend fun ensureOccurrences(start: LocalDate, endInclusive: LocalDate)
  suspend fun archiveTask(taskId: Long)
}

interface ScheduleRepository {
  fun observeDay(date: LocalDate): Flow<DaySchedule>
  suspend fun getBlock(id: Long): ScheduleBlock?
  suspend fun saveBlock(draft: ScheduleBlockDraft): Long
  suspend fun scheduleTask(
    occurrenceId: Long,
    date: LocalDate,
    startMinute: Int,
    endMinute: Int,
  ): Long
  suspend fun setBlockStatus(id: Long, status: OccurrenceStatus)
  suspend fun archiveBlock(id: Long)
  suspend fun startQuickPlan(date: LocalDate): QuickPlanDraft
  suspend fun applyQuickPlan(draft: QuickPlanDraft, baseline: QuickPlanDraft)
}

interface StockRepository {
  fun observeStock(kind: StockKind): Flow<List<StockItemDetails>>
  suspend fun getStockItem(id: Long): StockItemDetails?
  suspend fun saveStockItem(draft: StockItemDraft): Long
  suspend fun updateStock(id: Long, amount: Double?, status: StockLevel?)
  suspend fun archiveStockItem(id: Long)
}

interface ShoppingRepository {
  fun observeActive(): Flow<List<ShoppingEntry>>
  suspend fun addManual(name: String, unit: String, desiredAmount: Double?): Long
  suspend fun markPurchased(id: Long, purchasedAmount: Double?)
  suspend fun dismiss(id: Long)
}
