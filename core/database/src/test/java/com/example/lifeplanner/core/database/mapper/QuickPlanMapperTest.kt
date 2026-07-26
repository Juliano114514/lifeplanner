package com.example.lifeplanner.core.database.mapper

import com.example.lifeplanner.core.database.entity.QuickPlanAnswerEntity
import com.example.lifeplanner.core.database.entity.QuickPlanPeriodEntryEntity
import com.example.lifeplanner.core.domain.model.DayPeriod
import org.junit.Assert.assertEquals
import org.junit.Test

class QuickPlanMapperTest {
  @Test
  fun legacyWorkAnswerBecomesPeriodEntries() {
    val answer = answer(
      selectedOptions = "早上\u001F下午",
      subSelections = "北区",
    ).toDomain(emptyList())

    assertEquals(listOf(DayPeriod.MORNING, DayPeriod.AFTERNOON), answer.periodEntries.map { it.period })
    assertEquals("学习 / 工作", answer.periodEntries.first().customText)
    assertEquals("北区", answer.periodEntries.first().location)
  }

  @Test
  fun periodEntriesRestoreWithoutLegacyFallback() {
    val answer = answer(selectedOptions = "早上").toDomain(
      listOf(
        QuickPlanPeriodEntryEntity(
          draftDate = DATE,
          cardType = "WORK",
          period = DayPeriod.EVENING.name,
          tag = "写作",
          customText = "整理报告",
          location = "在家",
        ),
      ),
    )

    assertEquals(1, answer.periodEntries.size)
    assertEquals(DayPeriod.EVENING, answer.periodEntries.single().period)
    assertEquals("整理报告", answer.periodEntries.single().customText)
  }

  private fun answer(
    selectedOptions: String,
    subSelections: String = "",
  ): QuickPlanAnswerEntity = QuickPlanAnswerEntity(
    draftDate = DATE,
    cardType = "WORK",
    selectedOptions = selectedOptions,
    subSelections = subSelections,
    hour = null,
    note = "",
    extraNotes = "",
  )

  private companion object {
    const val DATE = "2026-07-26"
  }
}
