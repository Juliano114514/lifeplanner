package com.example.libroom.local.entity

import androidx.room.Embedded
import androidx.room.Relation

data class PlanRecordWithAnswers(
  @Embedded val record: PlanRecordEntity,
  @Relation(
    parentColumn = "id",
    entityColumn = "plan_record_id",
  )
  val answers: List<PlanCardAnswerEntity>,
)
