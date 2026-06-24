package com.example.libui.feature.plan

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.example.foundation.domain.plan.PlanInteraction
import com.example.libui.components.ChunkyButton
import com.example.libui.components.HourTimePicker
import com.example.libui.components.PlanCardFrame
import com.example.libui.components.PlanProgressBar
import com.example.libui.feature.plan.cards.OtherNoteCard
import com.example.libui.feature.plan.cards.TagCard
import com.example.libui.theme.Dimens
import com.example.libui.theme.Motion

@Composable
fun PlanContent(
  state: PlanUiState,
  onAction: (PlanAction) -> Unit,
  modifier: Modifier = Modifier,
) {
  if (state.isLoading) {
    Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
      CircularProgressIndicator()
    }
    return
  }

  Column(
    modifier = modifier
      .fillMaxSize()
      .padding(top = Dimens.space16, bottom = Dimens.space24),
    verticalArrangement = Arrangement.spacedBy(Dimens.space16),
  ) {
    PlanProgressBar(
      currentIndex = state.currentIndex,
      totalCount = state.totalCount,
      modifier = Modifier.padding(horizontal = Dimens.screenPadding),
    )
    PlanCardFrame(
      title = state.currentCard.title,
      step = "${state.currentIndex + 1} / ${state.totalCount}",
      onPrevious = { onAction(PlanAction.Previous) },
      onSkip = { onAction(PlanAction.Skip) },
      canGoPrevious = state.canGoPrevious,
      onSwipeDown = { onAction(PlanAction.Previous) },
      modifier = Modifier.weight(1f),
    ) {
      // 仅以 currentIndex 作为换卡 key；每一帧渲染各自的 state 快照，
      // 避免进出场期间读到对方的内容（原闪退/跳变根因）。
      AnimatedContent(
        targetState = state,
        contentKey = { it.currentIndex },
        transitionSpec = {
          val forward = targetState.currentIndex >= initialState.currentIndex
          val dir = if (forward) 1 else -1
          (slideInHorizontally(tween(Motion.MEDIUM)) { dir * it / 4 } + fadeIn(tween(Motion.MEDIUM)))
            .togetherWith(
              slideOutHorizontally(tween(Motion.MEDIUM)) { -dir * it / 4 } + fadeOut(tween(Motion.FAST)),
            )
        },
        label = "card",
      ) { frame ->
        CardBody(state = frame, onAction = onAction)
      }
    }
    ChunkyButton(
      text = if (state.isLastCard) "完成" else "下一张",
      onClick = { onAction(PlanAction.Next) },
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = Dimens.screenPadding),
    )
  }
}

@Composable
private fun CardBody(
  state: PlanUiState,
  onAction: (PlanAction) -> Unit,
) {
  when (state.currentCard.interaction) {
    PlanInteraction.MULTI_TAG, PlanInteraction.SINGLE_TAG -> TagCard(
      definition = state.currentCard,
      answer = state.currentAnswer,
      multiSelect = state.currentCard.interaction == PlanInteraction.MULTI_TAG,
      onToggle = { onAction(PlanAction.ToggleTag(it)) },
      onSelect = { onAction(PlanAction.SelectSingle(it)) },
      onSelectFollowUp = { onAction(PlanAction.SelectFollowUp(it)) },
    )
    PlanInteraction.HOUR_TIME -> {
      val hour = state.currentAnswer?.timeValue
        ?.substringBefore(":")
        ?.toIntOrNull()
      HourTimePicker(
        selectedHour = hour,
        onHourSelected = { onAction(PlanAction.SetHour(it)) },
      )
    }
    PlanInteraction.NOTE -> OtherNoteCard(
      answer = state.currentAnswer,
      onNoteChange = { onAction(PlanAction.SetNote(it)) },
      onAddExtraNote = { onAction(PlanAction.AddExtraNote) },
      onExtraNoteChange = { index, text ->
        onAction(PlanAction.UpdateExtraNote(index, text))
      },
    )
  }
}
