package de.coldtea.verborum.feature.bibliotheca.selfpractice.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.feature.bibliotheca.selfpractice.ui.model.PracticeWordUi
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import de.coldtea.verborum.core.localization.strings

/**
 * Practising a dictionary. Everything about *what* a session is — order, direction, which cards are
 * open, how an answer moves the level — lives in [SelfPracticeViewModel] and is shared.
 *
 * Only the presentation is per-platform, and deliberately so: iOS gets the Android app's expandable
 * cards in a single column, web gets a grid of flip cards. This is the one screen in the app with a
 * forked design; every other screen adapts by layout instead. Both actuals render the same state and
 * call the same callbacks, so the fork stops at [SelfPracticeContent].
 */
@Composable
internal fun SelfPracticeScreen(
    dictionaryId: String,
    modifier: Modifier = Modifier,
    viewModel: SelfPracticeViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    ShowSnackbarMessages(viewModel.messages)

    when (val practice = state.practice) {
        SelfPracticeState.Loading -> {
            RegisterTopBar(title = strings.selfPractice)
            LoadingState(modifier)
        }

        SelfPracticeState.Failed -> {
            RegisterTopBar(title = strings.selfPractice)
            ErrorState(
                message = strings.practiceFailed,
                modifier = modifier,
                onRetry = viewModel::retry,
            )
        }

        is SelfPracticeState.Success -> {
            RegisterTopBar(
                title = practice.dictionaryName,
                subtitle = if (state.isReversed) strings.translationFirst else strings.wordFirst,
                showBackButton = true,
            )

            SelfPracticeContent(
                words = practice.words,
                openWordIds = state.openWordIds,
                onToggleOpen = viewModel::toggleOpen,
                onCorrect = viewModel::onCorrect,
                onWrong = viewModel::onWrong,
                onSwitchSides = viewModel::switchSides,
                modifier = modifier,
            )
        }
    }
}

/**
 * The per-platform half of the screen.
 *
 * iOS: expandable cards in a column, swiped left/right to change a level — the Android design.
 * Web: a grid of flip cards with wrong/correct buttons on the back, spread across the full width.
 */
@Composable
internal expect fun SelfPracticeContent(
    words: List<PracticeWordUi>,
    openWordIds: Set<String>,
    onToggleOpen: (String) -> Unit,
    onCorrect: (String) -> Unit,
    onWrong: (String) -> Unit,
    onSwitchSides: () -> Unit,
    modifier: Modifier,
)
