package com.example.foundation.util

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object DateTimeUtil {
  private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE
  private val zoneId: ZoneId = ZoneId.systemDefault()

  fun todayString(): String = LocalDate.now(zoneId).format(dateFormatter)

  fun nowMillis(): Long = System.currentTimeMillis()

  fun isWithinHours(timestamp: Long, hours: Long, referenceMillis: Long = nowMillis()): Boolean {
    val elapsed = referenceMillis - timestamp
    return elapsed in 0..hours * 3_600_000L
  }

  fun formatDate(date: String): String {
    val localDate = LocalDate.parse(date, dateFormatter)
    return "${localDate.monthValue}月${localDate.dayOfMonth}日"
  }

  fun isExpired(createdAt: Long, hours: Long = 12): Boolean {
    return !isWithinHours(createdAt, hours)
  }

  fun fromMillis(millis: Long): LocalDate {
    return Instant.ofEpochMilli(millis).atZone(zoneId).toLocalDate()
  }
}
