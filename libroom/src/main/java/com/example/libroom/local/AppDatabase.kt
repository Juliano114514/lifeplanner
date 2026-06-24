package com.example.libroom.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.libroom.local.dao.PlanDao
import com.example.libroom.local.entity.PlanCardAnswerEntity
import com.example.libroom.local.entity.PlanRecordEntity

@Database(
  entities = [
    PlanRecordEntity::class,
    PlanCardAnswerEntity::class,
  ],
  version = 2,
  exportSchema = false,
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun planDao(): PlanDao

  companion object {
    @Volatile
    private var instance: AppDatabase? = null

    /** v2：卡片答案新增二级选单选择列（健身强度 / 三餐怎么做）。 */
    private val MIGRATION_1_2 = object : Migration(1, 2) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("ALTER TABLE plan_card_answer ADD COLUMN sub_selection TEXT")
      }
    }

    fun getInstance(context: Context): AppDatabase {
      return instance ?: synchronized(this) {
        instance ?: Room.databaseBuilder(
          context.applicationContext,
          AppDatabase::class.java,
          "lifeplanner.db",
        ).addMigrations(MIGRATION_1_2).build().also { instance = it }
      }
    }
  }
}
