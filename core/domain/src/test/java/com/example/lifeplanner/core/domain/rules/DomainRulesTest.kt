package com.example.lifeplanner.core.domain.rules

import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.RecurrenceRule
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TrackingMode
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DomainRulesTest {
  @Test
  fun monthlyRecurrenceClampsToMonthEnd() {
    val task = task(
      start = LocalDate.of(2026, 1, 31),
      recurrence = RecurrenceRule(RecurrenceFrequency.MONTHLY),
    )

    val dates = RecurrenceGenerator.dates(
      task,
      LocalDate.of(2026, 1, 1),
      LocalDate.of(2026, 3, 31),
    )

    assertEquals(
      listOf(
        LocalDate.of(2026, 1, 31),
        LocalDate.of(2026, 2, 28),
        LocalDate.of(2026, 3, 31),
      ),
      dates,
    )
  }

  @Test
  fun organizerDoesNotDuplicateUrgentTaskInToday() {
    val today = LocalDate.of(2026, 7, 26)
    val task = task(start = today, dueAt = today.atTime(18, 0), pinned = true)
    val occurrence = TaskOccurrence(1, task.id, today, task.dueAt, OccurrenceStatus.PENDING)

    val overview = TodoOrganizer.organize(listOf(task), listOf(occurrence), today)

    assertEquals(1, overview.urgent.size)
    assertTrue(overview.todayPending.isEmpty())
  }

  @Test
  fun scheduleOverlapMarksBothBlocks() {
    val today = LocalDate.of(2026, 7, 26)
    val result = ScheduleRules.withConflicts(
      today,
      listOf(
        ScheduleBlock(1, today, 60, 120, "A"),
        ScheduleBlock(2, today, 90, 180, "B"),
      ),
    )

    assertEquals(setOf(1L, 2L), result.conflictingBlockIds)
  }

  @Test
  fun lowStatusNeedsRestock() {
    val item = StockItem(
      id = 1,
      name = "洗发水",
      category = "日用品",
      kind = StockKind.HOUSEHOLD,
      trackingMode = TrackingMode.STATUS,
      currentStatus = StockLevel.LOW,
      createdAt = 0,
      updatedAt = 0,
    )

    assertTrue(StockRules.needsRestock(item))
  }

  @Test
  fun organizerShowsOnlyCurrentOccurrenceForDailySeries() {
    val today = LocalDate.of(2026, 7, 26)
    val task = task(
      start = today,
      recurrence = RecurrenceRule(RecurrenceFrequency.DAILY),
    )
    val occurrences = (0L..10L).map {
      TaskOccurrence(it + 1, task.id, today.plusDays(it), status = OccurrenceStatus.PENDING)
    }

    val overview = TodoOrganizer.organize(listOf(task), occurrences, today)

    assertEquals(1, overview.todayPending.size)
    assertTrue(overview.others.isEmpty())
  }

  @Test
  fun quickPlanUsesQuarterHourBoundaries() {
    val date = LocalDate.of(2026, 7, 26)
    val draft = QuickPlanDraft(
      date = date,
      answers = mapOf(
        QuickPlanCardType.WORK to QuickPlanAnswer(
          QuickPlanCardType.WORK,
          selectedOptions = listOf("早上"),
        ),
      ),
    )

    val block = QuickPlanGenerator.blocks(draft).single()

    assertEquals(9 * 60, block.startMinute)
    assertEquals(12 * 60, block.endMinute)
  }

  private fun task(
    start: LocalDate,
    dueAt: LocalDateTime? = null,
    recurrence: RecurrenceRule? = null,
    pinned: Boolean = false,
  ): Task = Task(
    id = 7,
    title = "任务",
    dueAt = dueAt,
    isPinned = pinned,
    recurrence = recurrence,
    recurrenceStart = start,
    createdAt = 0,
    updatedAt = 0,
  )
}
