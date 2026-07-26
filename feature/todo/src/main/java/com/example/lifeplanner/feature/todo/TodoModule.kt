package com.example.lifeplanner.feature.todo

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val todoFeatureModule = module {
  viewModel { TodoViewModel(get(), get()) }
  viewModel { TaskEditorViewModel(get()) }
}
