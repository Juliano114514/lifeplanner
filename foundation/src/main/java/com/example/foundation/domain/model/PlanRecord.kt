package com.example.foundation.domain.model

data class PlanRecord(
  val id: Long = 0,
  val date: String,
  val createdAt: Long,
  val completedAt: Long? = null,
  val currentIndex: Int = 0,
  val exportText: String? = null,
  val answers: List<PlanCardAnswer> = emptyList(),
)
