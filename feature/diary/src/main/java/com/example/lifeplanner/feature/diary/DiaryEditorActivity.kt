package com.example.lifeplanner.feature.diary

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.libui.theme.LifePlannerTheme

class DiaryEditorActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    val epochDay = intent.getLongExtra(EXTRA_EPOCH_DAY, java.time.LocalDate.now().toEpochDay())
    val entryId = intent.getLongExtra(EXTRA_ENTRY_ID, NO_ENTRY_ID).takeIf { it != NO_ENTRY_ID }
    setContent {
      LifePlannerTheme {
        DiaryEditorRoute(
          epochDay = epochDay,
          entryId = entryId,
          onBack = ::finish,
        )
      }
    }
  }

  companion object {
    private const val EXTRA_EPOCH_DAY = "diary_epoch_day"
    private const val EXTRA_ENTRY_ID = "diary_entry_id"
    private const val NO_ENTRY_ID = -1L

    fun createIntent(
      context: Context,
      epochDay: Long,
      entryId: Long? = null,
    ): Intent = Intent(context, DiaryEditorActivity::class.java).apply {
      putExtra(EXTRA_EPOCH_DAY, epochDay)
      entryId?.let { putExtra(EXTRA_ENTRY_ID, it) }
    }
  }
}
