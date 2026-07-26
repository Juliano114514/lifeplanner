package com.example.lifeplanner.feature.todo

import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.longClick
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.RecurrenceRule
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
import java.time.LocalDateTime
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
    val taskRepository = FakeTaskRepository(overview)
    val viewModel = TodoViewModel(taskRepository, FakeScheduleRepository(schedule))
    val taskEditorViewModel = TaskEditorViewModel(taskRepository)

    composeRule.setContent {
      LifePlannerTheme {
        TodoRoute(
          onScheduleTask = {},
          onOpenSchedule = { openedEpochDay = it },
          viewModel = viewModel,
          taskEditorViewModel = taskEditorViewModel,
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

  @Test
  fun taskCardOpensPrefilledEditorOnlyOnLongClickAndSavesExistingId() {
    val date = LocalDate.now()
    val dueAt = LocalDateTime.of(date, java.time.LocalTime.of(18, 30))
    val task = Task(
      id = 21,
      title = "现有任务",
      note = "已有备注",
      dueAt = dueAt,
      isPinned = true,
      recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
      recurrenceStart = date,
      createdAt = 1,
      updatedAt = 2,
    )
    val occurrence = TaskOccurrence(id = 22, taskId = task.id, plannedDate = date)
    val overview = TodoOverview(todayPending = listOf(TodoItem(task, occurrence)))
    val taskRepository = FakeTaskRepository(overview)
    val viewModel = TodoViewModel(taskRepository, FakeScheduleRepository(DaySchedule(date)))
    val taskEditorViewModel = TaskEditorViewModel(taskRepository)

    composeRule.setContent {
      LifePlannerTheme {
        TodoRoute(
          onScheduleTask = {},
          onOpenSchedule = {},
          viewModel = viewModel,
          taskEditorViewModel = taskEditorViewModel,
        )
      }
    }
    composeRule.waitUntil { !viewModel.state.value.isLoading }

    composeRule.onNodeWithTag("todo-card-${task.id}").performClick()
    composeRule.onNodeWithText("修改任务").assertDoesNotExist()

    composeRule.onNodeWithTag("todo-card-${task.id}").performTouchInput { longClick() }
    composeRule.waitUntil { !taskEditorViewModel.state.value.isLoading }

    composeRule.onNodeWithText("修改任务").assertExists()
    composeRule.onAllNodesWithText(task.title).assertCountEquals(2)
    composeRule.onNodeWithText(task.note).assertExists()
    composeRule.onNodeWithText("保存").performClick()
    composeRule.waitUntil { taskRepository.savedDraft != null }

    val saved = requireNotNull(taskRepository.savedDraft)
    assertEquals(task.id, saved.id)
    assertEquals(task.title, saved.title)
    assertEquals(task.note, saved.note)
    assertEquals(task.dueAt, saved.dueAt)
    assertEquals(task.isPinned, saved.isPinned)
    assertEquals(task.recurrence, saved.recurrence)
    assertEquals(task.recurrenceStart, saved.recurrenceStart)
  }
}

private class FakeTaskRepository(
  overview: TodoOverview,
) : TaskRepository {
  private val values = MutableStateFlow(overview)
  private val tasks = (
    overview.urgent +
      overview.todayPending +
      overview.todayCompleted +
      overview.others
    ).associate { it.task.id to it.task }
  var savedDraft: TaskDraft? = null
    private set

  override fun observeTodo(date: LocalDate): Flow<TodoOverview> = values
  override fun observeSchedulable(date: LocalDate): Flow<List<TodoItem>> =
    MutableStateFlow(emptyList())
  override suspend fun getTask(id: Long): Task? = tasks[id]
  override suspend fun saveTask(draft: TaskDraft): Long {
    savedDraft = draft
    return draft.id ?: 0
  }
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
  override suspend fun setBlockStatus(id: Long, status: OccurrenceStatus) = Unit
  override suspend fun archiveBlock(id: Long) = Unit
  override suspend fun startQuickPlan(date: LocalDate): QuickPlanDraft = QuickPlanDraft(date)
  override suspend fun applyQuickPlan(draft: QuickPlanDraft, baseline: QuickPlanDraft) = Unit
}
