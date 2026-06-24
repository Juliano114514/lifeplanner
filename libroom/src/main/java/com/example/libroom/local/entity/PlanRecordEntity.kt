package com.example.libroom.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "plan_record",
  indices = [Index(value = ["date"])],
)
data class PlanRecordEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val date: String,
  val created_at: Long,
  val completed_at: Long? = null,
  val current_index: Int = 0,
  val fitness_step: String = "TIME",
  val export_text: String? = null,
)
