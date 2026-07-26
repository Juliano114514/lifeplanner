package com.example.lifeplanner.core.domain.rules

import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.model.TrackingMode
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.YearMonth

object RecurrenceGenerator {
  fun dates(task: Task, start: LocalDate, endInclusive: LocalDate): List<LocalDate> {
    if (task.isArchived || endInclusive < start) return emptyList()
    val first = maxOf(task.recurrenceStart, start)
    val rule = task.recurrence ?: return listOf(task.recurrenceStart)
      .filter { !it.isBefore(start) && !it.isAfter(endInclusive) }

    return buildList {
      var cursor = task.recurrenceStart
      while (cursor.isBefore(first)) {
        cursor = next(cursor, task.recurrenceStart.dayOfMonth, rule.frequency)
      }
      while (!cursor.isAfter(endInclusive)) {
        add(cursor)
        cursor = next(cursor, task.recurrenceStart.dayOfMonth, rule.frequency)
      }
    }
  }

  private fun next(
    date: LocalDate,
    preferredDay: Int,
    frequency: RecurrenceFrequency,
  ): LocalDate = when (frequency) {
    RecurrenceFrequency.DAILY -> date.plusDays(1)
    RecurrenceFrequency.WEEKLY -> date.plusWeeks(1)
    RecurrenceFrequency.MONTHLY -> {
      val target = YearMonth.from(date).plusMonths(1)
      target.atDay(preferredDay.coerceAtMost(target.lengthOfMonth()))
    }
  }
}

object TodoOrganizer {
  fun organize(
    tasks: List<Task>,
    occurrences: List<TaskOccurrence>,
    date: LocalDate,
    dueSoonDays: Long = 3,
  ): TodoOverview {
    val occurrenceByTask = occurrences.groupBy(TaskOccurrence::taskId)
    val urgentEnd = date.plusDays(dueSoonDays)
    val urgent = mutableListOf<TodoItem>()
    val todayPending = mutableListOf<TodoItem>()
    val todayCompleted = mutableListOf<TodoItem>()
    val others = mutableListOf<TodoItem>()

    tasks.filterNot(Task::isArchived).forEach { task ->
      val related = occurrenceByTask[task.id].orEmpty()
      val pending = related.filter { it.status == OccurrenceStatus.PENDING }
      val urgentOccurrence = pending
        .filter { occurrence ->
          val dueDate = occurrence.dueAt?.toLocalDate() ?: task.dueAt?.toLocalDate()
          task.isPinned || dueDate != null && !dueDate.isAfter(urgentEnd)
        }
        .minWithOrNull(compareBy<TaskOccurrence> { it.dueAt }.thenBy { it.plannedDate })
      val pendingToday = pending.firstOrNull { it.plannedDate == date }
      val completedToday = related.firstOrNull {
        it.plannedDate == date && it.status == OccurrenceStatus.COMPLETED
      }

      when {
        urgentOccurrence != null -> urgent += TodoItem(task, urgentOccurrence)
        pendingToday != null -> todayPending += TodoItem(task, pendingToday)
        completedToday != null -> todayCompleted += TodoItem(task, completedToday)
        else -> {
          val next = pending.filter { !it.plannedDate.isBefore(date) }.minByOrNull { it.plannedDate }
            ?: pending.minByOrNull { it.plannedDate }
          others += TodoItem(task, next)
        }
      }
    }

    return TodoOverview(
      urgent = urgent.sortedWith(compareBy<TodoItem> { it.occurrence?.dueAt ?: it.task.dueAt }.thenBy { it.task.title }),
      todayPending = todayPending.sortedBy { it.occurrence?.dueAt },
      todayCompleted = todayCompleted.sortedByDescending { it.occurrence?.completedAt },
      others = others.sortedWith(compareBy<TodoItem> { it.occurrence?.plannedDate }.thenBy { it.task.title }),
    )
  }
}

object ScheduleRules {
  fun validate(startMinute: Int, endMinute: Int) {
    require(startMinute in 0 until MINUTES_PER_DAY) { "开始时间必须在当天范围内" }
    require(endMinute in 1..MINUTES_PER_DAY) { "结束时间必须在当天范围内" }
    require(endMinute > startMinute) { "结束时间必须晚于开始时间" }
  }

  fun withConflicts(date: LocalDate, blocks: List<ScheduleBlock>): DaySchedule {
    val active = blocks.filterNot(ScheduleBlock::isArchived).sortedBy(ScheduleBlock::startMinute)
    val conflicts = buildSet {
      active.forEachIndexed { index, left ->
        active.drop(index + 1).takeWhile { it.startMinute < left.endMinute }.forEach { right ->
          add(left.id)
          add(right.id)
        }
      }
    }
    return DaySchedule(date, active, conflicts)
  }

