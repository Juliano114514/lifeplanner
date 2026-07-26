package com.example.lifeplanner.feature.schedule

import com.example.lifeplanner.core.domain.model.DayPeriod
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
import com.example.lifeplanner.core.domain.model.QuickPlanAnswer
import com.example.lifeplanner.core.domain.model.QuickPlanCardType
import com.example.lifeplanner.core.domain.model.QuickPlanDraft
import com.example.lifeplanner.core.domain.model.QuickPlanPeriodEntry
import com.example.lifeplanner.core.domain.model.ScheduleBlock
import com.example.lifeplanner.core.domain.model.ScheduleBlockDraft
import com.example.lifeplanner.core.domain.model.StockItem
import com.example.lifeplanner.core.domain.model.StockItemDetails
import com.example.lifeplanner.core.domain.model.StockItemDraft
import com.example.lifeplanner.core.domain.model.StockKind
import com.example.lifeplanner.core.domain.model.StockLevel
import com.example.lifeplanner.core.domain.model.StorageLocation
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import java.time.LocalDate
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class QuickPlanViewModelTest {
  @get:Rule
  val mainDispatcherRule = MainDispatcherRule()

  @Test
  fun loadRestoresDraftAndKeepsPeriodEditsUntilComplete() = runTest(mainDispatcherRule.dispatcher) {
    val date = LocalDate.of(2026, 7, 28)
    val restored = QuickPlanDraft(
      date = date,
      answers = mapOf(
        QuickPlanCardType.WORK to QuickPlanAnswer(
          cardType = QuickPlanCardType.WORK,
          periodEntries = listOf(
            QuickPlanPeriodEntry(DayPeriod.MORNING, tag = "实验", location = "北区"),
          ),
        ),
      ),
    )
    val scheduleRepository = FakeScheduleRepository(restored)
    val viewModel = QuickPlanViewModel(scheduleRepository, FakeStockRepository())

    viewModel.load(date.toEpochDay())
    advanceUntilIdle()

    assertEquals("实验", viewModel.state.value.draft.workEntry(DayPeriod.MORNING).tag)
    viewModel.setPeriodText(DayPeriod.MORNING, "电池循环测试")
    viewModel.setPeriodTag(DayPeriod.AFTERNOON, "休息")
    viewModel.setPeriodLocation(DayPeriod.AFTERNOON, "北区")
    advanceUntilIdle()

    val edited = viewModel.state.value.draft
    assertEquals("电池循环测试", edited.workEntry(DayPeriod.MORNING).customText)
    assertEquals("休息", edited.workEntry(DayPeriod.AFTERNOON).tag)
    assertTrue(edited.workEntry(DayPeriod.AFTERNOON).location.isBlank())

    viewModel.clearPeriod(DayPeriod.MORNING)
    advanceUntilIdle()
    assertFalse(viewModel.state.value.draft.answers.getValue(QuickPlanCardType.WORK)
      .periodEntries.any { it.period == DayPeriod.MORNING })
    assertTrue(scheduleRepository.appliedDrafts.isEmpty())
  }

  @Test
  fun availableFoodsFollowTargetDateRules() = runTest(mainDispatcherRule.dispatcher) {
    val date = LocalDate.of(2026, 7, 28)
    val foods = listOf(
      food(1, "番茄", amount = 2.0, expiry = date.plusDays(1)),
      food(2, "昨天的饭", amount = 1.0, expiry = date.minusDays(1)),
      food(3, "空盒", amount = 0.0, expiry = date.plusDays(2)),
    )
    val viewModel = QuickPlanViewModel(
      FakeScheduleRepository(QuickPlanDraft(date)),
      FakeStockRepository(foods),
    )

    viewModel.load(date.toEpochDay())
    advanceUntilIdle()

    assertEquals(listOf("番茄"), viewModel.state.value.availableFoods.map { it.item.name })
  }

  private fun QuickPlanDraft.workEntry(period: DayPeriod): QuickPlanPeriodEntry =
    answers.getValue(QuickPlanCardType.WORK).periodEntries.first { it.period == period }

  private fun food(
    id: Long,
    name: String,
    amount: Double,
    expiry: LocalDate,
  ): StockItemDetails = StockItemDetails(
    item = StockItem(
      id = id,
      name = name,
      category = "蔬菜",
      kind = StockKind.FOOD,
      unit = "份",
      trackingMode = TrackingMode.QUANTITY,
      currentAmount = amount,
      createdAt = 1,
      updatedAt = 1,
    ),
    foodDetails = FoodDetails(
      stockItemId = id,
      foodKind = FoodKind.INGREDIENT,
      storageLocation = StorageLocation.REFRIGERATED,
      expiryDate = expiry,
    ),
  )
}

private class FakeScheduleRepository(
  private val restoredDraft: QuickPlanDraft?,
) : ScheduleRepository {
  val appliedDrafts = mutableListOf<QuickPlanDraft>()

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
  override suspend fun archiveBlock(id: Long) = Unit
  override suspend fun startQuickPlan(date: LocalDate): QuickPlanDraft =
    restoredDraft ?: QuickPlanDraft(date)
  override suspend fun applyQuickPlan(draft: QuickPlanDraft, baseline: QuickPlanDraft) {
    appliedDrafts += draft
  }
}

private class FakeStockRepository(
  foods: List<StockItemDetails> = emptyList(),
) : StockRepository {
  private val values = MutableStateFlow(foods)

  override fun observeStock(kind: StockKind): Flow<List<StockItemDetails>> = values
  override suspend fun getStockItem(id: Long): StockItemDetails? = null
  override suspend fun saveStockItem(draft: StockItemDraft): Long = 0
  override suspend fun updateStock(id: Long, amount: Double?, status: StockLevel?) = Unit
  override suspend fun archiveStockItem(id: Long) = Unit
}
