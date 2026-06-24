package com.example.libroom.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
  tableName = "plan_card_answer",
  foreignKeys = [
    ForeignKey(
      entity = PlanRecordEntity::class,
      parentColumns = ["id"],
      childColumns = ["plan_record_id"],
      onDelete = ForeignKey.CASCADE,
    ),
  ],
  indices = [Index(value = ["plan_record_id"])],
)
data class PlanCardAnswerEntity(
  @PrimaryKey(autoGenerate = true) val id: Long = 0,
  val plan_record_id: Long,
  val card_type: String,
  val card_index: Int,
  val selected_options: String,
  val slider_value: Float? = null,
  val sub_selection: String? = null,
  val time_value: String? = null,
  val note_text: String? = null,
  val extra_notes: String? = null,
)
