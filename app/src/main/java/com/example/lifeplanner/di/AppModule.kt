package com.example.lifeplanner.di

import com.example.libroom.repository.PlanRepository
import com.example.libroom.repository.PlanRepositoryFactory
import com.example.lifeplanner.ui.home.HomeViewModel
import com.example.lifeplanner.ui.plan.PlanResultViewModel
import com.example.lifeplanner.ui.plan.PlanViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val appModule = module {
  single<PlanRepository> { PlanRepositoryFactory.create(get()) }
  viewModel { HomeViewModel(get()) }
  viewModel { PlanViewModel(get()) }
  viewModel { PlanResultViewModel(get()) }
}
