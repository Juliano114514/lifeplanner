package com.example.lifeplanner.feature.schedule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockItemDraft
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.StorageLocation
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskDraft
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.quickplan.QuickPlanCatalog
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import com.example.lifeplanner.core.domain.repository.TaskRepository
import com.example.libui.theme.LifePlannerTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Rule
import org.junit.Test

class QuickPlanRouteTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun outingPeriodsDefaultToNotGoingOutAndExpandIndependently() {
    val date = LocalDate.of(2026, 7, 27)
    val viewModel = QuickPlanViewModel(
      FakeScheduleRepository(QuickPlanDraft(date)),
      FakeStockRepository(emptyList()),
      FakeTaskRepository(),
    )

    composeRule.setContent {
      LifePlannerTheme {
        QuickPlanRoute(date.toEpochDay(), onDone = {}, viewModel = viewModel)
      }
    }
    composeRule.waitUntil { !viewModel.state.value.isLoading }
    composeRule.onNodeWithText("下一张").performClick()
    composeRule.waitForIdle()

    composeRule.onAllNodesWithText("不出门").assertCountEquals(3)
    composeRule.onAllNodesWithText("出去玩").assertCountEquals(0)

    composeRule.onAllNodesWithText("出门")[0].performClick()
    composeRule.waitForIdle()

    composeRule.onAllNodesWithText("出去玩").assertCountEquals(1)
  }

  @Test
  fun availableFoodCardsOnlyShowForHomeCooking() {
    val date = LocalDate.of(2026, 7, 28)
    val breakfastIndex = QuickPlanCatalog.cards.indexOfFirst {
      it.type == QuickPlanCardType.BREAKFAST
    }
    val draft = QuickPlanDraft(
      date = date,
      currentIndex = breakfastIndex,
      answers = mapOf(
        QuickPlanCardType.BREAKFAST to QuickPlanAnswer(
          cardType = QuickPlanCardType.BREAKFAST,
          selectedOptions = listOf("自己做"),
        ),
      ),
    )
    val viewModel = QuickPlanViewModel(
      FakeScheduleRepository(draft),
      FakeStockRepository(listOf(food(date))),
      FakeTaskRepository(),
    )

    composeRule.setContent {
      LifePlannerTheme {
        QuickPlanRoute(date.toEpochDay(), onDone = {}, viewModel = viewModel)
      }
    }
    composeRule.waitUntil { !viewModel.state.value.isLoading }
    repeat(2) {
      composeRule.onNodeWithText("下一张").performClick()
      composeRule.waitForIdle()
    }

    composeRule.onNodeWithText("家里现有").assertExists()
    composeRule.onNodeWithText("番茄").assertExists()

    composeRule.onNodeWithText("食堂").performClick()
    composeRule.waitForIdle()

    composeRule.onNodeWithText("家里现有").assertDoesNotExist()
    composeRule.onNodeWithText("番茄").assertDoesNotExist()
  }

  private fun food(date: LocalDate): StockItemDetails = StockItemDetails(
    item = StockItem(
      id = 1,
      name = "番茄",
      category = "蔬菜",
      kind = StockKind.FOOD,
      unit = "个",
      trackingMode = TrackingMode.QUANTITY,
      currentAmount = 2.0,
      createdAt = 1,
      updatedAt = 1,
    ),
    foodDetails = FoodDetails(
      stockItemId = 1,
      foodKind = FoodKind.INGREDIENT,
      storageLocation = StorageLocation.REFRIGERATED,
      expiryDate = date.plusDays(2),
    ),
  )
}

private class FakeScheduleRepository(
  private val draft: QuickPlanDraft,
) : ScheduleRepository {
  override fun observeDay(date: LocalDate): Flow<DaySchedule> =
    MutableStateFlow(DaySchedule(date))
  override suspend fun getBlock(id: Long): ScheduleBlock? = null
  override suspend fun saveBlock(draft: ScheduleBlockDraft): Long = 0
  override suspend fun scheduleTask(
    occurrenceId: Long,
    date: LocalDate,
    startMinute: Int,
    endMinute: Int,
  ): Long = 0
  override suspend fun setBlockStatus(id: Long, status: OccurrenceStatus) = Unit
  override suspend fun archiveBlock(id: Long) = Unit
  override suspend fun startQuickPlan(date: LocalDate): QuickPlanDraft = draft
  override suspend fun applyQuickPlan(draft: QuickPlanDraft, baseline: QuickPlanDraft) = Unit
}

private class FakeTaskRepository : TaskRepository {
  private val values = MutableStateFlow<List<TodoItem>>(emptyList())
  private var nextId = 1L

  override fun observeTodo(date: LocalDate): Flow<TodoOverview> =
    MutableStateFlow(TodoOverview())
  override fun observeSchedulable(date: LocalDate): Flow<List<TodoItem>> = values
  override suspend fun getTask(id: Long): Task? = values.value
    .firstOrNull { it.task.id == id }
    ?.task
  override suspend fun saveTask(draft: TaskDraft): Long {
    val id = nextId++
    val task = Task(
      id = id,
      title = draft.title,
      recurrenceStart = draft.recurrenceStart,
      createdAt = id,
      updatedAt = id,
    )
    values.value += TodoItem(
      task = task,
      occurrence = TaskOccurrence(
        id = id,
        taskId = id,
        plannedDate = draft.recurrenceStart,
      ),
    )
    return id
  }
  override suspend fun setPinned(taskId: Long, pinned: Boolean) = Unit
  override suspend fun setOccurrenceStatus(
    occurrenceId: Long,
    status: OccurrenceStatus,
  ) = Unit
  override suspend fun ensureOccurrences(start: LocalDate, endInclusive: LocalDate) = Unit
  override suspend fun archiveTask(taskId: Long) = Unit
}

private class FakeStockRepository(
  foods: List<StockItemDetails>,
) : StockRepository {
  private val values = MutableStateFlow(foods)

  override fun observeStock(kind: StockKind): Flow<List<StockItemDetails>> = values
  override suspend fun getStockItem(id: Long): StockItemDetails? = null
  override suspend fun saveStockItem(draft: StockItemDraft): Long = 0
  override suspend fun updateStock(id: Long, amount: Double?, status: StockLevel?) = Unit
  override suspend fun archiveStockItem(id: Long) = Unit
}
