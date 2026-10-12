package de.coldtea.verborum.feature.forum.ui.search

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.RegisterTopBar
import de.coldtea.verborum.core.localization.LocalStrings
import de.coldtea.verborum.core.localization.SupportedLanguage
import de.coldtea.verborum.feature.forum.domain.MarketplaceLanguagePair
import de.coldtea.verborum.feature.forum.domain.MarketplaceListingFilter
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Picking what the Forum list should show.
 *
 * A screen of its own rather than a sheet over the list, because the draft has to survive being
 * edited: three kinds of criterion, two of them multi-select, and a two-step picker inside one of
 * them. **It touches no data layer at all** — the criteria are chosen here and applied there.
 */
@Composable
internal fun ForumSearchScreen(
    initialPairs: List<String>,
    initialTags: Set<String>,
    initialPublisherName: String?,
    onSearch: (MarketplaceListingFilter) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ForumSearchViewModel = koinViewModel {
        parametersOf(initialPairs, initialTags, initialPublisherName)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val strings = LocalStrings.current

    RegisterTopBar(
        title = strings.forumSearchScreenTitle,
        subtitle = strings.forumSearchScreenSubtitle,
        backLabel = strings.forumListScreenTitle,
    )

    ForumSearchContent(
        state = state,
        onAddLanguagePair = viewModel::openLanguagePicker,
        onDismissLanguagePicker = viewModel::dismissLanguagePicker,
        onPickFirstLanguage = viewModel::pickFirstLanguage,
        onPickSecondLanguage = viewModel::pickSecondLanguage,
        onRemoveLanguagePair = viewModel::removeLanguagePair,
        onToggleTag = viewModel::toggleTag,
        onPublisherNameChanged = viewModel::changePublisherName,
        onSearch = { onSearch(state.toFilter()) },
        modifier = modifier,
    )
}

/**
 * The per-platform half.
 *
 * iOS: the two-step language picker is a bottom sheet, and the tags a wrapping chip field. Web: the
 * same criteria as a form, with the picker as a dialog — a sheet is a phone idiom.
 */
@Composable
internal expect fun ForumSearchContent(
    state: ForumSearchUiState,
    onAddLanguagePair: () -> Unit,
    onDismissLanguagePicker: () -> Unit,
    onPickFirstLanguage: (SupportedLanguage) -> Unit,
    onPickSecondLanguage: (SupportedLanguage) -> Unit,
    onRemoveLanguagePair: (MarketplaceLanguagePair) -> Unit,
    onToggleTag: (String) -> Unit,
    onPublisherNameChanged: (String) -> Unit,
    onSearch: () -> Unit,
    modifier: Modifier,
)
