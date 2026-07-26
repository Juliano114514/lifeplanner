package com.example.lifeplanner.core.domain.rules

import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodEntry
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.RecurrenceRule
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.quickplan.QuickPlanReducer
import java.time.LocalDate
import java.time.LocalDateTime
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
          periodEntries = listOf(
            QuickPlanPeriodEntry(
              period = DayPeriod.MORNING,
              tag = "学习",
              customText = "看论文",
              location = "北区",
            ),
          ),
        ),
      ),
    )

    val block = QuickPlanGenerator.blocks(draft).single()

    assertEquals(9 * 60, block.startMinute)
    assertEquals(12 * 60, block.endMinute)
    assertEquals("看论文", block.title)
    assertEquals("学习 · 北区", block.note)
  }

  @Test
  fun quickPlanPeriodsUpdateIndependentlyAndRestClearsLocation() {
    var draft = QuickPlanReducer.newDraft(LocalDate.of(2026, 7, 26))
    draft = QuickPlanReducer.setPeriodTag(draft, DayPeriod.MORNING, "学习")
    draft = QuickPlanReducer.setPeriodLocation(draft, DayPeriod.MORNING, "北区")
    draft = QuickPlanReducer.setPeriodText(draft, DayPeriod.AFTERNOON, "做实验")
    draft = QuickPlanReducer.setPeriodTag(draft, DayPeriod.MORNING, "休息")

    val entries = draft.answers.getValue(QuickPlanCardType.WORK).periodEntries
    assertEquals("", entries.first { it.period == DayPeriod.MORNING }.location)
    assertEquals("做实验", entries.first { it.period == DayPeriod.AFTERNOON }.customText)
  }

  @Test
  fun quickPlanKeepsOverlappingWorkAndOutingBlocks() {
    val date = LocalDate.of(2026, 7, 26)
    val draft = QuickPlanDraft(
      date = date,
      answers = mapOf(
        QuickPlanCardType.WORK to QuickPlanAnswer(
          QuickPlanCardType.WORK,
          periodEntries = listOf(QuickPlanPeriodEntry(DayPeriod.MORNING, tag = "学习")),
        ),
        QuickPlanCardType.GO_OUT to QuickPlanAnswer(
          QuickPlanCardType.GO_OUT,
          periodEntries = listOf(QuickPlanPeriodEntry(DayPeriod.MORNING, tag = "出去办事")),
        ),
      ),
    )

    val blocks = QuickPlanGenerator.blocks(draft)

    assertEquals(2, blocks.size)
    assertTrue(blocks[0].startMinute < blocks[1].endMinute)
    assertTrue(blocks[1].startMinute < blocks[0].endMinute)
  }

  @Test
  fun quickPlanDoesNotGenerateDisabledOutingPeriod() {
    val date = LocalDate.of(2026, 7, 27)
    val draft = QuickPlanDraft(
      date = date,
      answers = mapOf(
        QuickPlanCardType.GO_OUT to QuickPlanAnswer(
          QuickPlanCardType.GO_OUT,
          periodEntries = listOf(
            QuickPlanPeriodEntry(
              period = DayPeriod.MORNING,
              isIncluded = false,
              tag = "出去办事",
            ),
            QuickPlanPeriodEntry(
              period = DayPeriod.AFTERNOON,
              tag = "出去玩",
            ),
          ),
        ),
      ),
    )

    val blocks = QuickPlanGenerator.blocks(draft)

    assertEquals(listOf("出去玩"), blocks.map(ScheduleBlock::title))
    assertFalse(blocks.any { it.startMinute < 12 * 60 })
  }

  @Test
  fun quickPlanReconcilePreservesExplicitNotGoingOutChoice() {
    val date = LocalDate.of(2026, 7, 27)
    val draft = QuickPlanDraft(
      date = date,
      answers = mapOf(
        QuickPlanCardType.GO_OUT to QuickPlanAnswer(
          QuickPlanCardType.GO_OUT,
          periodEntries = listOf(
            QuickPlanPeriodEntry(
              period = DayPeriod.MORNING,
              isIncluded = false,
              tag = "出去办事",
            ),
          ),
        ),
      ),
    )

    val restored = QuickPlanReconciler.withLinkedEntries(draft, emptyList())
      .answers.getValue(QuickPlanCardType.GO_OUT)
      .periodEntries.single()

    assertFalse(restored.isIncluded)
    assertEquals("出去办事", restored.tag)
  }

  @Test
  fun availableFoodsExcludeMissingAndExpiredItems() {
    val date = LocalDate.of(2026, 7, 26)
    val available = food("青菜", 1.0, null, date.plusDays(1))
    val expired = food("剩饭", 1.0, null, date.minusDays(1))
    val missing = food("鸡蛋", null, StockLevel.MISSING, null, TrackingMode.STATUS)

    assertEquals(listOf("青菜"), StockRules.availableFoods(listOf(missing, expired, available), date).map { it.item.name })
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

  private fun food(
    name: String,
    amount: Double?,
    status: StockLevel?,
    expiryDate: LocalDate?,
    trackingMode: TrackingMode = TrackingMode.QUANTITY,
  ): StockItemDetails = StockItemDetails(
    item = StockItem(
      id = name.hashCode().toLong(),
      name = name,
      category = "食品",
      kind = StockKind.FOOD,
      trackingMode = trackingMode,
      currentAmount = amount,
      currentStatus = status,
      createdAt = 0,
      updatedAt = 0,
    ),
    foodDetails = FoodDetails(
      stockItemId = name.hashCode().toLong(),
      foodKind = FoodKind.INGREDIENT,
      storageLocation = com.example.lifeplanner.core.domain.model.StorageLocation.REFRIGERATED,
      expiryDate = expiryDate,
    ),
  )
}
