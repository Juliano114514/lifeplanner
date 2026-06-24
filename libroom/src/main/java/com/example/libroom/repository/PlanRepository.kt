package com.example.libroom.repository

import com.example.foundation.domain.model.PlanCardAnswer
import com.example.foundation.domain.model.PlanRecord
import com.example.foundation.util.DateTimeUtil
import com.example.libroom.local.dao.PlanDao
import com.example.libroom.local.entity.PlanRecordEntity
import com.example.libroom.local.mapper.PlanMapper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

interface PlanRepository {
  fun observeIncompleteForToday(): Flow<PlanRecord?>
  suspend fun createTodayPlan(): Long
  suspend fun updateProgress(planId: Long, currentIndex: Int)
  suspend fun saveAnswer(planId: Long, answer: PlanCardAnswer)
  suspend fun completePlan(planId: Long, exportText: String)
  suspend fun getPlan(planId: Long): PlanRecord?
}

class PlanRepositoryImpl(
  private val planDao: PlanDao,
) : PlanRepository {
  override fun observeIncompleteForToday(): Flow<PlanRecord?> {
    val today = DateTimeUtil.todayString()
    return planDao.observeIncompleteByDate(today).map { entity ->
      entity?.let { record ->
        val domain = PlanMapper.toDomain(record)
        if (DateTimeUtil.isExpired(domain.createdAt)) null else domain
      }
    }
  }

  override suspend fun createTodayPlan(): Long {
    val entity = PlanRecordEntity(
      date = DateTimeUtil.todayString(),
      created_at = DateTimeUtil.nowMillis(),
    )
    return planDao.insertRecord(entity)
  }

  override suspend fun updateProgress(planId: Long, currentIndex: Int) {
    val current = planDao.getRecordWithAnswers(planId) ?: return
    planDao.updateRecord(current.record.copy(current_index = currentIndex))
  }

  override suspend fun saveAnswer(planId: Long, answer: PlanCardAnswer) {
    planDao.deleteAnswer(planId, answer.cardType.name)
    planDao.upsertAnswers(listOf(PlanMapper.toAnswerEntity(planId, answer)))
  }

  override suspend fun completePlan(planId: Long, exportText: String) {
    val current = planDao.getRecordWithAnswers(planId) ?: return
    planDao.updateRecord(
      current.record.copy(
        completed_at = DateTimeUtil.nowMillis(),
        export_text = exportText,
      ),
    )
  }

  override suspend fun getPlan(planId: Long): PlanRecord? {
    return planDao.getRecordWithAnswers(planId)?.let(PlanMapper::toDomain)
  }
}
