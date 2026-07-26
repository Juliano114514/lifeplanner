package com.example.lifeplanner.core.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.lifeplanner.core.database.dao.QuickPlanDao
import com.example.lifeplanner.core.database.dao.ScheduleDao
import com.example.lifeplanner.core.database.dao.ShoppingDao
import com.example.lifeplanner.core.database.dao.StockDao
import com.example.lifeplanner.core.database.dao.TaskDao
import com.example.lifeplanner.core.database.entity.FoodDetailsEntity
import com.example.lifeplanner.core.database.entity.QuickPlanAnswerEntity
import com.example.lifeplanner.core.database.entity.QuickPlanDraftEntity
import com.example.lifeplanner.core.database.entity.QuickPlanPeriodEntryEntity
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
    QuickPlanPeriodEntryEntity::class,
    StockItemEntity::class,
    FoodDetailsEntity::class,
    StockSnapshotEntity::class,
    ShoppingEntryEntity::class,
  ],
  version = 7,
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
        ).addMigrations(MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7)
          .fallbackToDestructiveMigrationFrom(dropAllTables = true, 1, 2)
          .build()
          .also { instance = it }
      }
    }

    val MIGRATION_3_4 = object : Migration(3, 4) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          CREATE TABLE IF NOT EXISTS `quick_plan_period_entry` (
            `draft_date` TEXT NOT NULL,
            `card_type` TEXT NOT NULL,
            `period` TEXT NOT NULL,
            `tag` TEXT NOT NULL,
            `custom_text` TEXT NOT NULL,
            `location` TEXT NOT NULL,
            PRIMARY KEY(`draft_date`, `card_type`, `period`),
            FOREIGN KEY(`draft_date`, `card_type`)
              REFERENCES `quick_plan_answer`(`draft_date`, `card_type`)
              ON UPDATE NO ACTION ON DELETE CASCADE
          )
          """.trimIndent(),
        )
        db.execSQL(
          """
          CREATE INDEX IF NOT EXISTS `index_quick_plan_period_entry_draft_date_card_type`
          ON `quick_plan_period_entry` (`draft_date`, `card_type`)
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_4_5 = object : Migration(4, 5) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          "ALTER TABLE `schedule_block` ADD COLUMN `quick_plan_card_type` TEXT",
        )
        db.execSQL(
          "ALTER TABLE `schedule_block` ADD COLUMN `quick_plan_entry_key` TEXT",
        )
        db.execSQL(
          """
          CREATE INDEX IF NOT EXISTS
            `index_schedule_block_date_quick_plan_card_type_quick_plan_entry_key`
          ON `schedule_block` (`date`, `quick_plan_card_type`, `quick_plan_entry_key`)
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_5_6 = object : Migration(5, 6) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          ALTER TABLE `quick_plan_period_entry`
          ADD COLUMN `is_included` INTEGER NOT NULL DEFAULT 1
          """.trimIndent(),
        )
      }
    }

    val MIGRATION_6_7 = object : Migration(6, 7) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
          """
          ALTER TABLE `schedule_block`
          ADD COLUMN `status` TEXT NOT NULL DEFAULT 'PENDING'
          """.trimIndent(),
        )
        db.execSQL(
          "ALTER TABLE `schedule_block` ADD COLUMN `completed_at` INTEGER",
        )
      }
    }
  }
}
