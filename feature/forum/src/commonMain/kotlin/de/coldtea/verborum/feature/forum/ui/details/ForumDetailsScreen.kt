package de.coldtea.verborum.feature.forum.ui.details

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.designsystem.component.ShowSnackbarMessages
import de.coldtea.verborum.core.localization.LocalStrings
import kotlinx.coroutines.flow.map
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/** One shared dictionary: what it is, who wrote it, how it is rated, and its words. */
@Composable
internal fun ForumDetailsScreen(
    dictionaryId: String,
    modifier: Modifier = Modifier,
    viewModel: ForumDetailsViewModel = koinViewModel { parametersOf(dictionaryId) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    RegisterTopBar(
        title = state.name.ifEmpty { strings.forumListScreenTitle },
        backLabel = strings.forumListScreenTitle,
    )

    ShowSnackbarMessages(
        remember(viewModel, strings) {
            viewModel.effects.map { effect ->
                when (effect) {
                    is ForumDetailsEffect.Message -> effect.message.text(strings)
                }
            }
        },
    )

    ForumDetailsContent(
        state = state,
        onRetry = viewModel::load,
        onDownload = viewModel::download,
        onRate = viewModel::rate,
        onRemoveRating = viewModel::removeRating,
        modifier = modifier,
    )
}

/**
 * The per-platform half. iOS keeps the Android layout — an info panel over a read-only word list.
 * Web lays the same content out as a page with the words in a bordered panel.
 */
@Composable
internal expect fun ForumDetailsContent(
    state: ForumDetailsUiState,
    onRetry: () -> Unit,
    onDownload: () -> Unit,
    onRate: (Int) -> Unit,
    onRemoveRating: () -> Unit,
    modifier: Modifier,
)
