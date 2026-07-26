package com.example.lifeplanner.feature.todo

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
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class TodoViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun combinesTaskOverviewWithFullDaySchedule() = runTest(mainDispatcherRule.dispatcher) {
    val date = LocalDate.now()
    val task = Task(
      id = 1,
      title = "写报告",
      recurrenceStart = date,
      createdAt = 1,
      updatedAt = 1,
    )
    val occurrence = TaskOccurrence(id = 2, taskId = 1, plannedDate = date)
    val overview = TodoOverview(urgent = listOf(TodoItem(task, occurrence)))
    val schedule = DaySchedule(
      date,
      blocks = listOf(
        ScheduleBlock(
          id = 10,
          date = date,
          startMinute = 9 * 60,
          endMinute = 10 * 60,
          title = "写报告",
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
      ),
    )
    val viewModel = TodoViewModel(FakeTaskRepository(overview), FakeScheduleRepository(schedule))

    advanceUntilIdle()

    assertFalse(viewModel.state.value.isLoading)
    assertSame(overview, viewModel.state.value.overview)
    assertEquals(listOf("写报告", "实验"), viewModel.state.value.daySchedule.blocks.map { it.title })
    assertEquals(1, viewModel.state.value.overview.urgent.size)
    assertEquals(2, viewModel.state.value.daySchedule.blocks.size)
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
  override suspend fun getQuickPlanDraft(date: LocalDate): QuickPlanDraft? = null
  override suspend fun saveQuickPlanDraft(draft: QuickPlanDraft) = Unit
  override suspend fun applyQuickPlan(draft: QuickPlanDraft) = Unit
}
