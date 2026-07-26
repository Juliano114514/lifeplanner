package com.example.lifeplanner.feature.todo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskDraft
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.TaskRepository
import com.example.libui.theme.LifePlannerTheme
import java.time.LocalDate
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

class TodoRouteTest {
  @get:Rule
  val composeRule = createComposeRule()

  @Test
  fun scheduleShowsSourcesKeepsTaskInBothSectionsAndOpensDay() {
    val date = LocalDate.now()
    val task = Task(
      id = 1,
      title = "任务A",
      recurrenceStart = date,
      createdAt = 1,
      updatedAt = 1,
    )
    val occurrence = TaskOccurrence(id = 2, taskId = task.id, plannedDate = date)
    val overview = TodoOverview(urgent = listOf(TodoItem(task, occurrence)))
    val schedule = DaySchedule(
      date = date,
      blocks = listOf(
        ScheduleBlock(
          id = 10,
          date = date,
          startMinute = 9 * 60,
          endMinute = 10 * 60,
          title = "任务A",
          taskOccurrenceId = occurrence.id,
        ),
        ScheduleBlock(
          id = 11,
          date = date,
          startMinute = 14 * 60,
          endMinute = 17 * 60,
          title = "实验",
          source = ScheduleSource.QUICK_PLAN,
        ),
        ScheduleBlock(
          id = 12,
          date = date,
          startMinute = 19 * 60,
          endMinute = 20 * 60,
          title = "手动安排",
        ),
      ),
    )
    var openedEpochDay: Long? = null
    val viewModel = TodoViewModel(FakeTaskRepository(overview), FakeScheduleRepository(schedule))

    composeRule.setContent {
      LifePlannerTheme {
        TodoRoute(
          onAddTask = {},
          onEditTask = {},
          onScheduleTask = {},
          onOpenSchedule = { openedEpochDay = it },
          viewModel = viewModel,
        )
      }
    }
    composeRule.waitUntil { !viewModel.state.value.isLoading }

    composeRule.onNodeWithText("今日日程").assertExists()
    composeRule.onNodeWithText("任务").assertExists()
    composeRule.onNodeWithText("快速安排").assertExists()
    composeRule.onNodeWithText("手动").assertExists()
    composeRule.onAllNodesWithText("任务A").assertCountEquals(2)

    val morningTop = composeRule.onNodeWithTag("today-schedule-10")
      .fetchSemanticsNode().boundsInRoot.top
    val afternoonTop = composeRule.onNodeWithTag("today-schedule-11")
      .fetchSemanticsNode().boundsInRoot.top
    assertTrue(morningTop < afternoonTop)

    composeRule.onNodeWithTag("today-schedule-12").performClick()
    assertEquals(date.toEpochDay(), openedEpochDay)
  }
}

private class FakeTaskRepository(
  overview: TodoOverview,
) : TaskRepository {
  private val values = MutableStateFlow(overview)

  override fun observeTodo(date: LocalDate): Flow<TodoOverview> = values
  override fun observeSchedulable(date: LocalDate): Flow<List<TodoItem>> =
    MutableStateFlow(emptyList())
  override suspend fun getTask(id: Long): Task? = null
  override suspend fun saveTask(draft: TaskDraft): Long = 0
  override suspend fun setPinned(taskId: Long, pinned: Boolean) = Unit
  override suspend fun setOccurrenceStatus(occurrenceId: Long, status: OccurrenceStatus) = Unit
  override suspend fun ensureOccurrences(start: LocalDate, endInclusive: LocalDate) = Unit
  override suspend fun archiveTask(taskId: Long) = Unit
}

private class FakeScheduleRepository(
  schedule: DaySchedule,
) : ScheduleRepository {
  private val values = MutableStateFlow(schedule)

  override fun observeDay(date: LocalDate): Flow<DaySchedule> = values
  override suspend fun getBlock(id: Long): ScheduleBlock? = null
  override suspend fun saveBlock(draft: ScheduleBlockDraft): Long = 0
  override suspend fun scheduleTask(
    occurrenceId: Long,
    date: LocalDate,
    startMinute: Int,
    endMinute: Int,
  ): Long = 0
  override suspend fun archiveBlock(id: Long) = Unit
  override suspend fun startQuickPlan(date: LocalDate): QuickPlanDraft = QuickPlanDraft(date)
  override suspend fun applyQuickPlan(draft: QuickPlanDraft, baseline: QuickPlanDraft) = Unit
}
