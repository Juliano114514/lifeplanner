package com.example.lifeplanner.feature.schedule

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val scheduleFeatureModule = module {
  viewModel { ScheduleViewModel(get(), get()) }
  viewModel { QuickPlanViewModel(get()) }
}