  private const val MINUTES_PER_DAY = 24 * 60
}

object QuickPlanGenerator {
  fun blocks(draft: QuickPlanDraft): List<ScheduleBlock> = buildList {
    addTimeOptions(draft, QuickPlanCardType.WORK, "学习 / 工作", workWindows)
    addTimeOptions(draft, QuickPlanCardType.GO_OUT, "出门", outingWindows)
    addMeal(draft, QuickPlanCardType.BREAKFAST, "早餐", 7 * 60 + 30, 8 * 60)
    addMeal(draft, QuickPlanCardType.LUNCH, "午餐", 12 * 60, 13 * 60)
    addMeal(draft, QuickPlanCardType.DINNER, "晚餐", 18 * 60, 19 * 60)
    addTimeOptions(draft, QuickPlanCardType.FITNESS, "健身", fitnessWindows)

    draft.answers[QuickPlanCardType.RETURN_HOME]?.hour?.let { hour ->
      val end = if (hour == 0) 24 * 60 else hour * 60
      add(generated(draft.date, end - 30, end, "回家"))
    }
    draft.answers[QuickPlanCardType.OTHER]?.let { answer ->
      val notes = listOf(answer.note) + answer.extraNotes
      notes.filter(String::isNotBlank).forEachIndexed { index, note ->
        val start = 21 * 60 + index * 30
        if (start < 24 * 60) add(generated(draft.date, start, (start + 30).coerceAtMost(24 * 60), "其他", note))
      }
    }
  }

  private fun MutableList<ScheduleBlock>.addTimeOptions(
    draft: QuickPlanDraft,
    type: QuickPlanCardType,
    title: String,
    windows: Map<String, IntRange>,
  ) {
    val answer = draft.answers[type] ?: return
    val note = answer.subSelections.joinToString("、")
    answer.selectedOptions.forEach { label ->
      windows[label]?.let { range ->
        add(generated(draft.date, range.first, range.last + 1, title, note))
      }
    }
  }

  private fun MutableList<ScheduleBlock>.addMeal(
    draft: QuickPlanDraft,
    type: QuickPlanCardType,
    title: String,
    start: Int,
    end: Int,
  ) {
    val answer = draft.answers[type] ?: return
    val choice = answer.selectedOptions.firstOrNull() ?: return
    if (choice == "不吃") return
    val note = listOf(choice, answer.subSelections.joinToString("、")).filter(String::isNotBlank)
      .joinToString(" · ")
    add(generated(draft.date, start, end, title, note))
  }

  private fun generated(
    date: LocalDate,
    start: Int,
    end: Int,
    title: String,
    note: String = "",
  ): ScheduleBlock = ScheduleBlock(
    date = date,
    startMinute = start,
    endMinute = end,
    title = title,
    note = note,
    source = ScheduleSource.QUICK_PLAN,
  )

  private val workWindows = mapOf(
    "早上" to (9 * 60 until 12 * 60),
    "下午" to (14 * 60 until 17 * 60 + 30),
    "晚上" to (19 * 60 until 21 * 60),
  )
  private val outingWindows = mapOf(
    "早上" to (9 * 60 until 11 * 60 + 30),
    "下午" to (14 * 60 until 17 * 60),
    "晚上" to (19 * 60 until 21 * 60),
  )
  private val fitnessWindows = mapOf(
    "早上" to (7 * 60 until 8 * 60),
    "下午" to (17 * 60 until 18 * 60),
    "晚上" to (20 * 60 until 21 * 60),
  )
}

object StockRules {
  fun needsRestock(item: StockItem): Boolean = when (item.trackingMode) {
    TrackingMode.QUANTITY, TrackingMode.PERCENT ->
      item.currentAmount != null &&
        item.replenishThreshold != null &&
        item.currentAmount <= item.replenishThreshold
    TrackingMode.STATUS -> item.currentStatus == StockLevel.MISSING || item.currentStatus == StockLevel.LOW
  }
}

fun TaskOccurrence.withTaskDue(task: Task): TaskOccurrence {
  val dueTime = task.dueAt?.toLocalTime()
  return copy(dueAt = dueTime?.let { LocalDateTime.of(plannedDate, it) })
}

fun defaultOccurrenceDate(task: Task): LocalDate = task.dueAt?.toLocalDate() ?: task.recurrenceStart

fun defaultDueTime(task: Task): LocalTime? = task.dueAt?.toLocalTime()
