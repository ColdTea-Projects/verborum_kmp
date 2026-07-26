package de.coldtea.verborum.feature.bibliotheca.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.coldtea.verborum.core.designsystem.component.EmptyState
import de.coldtea.verborum.core.designsystem.component.ErrorState
import de.coldtea.verborum.core.designsystem.component.LoadingState
import de.coldtea.verborum.core.designsystem.theme.Spacing
import de.coldtea.verborum.feature.bibliotheca.data.Word
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun DictionaryScreen(
    onWordClicked: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: DictionaryViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    DictionaryContent(
        state = state,
        onQueryChanged = viewModel::search,
        onWordClicked = onWordClicked,
        onRetry = viewModel::retry,
        modifier = modifier,
    )
}

@Composable
internal fun DictionaryContent(
    state: DictionaryState,
    onQueryChanged: (String) -> Unit,
    onWordClicked: (String) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        OutlinedTextField(
            value = state.query,
            onValueChange = onQueryChanged,
            label = { Text("Search the dictionary") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth().padding(Spacing.medium),
        )

        when {
            state.isLoading -> LoadingState()
            state.error != null -> ErrorState(
                message = "The dictionary could not be loaded.",
                onRetry = onRetry,
            )

            state.words.isEmpty() -> EmptyState("No entry matches “${state.query}”.")
            else -> LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(state.words, key = Word::id) { word ->
                    WordRow(word = word, onClick = { onWordClicked(word.id) })
                    HorizontalDivider()
                }
            }
        }
    }
}

@Composable
private fun WordRow(word: Word, onClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = Spacing.medium, vertical = Spacing.small),
    ) {
        Text(text = word.lemma, style = MaterialTheme.typography.titleMedium)
        Text(
            text = word.translation,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}
