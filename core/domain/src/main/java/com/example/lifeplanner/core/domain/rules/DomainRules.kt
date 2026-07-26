package com.example.lifeplanner.core.domain.rules

import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.OccurrenceStatus
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.QuickPlanEntryRef
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodEntry
import com.example.lifeplanner.core.domain.model.RecurrenceFrequency
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleSource
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.Task
import com.example.lifeplanner.core.domain.model.TaskOccurrence
import com.example.lifeplanner.core.domain.model.TodoItem
import com.example.lifeplanner.core.domain.model.TodoOverview
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.quickplan.QuickPlanCatalog
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
    addPeriodEntries(draft, QuickPlanCardType.WORK, "学习 / 工作", workWindows)
    addPeriodEntries(draft, QuickPlanCardType.GO_OUT, "出门", outingWindows)
    addMeal(draft, QuickPlanCardType.BREAKFAST, "早餐", 7 * 60 + 30, 8 * 60)
    addMeal(draft, QuickPlanCardType.LUNCH, "午餐", 12 * 60, 13 * 60)
    addMeal(draft, QuickPlanCardType.DINNER, "晚餐", 18 * 60, 19 * 60)
    addTimeOptions(draft, QuickPlanCardType.FITNESS, "健身", fitnessWindows)

    draft.answers[QuickPlanCardType.RETURN_HOME]?.hour?.let { hour ->
      val end = if (hour == 0) 24 * 60 else hour * 60
      add(generated(draft.date, end - 30, end, "回家", QuickPlanCardType.RETURN_HOME, SINGLE_SLOT))
    }
  }

  private fun MutableList<ScheduleBlock>.addPeriodEntries(
    draft: QuickPlanDraft,
    type: QuickPlanCardType,
    fallbackTitle: String,
    windows: Map<DayPeriod, IntRange>,
  ) {
    val answer = draft.answers[type] ?: return
    answer.resolvedPeriodEntries(type, fallbackTitle)
      .filter { it.isIncluded && it.hasActivity }
      .forEach { entry ->
        val range = windows[entry.period] ?: return@forEach
        val customText = entry.customText.trim()
        val tag = entry.tag.trim()
        val title = customText.ifBlank { tag }.ifBlank { fallbackTitle }
        val note = buildList {
          if (customText.isNotBlank() && tag.isNotBlank()) add(tag)
          if (entry.location.isNotBlank()) add(entry.location)
        }.joinToString(" · ")
        add(
          generated(
            draft.date,
            range.first,
            range.last + 1,
            title,
            type,
            periodSlot(entry.period),
            note,
          ),
        )
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
        val period = QuickPlanCatalog.periodForLabel(label) ?: return@let
        add(
          generated(
            draft.date,
            range.first,
            range.last + 1,
            title,
            type,
            periodSlot(period),
            note,
          ),
        )
      }
    }
  }

  private fun QuickPlanAnswer.resolvedPeriodEntries(
    type: QuickPlanCardType,
    fallbackTitle: String,
  ): List<QuickPlanPeriodEntry> {
    if (periodEntries.isNotEmpty()) return periodEntries
    val legacyPeriods = selectedOptions.mapNotNull { label ->
      QuickPlanCatalog.periodForLabel(label)?.let { period ->
        QuickPlanPeriodEntry(
          period = period,
          tag = if (type == QuickPlanCardType.GO_OUT) subSelections.firstOrNull().orEmpty() else "",
          customText = if (type == QuickPlanCardType.WORK) fallbackTitle else "",
          location = if (type == QuickPlanCardType.WORK) subSelections.firstOrNull().orEmpty() else "",
        )
      }
    }
    return legacyPeriods
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
    add(generated(draft.date, start, end, title, type, SINGLE_SLOT, note))
  }

  private fun generated(
    date: LocalDate,
    start: Int,
    end: Int,
    title: String,
    cardType: QuickPlanCardType,
    slotKey: String,
    note: String = "",
  ): ScheduleBlock = ScheduleBlock(
    date = date,
    startMinute = start,
    endMinute = end,
    title = title,
    note = note,
    source = ScheduleSource.QUICK_PLAN,
    quickPlanEntryRef = QuickPlanEntryRef(cardType, slotKey),
  )

  fun periodSlot(period: DayPeriod): String = "period:${period.name}"

  fun periodFromSlot(slotKey: String): DayPeriod? =
    slotKey.removePrefix("period:").takeIf { it != slotKey }?.let {
      runCatching { DayPeriod.valueOf(it) }.getOrNull()
    }

  const val SINGLE_SLOT = "single"

  private val workWindows = mapOf(
    DayPeriod.MORNING to (9 * 60 until 12 * 60),
    DayPeriod.AFTERNOON to (14 * 60 until 17 * 60 + 30),
    DayPeriod.EVENING to (19 * 60 until 21 * 60),
  )
  private val outingWindows = mapOf(
    DayPeriod.MORNING to (9 * 60 until 11 * 60 + 30),
    DayPeriod.AFTERNOON to (14 * 60 until 17 * 60),
    DayPeriod.EVENING to (19 * 60 until 21 * 60),
  )
  private val fitnessWindows = mapOf(
    "早上" to (7 * 60 until 8 * 60),
    "下午" to (17 * 60 until 18 * 60),
    "晚上" to (20 * 60 until 21 * 60),
  )
}

