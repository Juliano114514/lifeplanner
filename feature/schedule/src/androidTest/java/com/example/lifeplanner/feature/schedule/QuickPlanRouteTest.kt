package com.example.lifeplanner.feature.schedule

import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.example.lifeplanner.core.domain.model.DaySchedule
import com.example.lifeplanner.core.domain.model.FoodDetails
import com.example.lifeplanner.core.domain.model.FoodKind
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
import com.example.lifeplanner.core.domain.model.TrackingMode
import com.example.lifeplanner.core.domain.quickplan.QuickPlanCatalog
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
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
    )

    composeRule.setContent {
      LifePlannerTheme {
        QuickPlanRoute(date.toEpochDay(), onDone = {}, viewModel = viewModel)
      }
    }
    composeRule.waitUntil { !viewModel.state.value.isLoading }

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
  override suspend fun archiveBlock(id: Long) = Unit
  override suspend fun getQuickPlanDraft(date: LocalDate): QuickPlanDraft = draft
  override suspend fun saveQuickPlanDraft(draft: QuickPlanDraft) = Unit
  override suspend fun applyQuickPlan(draft: QuickPlanDraft) = Unit
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
