package com.example.lifeplanner.ui.plan

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.libui.feature.plan.PlanResultContent
import org.koin.androidx.compose.koinViewModel

@Composable
fun PlanResultScreen(
  planId: Long,
  onDone: () -> Unit,
  modifier: Modifier = Modifier,
  viewModel: PlanResultViewModel = koinViewModel(),
) {
  val state by viewModel.state.collectAsStateWithLifecycle()
  val context = LocalContext.current

  LaunchedEffect(planId) {
    viewModel.load(planId)
  }

  LaunchedEffect(viewModel) {
    viewModel.effect.collect { effect ->
      when (effect) {
        is PlanResultEffect.CopyToClipboard -> {
          val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
          clipboard.setPrimaryClip(ClipData.newPlainText("plan", effect.text))
          Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show()
        }
        PlanResultEffect.NavigateHome -> onDone()
      }
    }
  }

  PlanResultContent(
    state = state,
    onAction = viewModel::onAction,
    modifier = modifier,
  )
}
