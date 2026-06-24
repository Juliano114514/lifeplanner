package com.example.foundation.domain.model

data class PlanCardAnswer(
  val cardType: PlanCardType,
  val cardIndex: Int,
  val selectedOptions: List<String> = emptyList(),
  val subSelection: String? = null,
  val timeValue: String? = null,
  val noteText: String? = null,
  val extraNotes: List<String> = emptyList(),
)
