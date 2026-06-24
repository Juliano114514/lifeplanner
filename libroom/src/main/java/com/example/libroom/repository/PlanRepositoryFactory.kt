package com.example.libroom.repository

import android.content.Context

object PlanRepositoryFactory {
  fun create(context: Context): PlanRepository {
    val database = com.example.libroom.local.AppDatabase.getInstance(context)
    return PlanRepositoryImpl(database.planDao())
  }
}
