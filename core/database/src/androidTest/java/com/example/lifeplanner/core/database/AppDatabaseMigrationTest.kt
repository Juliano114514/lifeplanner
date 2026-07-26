package com.example.lifeplanner.core.database

import androidx.room.testing.MigrationTestHelper
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppDatabaseMigrationTest {
  @get:Rule
  val helper = MigrationTestHelper(
    InstrumentationRegistry.getInstrumentation(),
    AppDatabase::class.java,
  )

  @Test
  fun migrate3To4PreservesQuickPlanAnswers() {
    helper.createDatabase(TEST_DB, 3).apply {
      execSQL(
        """
        INSERT INTO quick_plan_draft(date, current_index, completed_at)
        VALUES ('2026-07-26', 0, NULL)
        """.trimIndent(),
      )
      execSQL(
        """
        INSERT INTO quick_plan_answer(
          draft_date, card_type, selected_options, sub_selections, hour, note, extra_notes
        ) VALUES ('2026-07-26', 'WORK', '早上', '北区', NULL, '', '')
        """.trimIndent(),
      )
      close()
    }

    val migrated = helper.runMigrationsAndValidate(
      TEST_DB,
      4,
      true,
      AppDatabase.MIGRATION_3_4,
    )

    migrated.query(
      "SELECT COUNT(*) FROM quick_plan_answer WHERE draft_date = '2026-07-26'",
    ).use { cursor ->
      cursor.moveToFirst()
      assertEquals(1, cursor.getInt(0))
    }
    migrated.query("SELECT COUNT(*) FROM quick_plan_period_entry").use { cursor ->
      cursor.moveToFirst()
      assertEquals(0, cursor.getInt(0))
    }
    migrated.close()
  }

  @Test
  fun migrate5To6KeepsExistingPeriodsIncluded() {
    helper.createDatabase(TEST_DB_5_6, 5).apply {
      execSQL(
        """
        INSERT INTO quick_plan_draft(date, current_index, completed_at)
        VALUES ('2026-07-27', 0, NULL)
        """.trimIndent(),
      )
      execSQL(
        """
        INSERT INTO quick_plan_answer(
          draft_date, card_type, selected_options, sub_selections, hour, note, extra_notes
        ) VALUES ('2026-07-27', 'GO_OUT', '', '', NULL, '', '')
        """.trimIndent(),
      )
      execSQL(
        """
        INSERT INTO quick_plan_period_entry(
          draft_date, card_type, period, tag, custom_text, location
        ) VALUES ('2026-07-27', 'GO_OUT', 'MORNING', '出去办事', '', '')
        """.trimIndent(),
      )
      close()
    }

    val migrated = helper.runMigrationsAndValidate(
      TEST_DB_5_6,
      6,
      true,
      AppDatabase.MIGRATION_5_6,
    )

    migrated.query(
      """
      SELECT is_included FROM quick_plan_period_entry
      WHERE draft_date = '2026-07-27' AND card_type = 'GO_OUT' AND period = 'MORNING'
      """.trimIndent(),
    ).use { cursor ->
      cursor.moveToFirst()
      assertEquals(1, cursor.getInt(0))
    }
    migrated.close()
  }

  @Test
  fun migrate6To7KeepsSchedulesPending() {
    helper.createDatabase(TEST_DB_6_7, 6).apply {
      execSQL(
        """
        INSERT INTO schedule_block(
          date, start_minute, end_minute, title, note, task_occurrence_id,
          source, is_user_modified, is_archived, quick_plan_card_type, quick_plan_entry_key
        ) VALUES (
          '2026-07-27', 540, 600, '晨会', '', NULL,
          'MANUAL', 0, 0, NULL, NULL
        )
        """.trimIndent(),
      )
      close()
    }

    val migrated = helper.runMigrationsAndValidate(
      TEST_DB_6_7,
      7,
      true,
      AppDatabase.MIGRATION_6_7,
    )

    migrated.query(
      "SELECT status, completed_at FROM schedule_block WHERE title = '晨会'",
    ).use { cursor ->
      cursor.moveToFirst()
      assertEquals("PENDING", cursor.getString(0))
      assertTrue(cursor.isNull(1))
    }
    migrated.close()
  }

  private companion object {
    const val TEST_DB = "migration-3-4"
    const val TEST_DB_5_6 = "migration-5-6"
    const val TEST_DB_6_7 = "migration-6-7"
  }
}
