package com.example.lifeplanner

import android.app.Application
import com.example.lifeplanner.di.appModule
import com.example.lifeplanner.feature.dishes.dishesFeatureModule
import com.example.lifeplanner.feature.diary.diaryFeatureModule
import com.example.lifeplanner.feature.inventory.inventoryFeatureModule
import com.example.lifeplanner.feature.schedule.scheduleFeatureModule
import com.example.lifeplanner.feature.todo.todoFeatureModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class App : Application() {
  override fun onCreate() {
    super.onCreate()
    startKoin {
      androidContext(this@App)
      modules(
        appModule,
        todoFeatureModule,
        scheduleFeatureModule,
        diaryFeatureModule,
        dishesFeatureModule,
        inventoryFeatureModule,
      )
    }
  }
}