object QuickPlanReconciler {
  fun withLinkedEntries(
    draft: QuickPlanDraft,
    blocks: List<ScheduleBlock>,
  ): QuickPlanDraft {
    val linked = blocks.filter {
      !it.isArchived &&
        it.source == ScheduleSource.QUICK_PLAN &&
        it.quickPlanEntryRef != null
    }
    val linkedByType = linked.groupBy { requireNotNull(it.quickPlanEntryRef).cardType }
    val answers = draft.answers.toMutableMap()

    reconcilePeriods(QuickPlanCardType.WORK, linkedByType, answers)
    reconcilePeriods(QuickPlanCardType.GO_OUT, linkedByType, answers)
    reconcileMeal(QuickPlanCardType.BREAKFAST, linkedByType, answers)
    reconcileMeal(QuickPlanCardType.LUNCH, linkedByType, answers)
    reconcileMeal(QuickPlanCardType.DINNER, linkedByType, answers)
    reconcileReturnHome(linkedByType, answers)
    reconcileFitness(linkedByType, answers)
    return draft.copy(
      currentIndex = 0,
      answers = answers,
      completedAt = null,
    )
  }

  private fun reconcilePeriods(
    type: QuickPlanCardType,
    linkedByType: Map<QuickPlanCardType, List<ScheduleBlock>>,
    answers: MutableMap<QuickPlanCardType, QuickPlanAnswer>,
  ) {
    val existing = answers[type]
    val existingByPeriod = existing?.periodEntries.orEmpty().associateBy(QuickPlanPeriodEntry::period)
    val blocksByPeriod = linkedByType[type].orEmpty().mapNotNull { block ->
      val period = block.quickPlanEntryRef?.slotKey?.let(QuickPlanGenerator::periodFromSlot)
      period?.let { it to block }
    }.toMap()
    val entries = DayPeriod.entries.mapNotNull { period ->
      val current = existingByPeriod[period]
      val block = blocksByPeriod[period]
      when {
        block != null -> current?.copy(isIncluded = true) ?: when (type) {
          QuickPlanCardType.WORK -> QuickPlanPeriodEntry(
            period = period,
            customText = block.title,
            location = block.note,
          )
          else -> QuickPlanPeriodEntry(
            period = period,
            tag = block.title,
            location = block.note,
          )
        }
        type == QuickPlanCardType.GO_OUT &&
          current != null &&
          (!current.isIncluded || !current.hasActivity) -> current
        else -> null
      }
    }
    if (entries.isEmpty()) {
      answers.remove(type)
    } else {
      answers[type] = (existing ?: QuickPlanAnswer(type)).copy(periodEntries = entries)
    }
  }

