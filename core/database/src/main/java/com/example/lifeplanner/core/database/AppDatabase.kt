package com.example.lifeplanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.lifeplanner.core.database.dao.QuickPlanDao
import com.example.lifeplanner.core.database.dao.ScheduleDao
import com.example.lifeplanner.core.database.dao.ShoppingDao
import com.example.lifeplanner.core.database.dao.StockDao
import com.example.lifeplanner.core.database.dao.TaskDao
import com.example.lifeplanner.core.database.entity.FoodDetailsEntity
import com.example.lifeplanner.core.database.entity.QuickPlanAnswerEntity
import com.example.lifeplanner.core.database.entity.QuickPlanDraftEntity
import com.example.lifeplanner.core.database.entity.ScheduleBlockEntity
import com.example.lifeplanner.core.database.entity.ShoppingEntryEntity
import com.example.lifeplanner.core.database.entity.StockItemEntity
import com.example.lifeplanner.core.database.entity.StockSnapshotEntity
import com.example.lifeplanner.core.database.entity.TaskEntity
import com.example.lifeplanner.core.database.entity.TaskOccurrenceEntity

@Database(
  entities = [
    TaskEntity::class,
    TaskOccurrenceEntity::class,
    ScheduleBlockEntity::class,
    QuickPlanDraftEntity::class,
    QuickPlanAnswerEntity::class,
    StockItemEntity::class,
    FoodDetailsEntity::class,
    StockSnapshotEntity::class,
    ShoppingEntryEntity::class,
  ],
  version = 3,
  exportSchema = true,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun taskDao(): TaskDao
  abstract fun scheduleDao(): ScheduleDao
  abstract fun quickPlanDao(): QuickPlanDao
  abstract fun stockDao(): StockDao
  abstract fun shoppingDao(): ShoppingDao

  companion object {
    @Volatile
    private var instance: AppDatabase? = null

    fun getInstance(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "lifeplanner.db",
        ).fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
          .build()
          .also { instance = it }
      }
    }
  }
}
