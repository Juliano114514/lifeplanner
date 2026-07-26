package com.example.lifeplanner.di

import com.example.lifeplanner.core.database.AppDatabase
import com.example.lifeplanner.core.database.repository.ScheduleRepositoryImpl
import com.example.lifeplanner.core.database.repository.ShoppingRepositoryImpl
import com.example.lifeplanner.core.database.repository.StockRepositoryImpl
import com.example.lifeplanner.core.database.repository.TaskRepositoryImpl
import com.example.lifeplanner.core.domain.repository.ScheduleRepository
import com.example.lifeplanner.core.domain.repository.ShoppingRepository
import com.example.lifeplanner.core.domain.repository.StockRepository
import com.example.lifeplanner.core.domain.repository.TaskRepository
import org.koin.dsl.module

val appModule = module {
  single { AppDatabase.getInstance(get()) }
  single<TaskRepository> { TaskRepositoryImpl(get()) }
  single<ScheduleRepository> { ScheduleRepositoryImpl(get()) }
  single<StockRepository> { StockRepositoryImpl(get()) }
  single<ShoppingRepository> { ShoppingRepositoryImpl(get()) }
}
