package com.example.lifeplanner.feature.diary

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val diaryFeatureModule = module {
  viewModel { DiaryViewModel(get()) }
}