  private fun reconcileMeal(
    type: QuickPlanCardType,
    linkedByType: Map<QuickPlanCardType, List<ScheduleBlock>>,
    answers: MutableMap<QuickPlanCardType, QuickPlanAnswer>,
  ) {
    val block = linkedByType[type].orEmpty().firstOrNull {
      it.quickPlanEntryRef?.slotKey == QuickPlanGenerator.SINGLE_SLOT
    }
    val existing = answers[type]
    if (block == null) {
      if (existing?.selectedOptions?.singleOrNull() != "不吃") answers.remove(type)
      return
    }
    if (existing == null || existing.selectedOptions.isEmpty()) {
      val inferred = block.note.substringBefore(" · ").takeIf(String::isNotBlank)
      answers[type] = QuickPlanAnswer(type, selectedOptions = listOfNotNull(inferred))
    }
  }

  private fun reconcileReturnHome(
    linkedByType: Map<QuickPlanCardType, List<ScheduleBlock>>,
    answers: MutableMap<QuickPlanCardType, QuickPlanAnswer>,
  ) {
    val type = QuickPlanCardType.RETURN_HOME
    val block = linkedByType[type].orEmpty().firstOrNull {
      it.quickPlanEntryRef?.slotKey == QuickPlanGenerator.SINGLE_SLOT
    }
    if (block == null) {
      answers.remove(type)
      return
    }
    if (answers[type]?.hour == null) {
      val hour = if (block.endMinute == 24 * 60) 0 else block.endMinute / 60
      answers[type] = QuickPlanAnswer(type, hour = hour)
    }
  }

  private fun reconcileFitness(
    linkedByType: Map<QuickPlanCardType, List<ScheduleBlock>>,
    answers: MutableMap<QuickPlanCardType, QuickPlanAnswer>,
  ) {
    val type = QuickPlanCardType.FITNESS
    val periods = linkedByType[type].orEmpty().mapNotNull {
      it.quickPlanEntryRef?.slotKey?.let(QuickPlanGenerator::periodFromSlot)
    }.toSet()
    val existing = answers[type]
    if (periods.isEmpty()) {
      if (existing?.selectedOptions?.singleOrNull() != "今日练休") answers.remove(type)
      return
    }
    val selected = DayPeriod.entries.filter(periods::contains).map(QuickPlanCatalog::periodLabel)
    answers[type] = (existing ?: QuickPlanAnswer(type)).copy(selectedOptions = selected)
  }
}

object StockRules {
  fun needsRestock(item: StockItem): Boolean = when (item.trackingMode) {
    TrackingMode.QUANTITY, TrackingMode.PERCENT ->
      item.currentAmount != null &&
        item.replenishThreshold != null &&
        item.currentAmount <= item.replenishThreshold
    TrackingMode.STATUS -> item.currentStatus == StockLevel.MISSING || item.currentStatus == StockLevel.LOW
  }

  fun availableFoods(items: List<StockItemDetails>, date: LocalDate): List<StockItemDetails> =
    items.filter { details ->
      val item = details.item
      val available = when (item.trackingMode) {
        TrackingMode.QUANTITY, TrackingMode.PERCENT -> (item.currentAmount ?: 0.0) > 0.0
        TrackingMode.STATUS -> item.currentStatus != null && item.currentStatus != StockLevel.MISSING
      }
      item.kind == StockKind.FOOD &&
        !item.isArchived &&
        details.foodDetails != null &&
        available &&
        details.foodDetails.expiryDate?.isBefore(date) != true
    }.sortedWith(
      compareBy<StockItemDetails, LocalDate?>(nullsLast()) { it.foodDetails?.expiryDate }
        .thenBy { if (it.foodDetails?.foodKind == FoodKind.PREPARED) 0 else 1 }
        .thenBy { it.item.name },
    )
}

fun TaskOccurrence.withTaskDue(task: Task): TaskOccurrence {
  val dueTime = task.dueAt?.toLocalTime()
  return copy(dueAt = dueTime?.let { LocalDateTime.of(plannedDate, it) })
}

fun defaultOccurrenceDate(task: Task): LocalDate = task.dueAt?.toLocalDate() ?: task.recurrenceStart

fun defaultDueTime(task: Task): LocalTime? = task.dueAt?.toLocalTime()
